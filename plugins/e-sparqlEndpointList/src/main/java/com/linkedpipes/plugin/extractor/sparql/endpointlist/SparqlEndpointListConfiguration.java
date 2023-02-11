package com.linkedpipes.plugin.extractor.sparql.endpointlist;

import com.linkedpipes.etl.executor.api.v1.component.task.TaskExecutionConfiguration;
import com.linkedpipes.etl.executor.api.v1.rdf.RdfToPojo;

@RdfToPojo.Type(iri = SparqlEndpointListVocabulary.CONFIG)
public class SparqlEndpointListConfiguration {

    @RdfToPojo.Property(iri = SparqlEndpointListVocabulary.HAS_USED_THREADS)
    private int threadsNumber = 1;

    @RdfToPojo.Property(iri = SparqlEndpointListVocabulary.HAS_TIME_LIMIT)
    private int executionTimeLimit = -1;

    @RdfToPojo.Property(iri = SparqlEndpointListVocabulary.HAS_ENCODE_RDF)
    private boolean fixIncomingRdf = false;

    @RdfToPojo.Property(iri = SparqlEndpointListVocabulary.HAS_TASK_PER_GROUP)
    private int taskPerGroupLimit = 0;

    @RdfToPojo.Property(iri = SparqlEndpointListVocabulary.HAS_COMMIT_SIZE)
    private int commitSize = 0;

    @RdfToPojo.Property(
            iri = SparqlEndpointListVocabulary.HAS_USE_TOLERANT_REPOSITORY)
    private boolean useTolerantRepository = false;

    public SparqlEndpointListConfiguration() {
    }

    public int getThreadsNumber() {
        return threadsNumber;
    }

    public void setThreadsNumber(int threadsNumber) {
        this.threadsNumber = threadsNumber;
    }

    public int getExecutionTimeLimit() {
        return executionTimeLimit;
    }

    public void setExecutionTimeLimit(int executionTimeLimit) {
        this.executionTimeLimit = executionTimeLimit;
    }

    public boolean isFixIncomingRdf() {
        return fixIncomingRdf;
    }

    public void setFixIncomingRdf(boolean fixIncomingRdf) {
        this.fixIncomingRdf = fixIncomingRdf;
    }

    public int getTaskPerGroupLimit() {
        return taskPerGroupLimit;
    }

    public void setTaskPerGroupLimit(int taskPerGroupLimit) {
        this.taskPerGroupLimit = taskPerGroupLimit;
    }

    public int getCommitSize() {
        return commitSize;
    }

    public void setCommitSize(int commitSize) {
        this.commitSize = commitSize;
    }

    public boolean isUseTolerantRepository() {
        return useTolerantRepository;
    }

    public void setUseTolerantRepository(boolean useTolerantRepository) {
        this.useTolerantRepository = useTolerantRepository;
    }

}
