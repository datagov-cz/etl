package com.linkedpipes.etl.storage.pipeline.transformation;

//import com.linkedpipes.etl.executor.api.v1.vocabulary.LP_PIPELINE;
//import com.linkedpipes.etl.storage.StorageException;
//import com.linkedpipes.etl.storage.migration.MigrateV1ToV2;
//import com.linkedpipes.etl.storage.pipeline.PipelineRef;
//import com.linkedpipes.etl.storage.template.Template;
//import com.linkedpipes.etl.storage.template.TemplateFacade;
//import com.linkedpipes.etl.storage.template.mapping.Mapping;
//import com.linkedpipes.etl.storage.template.mapping.MappingFacade;
//import org.eclipse.rdf4j.model.IRI;
//import org.eclipse.rdf4j.model.Resource;
//import org.eclipse.rdf4j.model.Statement;
//import org.eclipse.rdf4j.model.ValueFactory;
//import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
//import org.eclipse.rdf4j.model.vocabulary.RDF;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.Collections;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.LinkedList;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//
//class ImportTemplates {
//
//    private static final Logger LOG =
//            LoggerFactory.getLogger(ImportTemplates.class);
//
//    private final TemplateFacade templateFacade;
//
//    private final MappingFacade mappingFacade;
//
//    private boolean importMissing = false;
//
//    private boolean updateExisting = false;
//
//    private int pipelineVersion;
//
//    private Resource pipelineGraph;
//
//    private Map<IRI, List<Statement>> graphs;
//
//    private Mapping mapping;
//
//    private final ValueFactory valueFactory = SimpleValueFactory.getInstance();
//
//    public ImportTemplates(
//            TemplateFacade templateFacade, MappingFacade mappingFacade) {
//        this.templateFacade = templateFacade;
//        this.mappingFacade = mappingFacade;
//    }
//
//    public void setImportMissing(boolean importMissing) {
//        this.importMissing = importMissing;
//    }
//
//    public void setUpdateExisting(boolean updateExisting) {
//        this.updateExisting = updateExisting;
//    }
//
//    public void setPipelineVersion(int pipelineVersion) {
//        this.pipelineVersion = pipelineVersion;
//    }
//
//    /**
//     * Align templates in the pipeline with local templates. Missing
//     * templates are imported.
//     */
//    public Collection<Statement> importTemplatesFromPipeline(
//            Collection<Statement> pipelineRdf) throws StorageException {
//        LOG.debug("Import options:");
//        LOG.debug("  Import templates: {}", this.importMissing);
//        LOG.debug("  Update existing templates: {}", this.updateExisting);
//        LOG.debug("  Pipeline version: {}", this.pipelineVersion);
//        initialize();
//        loadStatements(pipelineRdf);
//        if (pipelineGraph == null) {
//            // There is no pipeline.
//            LOG.warn("No pipeline graph found!");
//            return pipelineRdf;
//        }
//        loadMapping(pipelineRdf);
//        importTemplates();
//        saveMappingsToHdd();
//        return collectPipeline();
//    }
//
//    private void initialize() {
//        this.pipelineGraph = null;
//        this.graphs = new HashMap<>();
//        this.mapping = null;
//    }
//
//    private void loadStatements(Collection<Statement> pipelineRdf) {
//        for (Statement statement : pipelineRdf) {
//            List<Statement> graph = graphs.get(statement.getContext());
//            if (graph == null) {
//                graph = new LinkedList<>();
//                graphs.put((IRI) statement.getContext(), graph);
//            }
//            if (statement.getPredicate().equals(RDF.TYPE)
//                    && statement.getObject().equals(PipelineRef.TYPE)) {
//                pipelineGraph = statement.getContext();
//            }
//            graph.add(statement);
//        }
//    }
//
//    private void loadMapping(Collection<Statement> pipelineRdf) {
//        this.mapping = mappingFacade.createMappingFromStatements(pipelineRdf);
//    }
//
//    private void importTemplates() throws StorageException {
//        List<TemplateInfo> templates = TemplateInfo.create(graphs);
//        List<TemplateInfo> resolvedTemplates = new ArrayList<>();
//        // We try to import templates. As there might be hierarchy we should
//        // import at least one template in each cycle.
//        while (!templates.isEmpty()) {
//            for (TemplateInfo templateInfo : templates) {
//                if (resolveTemplate(templateInfo)) {
//                    resolvedTemplates.add(templateInfo);
//                }
//            }
//            if (resolvedTemplates.isEmpty()) {
//                LOG.error("Failed to import following templates:");
//                for (TemplateInfo templateInfo : templates) {
//                    LOG.info("   {}", templateInfo.getIri());
//                }
//                return;
//            }
//            templates.removeAll(resolvedTemplates);
//            resolvedTemplates.clear();
//        }
//    }
//
//    private boolean resolveTemplate(TemplateInfo template)
//            throws StorageException {
//        Template localTemplate;
//        // First try to just ask for the URL.
//        localTemplate = templateFacade.getTemplate(template.getIri());
//        if (localTemplate != null) {
//            return true;
//        }
//        // Try mapping.
//        String templateIri = mapping.remoteToLocal(template.getIri());
//        Template mappedLocalTemplate =
//                templateFacade.getTemplate(templateIri);
//        if (mappedLocalTemplate == null) {
//            if (importMissing) {
//                return importTemplate(template);
//            } else {
//                LOG.info("Skip: {}", templateIri);
//                return false;
//            }
//        } else {
//            LOG.debug("Mapping {} to {}", template.getIri(), templateIri);
//            if (updateExisting) {
//                LOG.info("Updating local template: {}", templateIri);
//                updateLocal(template, mappedLocalTemplate);
//            }
//            return true;
//        }
//    }
//
//    private void updateLocal(TemplateInfo remote, Template local)
//            throws StorageException {
//        templateFacade.updateInterface(local, remote.getDefinition());
//        Template parent = templateFacade.getParent(local);
//        prepareTemplateForImport(remote, parent);
//        Collection<Statement> config = remote.getConfiguration();
//        templateFacade.updateConfig(local, config);
//    }
//
//    private void prepareTemplateForImport(
//            TemplateInfo remote, Template localParent) {
//        if (pipelineVersion < 2) {
//            Template root = templateFacade.getRootTemplate(localParent);
//            if (MigrateV1ToV2.shouldUpdate(root.getIri())) {
//                remote.setConfiguration(MigrateV1ToV2.updateConfiguration(
//                        remote.getConfiguration(),
//                        root.getIri()));
//            }
//        }
//    }
//
//    private boolean importTemplate(TemplateInfo remoteTemplate) {
//        Template parent = getLocalParent(remoteTemplate);
//        if (parent == null) {
//            return false;
//        }
//        LOG.info("Importing: {} with remote parent: {}",
//                remoteTemplate.getIri(), remoteTemplate.getTemplate());
//        LOG.info("   local parent: {}", parent.getIri());
//        remoteTemplate.setTemplate(valueFactory.createIRI(parent.getIri()));
//        prepareTemplateForImport(remoteTemplate, parent);
//        try {
//            Template template = templateFacade.createTemplate(
//                    remoteTemplate.getDefinition(),
//                    remoteTemplate.getConfiguration(),
//                    getConfigDescription(remoteTemplate, parent));
//            LOG.info("   imported as : {}", template.getIri());
//            this.mapping.onImport(template, remoteTemplate.getIri());
//        } catch (StorageException ex) {
//            LOG.error("Can't import template: {}", remoteTemplate.getIri(), ex);
//            LOG.info("Template is ignored.");
//            return true;
//        }
//        return true;
//    }
//
//    private Template getLocalParent(TemplateInfo remoteTemplate) {
//        String parentIri = remoteTemplate.getTemplate().stringValue();
//        Template localTemplate = templateFacade.getTemplate(parentIri);
//        if (localTemplate == null) {
//            String localParentIri = mapping.remoteToLocal(parentIri);
//            return templateFacade.getTemplate(localParentIri);
//        } else {
//            return localTemplate;
//        }
//    }
//
//    private Collection<Statement> getConfigDescription(
//            TemplateInfo templateInfo, Template parent) {
//        // We used to use description from the pipeline, but as there
//        // is no versioning that cause issues with old versions
//        // - upon configuration change. For this reason we always
//        // use descriptions from root templates.
//        return null;
//    }
//
//    private void saveMappingsToHdd() {
//        mappingFacade.save();
//    }
//
//    private List<Statement> collectPipeline() {
//        List<Statement> definition = graphs.get(pipelineGraph);
//        Set<Resource> configurations = new HashSet<>();
//        List<Statement> toRemove = new ArrayList<>();
//        List<Statement> toAdd = new ArrayList<>();
//        for (Statement statement : definition) {
//            // Check for configuration.
//            String predicate = statement.getPredicate().stringValue();
//            if (predicate.equals(LP_PIPELINE.HAS_CONFIGURATION_GRAPH)) {
//                configurations.add((Resource) statement.getObject());
//                continue;
//            }
//            // Check template references and update them.
//            if (predicate.equals(LP_PIPELINE.HAS_TEMPLATE)) {
//                // Check for import. Now all templates should be imported
//                // so we can just ask for mapping.
//                String templateIri = statement.getObject().stringValue();
//                String localTemplateIri = getIriForTemplate(templateIri);
//                toRemove.add(statement);
//                toAdd.add(valueFactory.createStatement(
//                        statement.getSubject(),
//                        statement.getPredicate(),
//                        valueFactory.createIRI(localTemplateIri),
//                        statement.getContext()));
//            }
//        }
//        // Collect.
//        List<Statement> result = new ArrayList<>(definition.size());
//        result.addAll(definition);
//        result.removeAll(toRemove);
//        result.addAll(toAdd);
//        for (Resource configurationIri : configurations) {
//            result.addAll(graphs.getOrDefault(
//                    configurationIri, Collections.emptyList()));
//        }
//        return result;
//    }
//
//    private String getIriForTemplate(String iri) {
//        String result = mapping.remoteToLocal(iri);
//        if (result == null) {
//            // Not mapped, could be a core template, or we just failed
//            // to import something.
//            return iri;
//        } else {
//            return result;
//        }
//    }
//
//    /**
//     * Remove templates and all related information from a pipeline.
//     * TODO Move to another class.
//     */
//    public Collection<Statement> removeTemplates(
//            Collection<Statement> pipelineRdf) {
//        initialize();
//        loadStatements(pipelineRdf);
//        // Collect pipeline without templates.
//        List<Statement> definition = graphs.get(pipelineGraph);
//        Set<Resource> configurations = new HashSet<>();
//        for (Statement statement : definition) {
//            // Check for configuration.
//            String predicate = statement.getPredicate().stringValue();
//            if (predicate.equals(LP_PIPELINE.HAS_CONFIGURATION_GRAPH)) {
//                configurations.add((Resource) statement.getObject());
//                continue;
//            }
//        }
//        // Collect.
//        List<Statement> result = new ArrayList<>(definition.size());
//        result.addAll(definition);
//        List<Statement> toRemove = new ArrayList<>();
//        result.removeAll(toRemove);
//        List<Statement> toAdd = new ArrayList<>();
//        result.addAll(toAdd);
//        for (Resource configurationIri : configurations) {
//            result.addAll(graphs.getOrDefault(
//                    configurationIri, Collections.emptyList()));
//        }
//        return result;
//    }
//
//}
