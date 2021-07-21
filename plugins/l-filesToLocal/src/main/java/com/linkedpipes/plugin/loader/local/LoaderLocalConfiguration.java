package com.linkedpipes.plugin.loader.local;

import com.linkedpipes.etl.executor.api.v1.rdf.RdfToPojo;

@RdfToPojo.Type(iri = LoaderLocalVocabulary.CONFIG)
public class LoaderLocalConfiguration {

    @RdfToPojo.Property(iri = LoaderLocalVocabulary.HAS_PATH)
    private String path;

    @RdfToPojo.Property(iri = LoaderLocalVocabulary.HAS_PERMISSIONS)
    private String permissions = null;

    public LoaderLocalConfiguration() {
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getPermissions() {
        return permissions;
    }

    public void setPermissions(String permissions) {
        this.permissions = permissions;
    }

}
