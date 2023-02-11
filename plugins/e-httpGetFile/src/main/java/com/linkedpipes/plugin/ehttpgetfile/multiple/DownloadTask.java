package com.linkedpipes.plugin.ehttpgetfile.multiple;

import com.linkedpipes.etl.executor.api.v1.component.task.Task;
import com.linkedpipes.etl.executor.api.v1.rdf.RdfToPojo;

import java.util.LinkedList;
import java.util.List;

@RdfToPojo.Type(iri = HttpGetFilesVocabulary.REFERENCE)
public class DownloadTask implements Task {

    @RdfToPojo.Resource
    private String iri;

    @RdfToPojo.Property(iri = HttpGetFilesVocabulary.HAS_URI)
    private String uri;

    @RdfToPojo.Property(iri = HttpGetFilesVocabulary.HAS_FILE_NAME)
    private String fileName;

    @RdfToPojo.Property(iri = HttpGetFilesVocabulary.HAS_HEADER)
    private List<RequestHeader> headers = new LinkedList<>();

    @RdfToPojo.Property(iri = HttpGetFilesVocabulary.HAS_TIMEOUT)
    private Integer timeOut = null;

    @RdfToPojo.Property(iri = HttpGetFilesVocabulary.HAS_GROUP)
    private String group = null;

    public DownloadTask() {
    }

    @Override
    public String getIri() {
        return iri;
    }

    public void setIri(String iri) {
        this.iri = iri;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public List<RequestHeader> getHeaders() {
        return headers;
    }

    public void setHeaders(List<RequestHeader> headers) {
        this.headers = headers;
    }

    public Integer getTimeOut() {
        return timeOut;
    }

    public void setTimeOut(Integer timeOut) {
        this.timeOut = timeOut;
    }

    @Override
    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

}
