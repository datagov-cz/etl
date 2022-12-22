package com.linkedpipes.plugin.transformer.rdftofile;

import com.linkedpipes.etl.dataunit.core.files.WritableFilesDataUnit;
import com.linkedpipes.etl.dataunit.core.rdf.SingleGraphDataUnit;
import com.linkedpipes.etl.executor.api.v1.LpException;
import com.linkedpipes.etl.executor.api.v1.component.Component;
import com.linkedpipes.etl.executor.api.v1.component.SequentialExecution;
import com.linkedpipes.etl.executor.api.v1.service.ProgressReport;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Optional;

public final class RdfToFile implements Component, SequentialExecution {

    private static final String FILE_ENCODE = "UTF-8";

    @Component.ContainsConfiguration
    @Component.InputPort(iri = "Configuration")
    public SingleGraphDataUnit configurationRdf;

    @Component.InputPort(iri = "InputRdf")
    public SingleGraphDataUnit inputRdf;

    @Component.OutputPort(iri = "OutputFile")
    public WritableFilesDataUnit outputFiles;

    @Component.Configuration
    public RdfToFileConfiguration configuration;

    @Component.Inject
    public ProgressReport progressReport;

    private RDFFormat outputFormat;

    private File outputFile;

    @Override
    public void execute() throws LpException {
        prepareOutputFormat();
        prepareOutputFile();
        inputRdf.execute((connection) -> {
            export(connection);
        });
    }

    private void prepareOutputFormat() throws LpException {
        Optional<RDFFormat> rdfFormat = Rio.getParserFormatForMIMEType(
                configuration.getFileType());
        if (!rdfFormat.isPresent()) {
            throw new LpException("Invalid output file type: {}",
                    configuration.getFileName());
        }
        outputFormat = rdfFormat.get();
    }

    private void prepareOutputFile() throws LpException {
        outputFile = outputFiles.createFile(configuration.getFileName());
    }

    private void export(RepositoryConnection connection) throws LpException {
        reportStart(connection);
        try (FileOutputStream outStream = new FileOutputStream(outputFile);
             OutputStreamWriter outWriter = new OutputStreamWriter(
                     outStream, Charset.forName(FILE_ENCODE))) {
            RDFHandler writer = createWriter(outWriter);
            connection.export(writer, inputRdf.getReadGraph());
        } catch (IOException ex) {
            throw new LpException("Can't write data.", ex);
        }
        reportEnd();
    }

    private RDFHandler createWriter(OutputStreamWriter streamWriter) {
        RDFWriter writer;
        if (outputFormat == RDFFormat.JSONLD) {
            writer = new JsonLdWriter(streamWriter);
        } else {
            writer = Rio.createWriter(outputFormat, streamWriter);
        }

        if (outputFormat.supportsContexts()) {
            writer = new ChangeContext(writer, getOutputGraph());
        }

        writer = new PerStatementProgressReport(writer, progressReport);
        return writer;
    }

    private void reportStart(RepositoryConnection connection) {
        progressReport.start((int) connection.size(inputRdf.getReadGraph()));
    }

    private void reportEnd() {
        progressReport.done();
    }

    private IRI getOutputGraph() {
        String graph = configuration.getGraphUri();
        if (graph == null || graph.isEmpty() || graph.isBlank()) {
            return null;
        }
        return SimpleValueFactory.getInstance().createIRI(
                configuration.getGraphUri());
    }

}
