package com.linkedpipes.plugin.transformer.sparql.constructtofilelist;

import com.linkedpipes.etl.dataunit.core.files.WritableFilesDataUnit;
import com.linkedpipes.etl.dataunit.core.rdf.SingleGraphDataUnit;
import com.linkedpipes.etl.executor.api.v1.LpException;
import com.linkedpipes.etl.executor.api.v1.component.Component;
import com.linkedpipes.etl.executor.api.v1.component.SequentialExecution;
import com.linkedpipes.etl.executor.api.v1.rdf.model.RdfSource;
import com.linkedpipes.etl.executor.api.v1.rdf.pojo.RdfToPojoLoader;
import com.linkedpipes.etl.executor.api.v1.service.ProgressReport;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.impl.SimpleDataset;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SparqlConstructToFileList
        implements Component, SequentialExecution {

    private static final Logger LOG =
            LoggerFactory.getLogger(SparqlConstructToFileList.class);

    @Component.InputPort(iri = "InputRdf")
    public SingleGraphDataUnit inputRdf;

    @Component.InputPort(iri = "OutputFiles")
    public WritableFilesDataUnit outputFiles;

    @Component.InputPort(iri = "Tasks")
    public SingleGraphDataUnit tasksRdf;

    @Component.ContainsConfiguration
    @Component.InputPort(iri = "Configuration")
    public SingleGraphDataUnit configurationRdf;

    @Component.Inject
    public ProgressReport progressReport;

    @Configuration
    public SparqlConstructToFileListConfiguration configuration;

    private List<TaskGroup> taskGroups;

    private final ValueFactory valueFactory = SimpleValueFactory.getInstance();

    private final Map<String, String> namespaces = new HashMap<>();

    @Override
    public void execute() throws LpException {
        loadNamespaces();
        loadTasksGroups();
        executeTaskGroups();
    }

    private void loadNamespaces() throws LpException {
        if (configuration.getPrefixTurtle() == null) {
            return;
        }
        RDFParser parser = Rio.createParser(RDFFormat.TURTLE);
        try {
            Reader reader = new StringReader(configuration.getPrefixTurtle());
            parser.setRDFHandler(new AbstractRDFHandler() {
                @Override
                public void handleNamespace(String prefix, String uri) {
                    namespaces.put(prefix, uri);
                }
            });
            parser.parse(reader, "http://localhost");
        } catch (IOException ex) {
            throw new LpException(
                    "Can't parse TTL with prefixes.", ex);
        }
    }

    private void loadTasksGroups() throws LpException {
        RdfSource source = tasksRdf.asRdfSource();
        List<String> resources = source.getByType(
                SparqlConstructToFileListVocabulary.TASK);
        taskGroups = new ArrayList<>(resources.size());
        for (String resource : resources) {
            TaskGroup task = new TaskGroup();
            RdfToPojoLoader.loadByReflection(source, resource, task);
            taskGroups.add(task);
        }
    }

    private void executeTaskGroups() throws LpException {
        progressReport.start(taskGroups.size());
        for (TaskGroup group : taskGroups) {
            File outputFile = outputFiles.createFile(group.getFileName());
            RDFFormat format = getRdfFormat(group);
            executeTasks(outputFile, format, group.getTasks());
            progressReport.entryProcessed();
        }
        progressReport.done();
    }

    private RDFFormat getRdfFormat(TaskGroup group) throws LpException {
        Optional<RDFFormat> format =
                Rio.getParserFormatForMIMEType(group.getFormat());
        if (format.isPresent()) {
            return format.get();
        } else {
            throw new LpException("Can't determine format for: {}",
                    group.getFormat());
        }
    }

    private void executeTasks(File outputFile, RDFFormat format,
                              List<QueryTask> tasks) throws LpException {
        try (OutputStream stream = new FileOutputStream(outputFile)) {
            RDFWriter writer = Rio.createWriter(format, stream);
            writer.startRDF();
            addNamespaces(writer);
            for (QueryTask task : tasks) {
                executeTask(task, writer);
            }
            writer.endRDF();
        } catch (IOException ex) {
            throw new LpException("Can't write result file.", ex);
        }
    }

    private void addNamespaces(RDFWriter writer) {
        for (Map.Entry<String, String> entry : namespaces.entrySet()) {
            writer.handleNamespace(entry.getKey(), entry.getValue());
        }
    }

    private void executeTask(QueryTask task, RDFWriter writer)
            throws LpException {
        writer = new ChangeContext(writer, createIriOrNull(task.getGraph()));
        LOG.info("Executing query:\n{}", task.getQuery());
        for (Statement statement : executeQuery(task.getQuery())) {
            writer.handleStatement(statement);
        }
    }

    private IRI createIriOrNull(String iriAsString) {
        if (iriAsString == null) {
            return null;
        } else {
            return valueFactory.createIRI(iriAsString);
        }
    }

    private List<Statement> executeQuery(String queryAsString)
            throws LpException {
        return inputRdf.execute((connection -> {
            List<Statement> statements = new LinkedList<>();
            GraphQuery query = connection.prepareGraphQuery(
                    QueryLanguage.SPARQL, queryAsString);
            SimpleDataset dataset = new SimpleDataset();
            dataset.addDefaultGraph(inputRdf.getReadGraph());
            query.setDataset(dataset);
            GraphQueryResult queryResult = query.evaluate();
            if (configuration.isUseDeduplication()) {
                // Sparql construct does not return distinct results by default:
                // https://github.com/eclipse/rdf4j/issues/857
                queryResult = QueryResults.distinctResults(queryResult);
            }
            while (queryResult.hasNext()) {
                statements.add(queryResult.next());
            }
            return statements;
        }));
    }

}
