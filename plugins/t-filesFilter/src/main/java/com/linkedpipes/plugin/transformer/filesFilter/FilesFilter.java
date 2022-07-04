package com.linkedpipes.plugin.transformer.filesFilter;

import com.linkedpipes.etl.dataunit.core.files.FilesDataUnit;
import com.linkedpipes.etl.dataunit.core.files.WritableFilesDataUnit;
import com.linkedpipes.etl.dataunit.core.rdf.SingleGraphDataUnit;
import com.linkedpipes.etl.executor.api.v1.LpException;
import com.linkedpipes.etl.executor.api.v1.component.Component;
import com.linkedpipes.etl.executor.api.v1.component.SequentialExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class FilesFilter implements Component, SequentialExecution {

    private static final Logger LOG =
            LoggerFactory.getLogger(FilesFilter.class);

    @Component.ContainsConfiguration
    @Component.InputPort(iri = "Configuration")
    public SingleGraphDataUnit configurationRdf;

    @Component.InputPort(iri = "InputFiles")
    public FilesDataUnit inputFiles;

    @Component.InputPort(iri = "OutputFiles")
    public WritableFilesDataUnit outputFiles;

    @Component.Configuration
    public FilesFilterConfiguration configuration;

    @Override
    public void execute() throws LpException {
        if (configuration.getFileNamePattern() == null
                || configuration.getFileNamePattern().isEmpty()) {
            throw new LpException(
                    FilesFilterVocabulary.HAS_PATTERN);
        }
        //
        final String pattern = configuration.getFileNamePattern();
        LOG.debug("Pattern: {}", pattern);
        for (FilesDataUnit.Entry entry : inputFiles) {
            final boolean matches = entry.getFileName().matches(pattern);
            LOG.debug("Entry: {} : {}", entry.getFileName(), matches);
            if (matches) {
                final File outputFile = outputFiles.createFile(
                        entry.getFileName());
                try {
                    Files.copy(entry.toFile().toPath(), outputFile.toPath());
                } catch (IOException ex) {
                    throw new LpException("Can't copy file: {}",
                            entry.getFileName(), ex);
                }
            }
        }
    }

}
