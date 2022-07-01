package com.linkedpipes.etl.executor.api.v1;

import com.linkedpipes.etl.executor.api.v1.rdf.model.RdfSource;

/**
 * Observer for pipeline execution events. Can be used by
 * plugins to observe the state of the pipeline.
 */
public interface PipelineExecutionObserver {

    void onPipelineBegin(String pipeline, RdfSource definition)
            throws LpException;

    void onPipelineEnd();

}
