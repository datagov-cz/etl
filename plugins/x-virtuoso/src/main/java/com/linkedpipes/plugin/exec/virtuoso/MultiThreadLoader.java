package com.linkedpipes.plugin.exec.virtuoso;

import com.linkedpipes.etl.executor.api.v1.LpException;
import com.linkedpipes.etl.executor.api.v1.service.ProgressReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

class MultiThreadLoader {

    private static final Logger LOG =
            LoggerFactory.getLogger(MultiThreadLoader.class);

    private final SqlExecutor sqlExecutor;

    private final VirtuosoConfiguration configuration;

    private final ProgressReport progressReport;

    private int lastReportedProgress = 0;

    private ExecutorService executor;

    private List<LoadWorker> workers;

    private int loadedBeforeStart;

    public MultiThreadLoader(
            SqlExecutor sqlExecutor,
            VirtuosoConfiguration configuration,
            ProgressReport progressReport) {
        this.sqlExecutor = sqlExecutor;
        this.configuration = configuration;
        this.progressReport = progressReport;
    }

    public void loadData(int filesToLoad) throws LpException {
        loadedBeforeStart = sqlExecutor.getFilesLoaded(
                configuration.getLoadDirectoryPath());

        startLoading(filesToLoad);
        while (true) {
            waitBeforeNextCheck();
            LOG.debug("Checking status ... ");
            boolean finished = checkLoadingFinished(sqlExecutor, filesToLoad);
            LOG.debug("Checking status ... done");
            if (finished) {
                break;
            }
        }
        LOG.debug("Awaiting termination ...");
        waitForTermination();
        LOG.debug("Awaiting termination ... done");
        checkResults();
    }

    private void waitBeforeNextCheck() {
        try {
            Thread.sleep(configuration.getStatusUpdateInterval() * 1000);
        } catch (InterruptedException ex) {
            // Do nothing here.
        }
    }

    private boolean checkLoadingFinished(SqlExecutor executor, int taskCount)
            throws LpException {
        final int filesLoaded = executor.getFilesLoaded(
                configuration.getLoadDirectoryPath());
        int loadedFromStart = filesLoaded - loadedBeforeStart;
        LOG.debug("Processing {}/{} files", loadedFromStart, taskCount);
        while (loadedFromStart > lastReportedProgress) {
            progressReport.entryProcessed();
            ++lastReportedProgress;
        }

        if (loadedFromStart >= taskCount) {
            return true;
        }
        return false;
    }

    private void startLoading(int filesToLoad) throws LpException {
        int loaders = configuration.getLoaderCount();
        if (configuration.getLoaderCount() > filesToLoad) {
            loaders = filesToLoad;
            LOG.info("Decreasing number of threads to files to load: {}",
                    filesToLoad);
        }
        initializeWorkers(loaders);
    }

    private void initializeWorkers(int loaders) {
        executor = Executors.newFixedThreadPool(loaders);
        workers = new ArrayList<>(loaders);
        LOG.info("Using {} loaders", loaders);
        for (int i = 0; i < loaders; ++i) {
            LoadWorker worker = new LoadWorker(sqlExecutor);
            executor.submit(worker);
            workers.add(worker);
        }
    }

    private void waitForTermination() {
        executor.shutdown();
        while (true) {
            try {
                if (executor.awaitTermination(100, TimeUnit.MILLISECONDS)) {
                    break;
                }
            } catch (InterruptedException ex) {
                // Ignore.
            }
        }
    }

    public void checkResults() throws LpException {
        for (LoadWorker worker : workers) {
            if (worker.getException() != null) {
                throw new LpException("Can't load data");
            }
        }
    }

}
