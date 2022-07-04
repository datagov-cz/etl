package com.linkedpipes.plugin.transformer.sparql.construct;

import com.linkedpipes.etl.dataunit.core.rdf.ChunkedTriples;
import com.linkedpipes.etl.dataunit.core.rdf.SingleGraphDataUnit;
import com.linkedpipes.etl.dataunit.core.rdf.WritableChunkedTriples;
import com.linkedpipes.etl.executor.api.v1.LpException;
import com.linkedpipes.etl.executor.api.v1.component.Component;
import com.linkedpipes.etl.executor.api.v1.component.SequentialExecution;
import com.linkedpipes.etl.executor.api.v1.service.ProgressReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Chunked version of SPARQL construct. Perform the construct operation
 * on chunks of RDF data.
 *
 * TODO: Use the same vocabulary as SPARQL construct.
 */
public final class SparqlConstructChunked implements Component,
        SequentialExecution {

    private static final Logger LOG =
            LoggerFactory.getLogger(SparqlConstructChunked.class);

    @Component.InputPort(iri = "InputRdf")
    public ChunkedTriples inputRdf;

    @Component.ContainsConfiguration
    @Component.InputPort(iri = "Configuration")
    public SingleGraphDataUnit configurationRdf;

    @Component.InputPort(iri = "OutputRdf")
    public WritableChunkedTriples outputRdf;

    @Component.Configuration
    public SparqlConstructConfiguration configuration;

    @Component.Inject
    public ProgressReport progressReport;

    private ExecutorManager executorManager;

    private List<SparqlConstructExecutor> executors = new LinkedList<>();

    @Override
    public void execute() throws LpException {
        checkConfiguration();
        createExecutors();
        ExecutorService executor = Executors.newFixedThreadPool(
                configuration.getNumberOfThreads());

        progressReport.start(inputRdf.size());
        startThreads(executor);
        executor.shutdown();
        awaitTermination(executor);
        progressReport.done();
        checkExecutorsStatus();
    }

    private void createExecutors() {
        executorManager = new ExecutorManager(
                inputRdf, outputRdf, progressReport);
        for (int i = 0; i < configuration.getNumberOfThreads(); ++i) {
            SparqlConstructExecutor constructExecutor =
                    new SparqlConstructExecutor(executorManager,
                            configuration.getQuery(),
                            configuration.isUseDeduplication(),
                            configuration.isSkipOnFailure());
            executors.add(constructExecutor);
        }
    }

    private void checkConfiguration() throws LpException {
        String query = configuration.getQuery();
        if (query == null || query.isEmpty()) {
            throw new LpException("Missing property: {}",
                    SparqlConstructVocabulary.HAS_QUERY);
        }
    }

    private void startThreads(ExecutorService executor) {
        LOG.info("Number of threads: {}", configuration.getNumberOfThreads());
        for (SparqlConstructExecutor constructExecutor : executors) {
            executor.submit(constructExecutor);
        }
    }

    private void awaitTermination(ExecutorService executor) {
        LOG.info("Waiting for executors ...");
        while (true) {
            try {
                if (executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    break;
                }
            } catch (InterruptedException ex) {
                // Ignore.
            }
        }
        LOG.info("Waiting for executors ... done");
    }

    private void checkExecutorsStatus() throws LpException {
        for (SparqlConstructExecutor constructExecutor : executors) {
            if (constructExecutor.isFailed()) {
                throw new LpException(
                        "At least one construct failed. See logs for more info.");
            }
        }
    }

}
