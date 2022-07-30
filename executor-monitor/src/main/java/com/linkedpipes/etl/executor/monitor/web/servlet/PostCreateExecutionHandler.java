package com.linkedpipes.etl.executor.monitor.web.servlet;

import com.linkedpipes.etl.executor.monitor.MonitorException;
import com.linkedpipes.etl.executor.monitor.execution.Execution;
import com.linkedpipes.etl.executor.monitor.execution.ExecutionFacade;
import com.linkedpipes.etl.executor.monitor.executor.ExecutorService;
import com.linkedpipes.etl.library.rdf.Statements;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

class PostCreateExecutionHandler {

    public static class Response {

        private String iri;

        Response(Execution execution) {
            this.iri = execution.getIri();
        }

        public String getIri() {
            return iri;
        }

        public void setIri(String iri) {
            this.iri = iri;
        }

    }

    private final ExecutionFacade executionFacade;

    private final ExecutorService executorService;

    public PostCreateExecutionHandler(
            ExecutionFacade executionFacade, ExecutorService executorService) {
        this.executionFacade = executionFacade;
        this.executorService = executorService;
    }

    public Response handle(MultipartFile pipeline, List<MultipartFile> inputs)
            throws MonitorException {
        Statements pipelineRdf = readPipeline(pipeline);
        Execution execution = executionFacade.createExecution(
                pipelineRdf, inputs);
        executorService.asyncStartExecutions();
        return new Response(execution);
    }

    private Statements readPipeline(MultipartFile pipeline)
            throws MonitorException {
        if (pipeline.getOriginalFilename() == null) {
            throw new MonitorException("Missing name of the pipeline.");
        }
        Optional<RDFFormat> format = Rio.getWriterFormatForFileName(
                pipeline.getOriginalFilename());
        if (!format.isPresent()) {
            throw new MonitorException("Can't determined format type.");
        }
        Statements statements = Statements.arrayList();
        try (InputStream stream = pipeline.getInputStream()) {
            statements.file().addAll(stream, format.get());
        } catch (IOException ex) {
            throw new MonitorException("Can't read pipeline.", ex);
        }
        return statements;
    }

}
