package org.visallo.web.structuredingest.core.worker;

import com.google.inject.Inject;
import org.json.JSONObject;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.Vertex;
import org.vertexium.property.StreamingPropertyValue;
import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.Description;
import org.visallo.core.model.Name;
import org.visallo.core.model.longRunningProcess.LongRunningProcessRepository;
import org.visallo.core.model.longRunningProcess.LongRunningProcessWorker;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.user.PrivilegeRepository;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workspace.WorkspaceHelper;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.security.VisibilityTranslator;
import org.visallo.core.user.User;
import org.visallo.core.util.ClientApiConverter;
import org.visallo.web.structuredingest.core.model.StructuredIngestParser;
import org.visallo.web.structuredingest.core.util.StructuredIngestParserFactory;
import org.visallo.web.structuredingest.core.model.StructuredIngestQueueItem;
import org.visallo.web.structuredingest.core.util.GraphBuilderParserHandler;
import org.visallo.web.structuredingest.core.util.ProgressReporter;
import org.visallo.web.structuredingest.core.util.mapping.ParseMapping;

import java.io.InputStream;
import java.text.NumberFormat;

@Name("Structured Import")
@Description("Extracts structured data from csv, and excel")
public class StructuredIngestProcessWorker extends LongRunningProcessWorker {
    public static final String TYPE = "org-visallo-structured-ingest";
    private OntologyRepository ontologyRepository;
    private VisibilityTranslator visibilityTranslator;
    private PrivilegeRepository privilegeRepository;
    private WorkspaceHelper workspaceHelper;
    private WorkspaceRepository workspaceRepository;
    private UserRepository userRepository;
    private Configuration configuration;
    private StructuredIngestParserFactory structuredIngestParserFactory;
    private Graph graph;
    private LongRunningProcessRepository longRunningProcessRepository;

    @Override
    public boolean isHandled(JSONObject longRunningProcessQueueItem) {
        return TYPE.equals(longRunningProcessQueueItem.getString("type"));
    }

    @Override
    protected void processInternal(final JSONObject longRunningProcessQueueItem) {
        StructuredIngestQueueItem structuredIngestQueueItem = ClientApiConverter.toClientApi(longRunningProcessQueueItem.toString(), StructuredIngestQueueItem.class);
        ParseMapping parseMapping = new ParseMapping(ontologyRepository, visibilityTranslator, structuredIngestQueueItem.getWorkspaceId(), structuredIngestQueueItem.getMapping());
        Authorizations authorizations = graph.createAuthorizations(structuredIngestQueueItem.getAuthorizations());
        Vertex vertex = graph.getVertex(structuredIngestQueueItem.getVertexId(), authorizations);
        User user = userRepository.findById(structuredIngestQueueItem.getUserId());
        StreamingPropertyValue rawPropertyValue = VisalloProperties.RAW.getPropertyValue(vertex);
        NumberFormat numberFormat = NumberFormat.getIntegerInstance();

        ProgressReporter reporter = new ProgressReporter() {
            public void finishedRow(long row, long totalRows) {
                if (totalRows != -1) {
                    longRunningProcessRepository.reportProgress(
                            longRunningProcessQueueItem,
                            ((float)row) / ((float) totalRows),
                            "Row " + numberFormat.format(row) + " of " + numberFormat.format(totalRows));
                }
            }
        };
        GraphBuilderParserHandler parserHandler = new GraphBuilderParserHandler(
                graph,
                user,
                visibilityTranslator,
                privilegeRepository,
                authorizations,
                workspaceRepository,
                workspaceHelper,
                structuredIngestQueueItem.getWorkspaceId(),
                structuredIngestQueueItem.isPublish(),
                vertex,
                parseMapping,
                reporter);


        longRunningProcessRepository.reportProgress(longRunningProcessQueueItem, 0, "Deleting previous imports");
        parserHandler.cleanUpExistingImport();

        parserHandler.dryRun = false;
        parserHandler.reset();
        try {
            parse(vertex, rawPropertyValue, parserHandler, structuredIngestQueueItem);
        } catch (Exception e) {
            throw new VisalloException("Unable to ingest vertex: " + vertex, e);
        }
    }

    private void parse(Vertex vertex, StreamingPropertyValue rawPropertyValue, GraphBuilderParserHandler parserHandler, StructuredIngestQueueItem item) throws Exception {
        String mimeType = (String) vertex.getPropertyValue(VisalloProperties.MIME_TYPE.getPropertyName());
        if (mimeType == null) {
            throw new VisalloException("No mimeType property found for vertex");
        }

        StructuredIngestParser structuredIngestParser = structuredIngestParserFactory.getParser(mimeType);
        if (structuredIngestParser == null) {
            throw new VisalloException("No parser registered for mimeType: " + mimeType);
        }

        try (InputStream in = rawPropertyValue.getInputStream()) {
            structuredIngestParser.ingest(in, item.getParseOptions(), parserHandler);
        }
    }

    @Inject
    public void setPrivilegeRepository(PrivilegeRepository privilegeRepository) {
        this.privilegeRepository = privilegeRepository;
    }

    @Inject
    public void setOntologyRepository(OntologyRepository ontologyRepository) {
        this.ontologyRepository = ontologyRepository;
    }

    @Inject
    public void setVisibilityTranslator(VisibilityTranslator visibilityTranslator) {
        this.visibilityTranslator = visibilityTranslator;
    }

    @Inject
    public void setWorkspaceHelper(WorkspaceHelper workspaceHelper) {
        this.workspaceHelper = workspaceHelper;
    }

    @Inject
    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Inject
    public void setWorkspaceRepository(WorkspaceRepository workspaceRepository) {
        this.workspaceRepository = workspaceRepository;
    }

    @Inject
    public void setStructuredIngestParserFactory(StructuredIngestParserFactory structuredIngestParserFactory) {
        this.structuredIngestParserFactory = structuredIngestParserFactory;
    }

    @Inject
    public void setGraph(Graph graph) {
        this.graph = graph;
    }

    @Inject
    public void setLongRunningProcessRepository(LongRunningProcessRepository longRunningProcessRepository) {
        this.longRunningProcessRepository = longRunningProcessRepository;
    }

    @Inject
    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }
}


