package org.visallo.web.structuredingest.core.routes;

import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Optional;
import com.v5analytics.webster.annotations.Required;
import org.json.JSONArray;
import org.json.JSONObject;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.Vertex;
import org.vertexium.property.StreamingPropertyValue;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.exception.VisalloResourceNotFoundException;
import org.visallo.core.model.longRunningProcess.LongRunningProcessRepository;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.user.PrivilegeRepository;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.model.workspace.WorkspaceHelper;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.security.VisibilityTranslator;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.VisalloResponse;
import org.visallo.web.clientapi.model.ClientApiObject;
import org.visallo.web.parameterProviders.ActiveWorkspaceId;
import org.visallo.web.structuredingest.core.model.ClientApiMappingErrors;
import org.visallo.web.structuredingest.core.model.StructuredIngestParser;
import org.visallo.web.structuredingest.core.util.StructuredIngestParserFactory;
import org.visallo.web.structuredingest.core.model.StructuredIngestQueueItem;
import org.visallo.web.structuredingest.core.util.BaseStructuredFileParserHandler;
import org.visallo.web.structuredingest.core.util.GraphBuilderParserHandler;
import org.visallo.web.structuredingest.core.model.ParseOptions;
import org.visallo.web.structuredingest.core.util.ProgressReporter;
import org.visallo.web.structuredingest.core.util.mapping.ParseMapping;
import org.visallo.web.structuredingest.core.worker.StructuredIngestProcessWorker;

import javax.inject.Inject;
import java.io.InputStream;

public class Ingest implements ParameterizedHandler {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(Ingest.class);

    private final LongRunningProcessRepository longRunningProcessRepository;
    private final OntologyRepository ontologyRepository;
    private final PrivilegeRepository privilegeRepository;
    private final VisibilityTranslator visibilityTranslator;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceHelper workspaceHelper;
    private final WorkQueueRepository workQueueRepository;
    private final Graph graph;
    private final StructuredIngestParserFactory structuredIngestParserFactory;

    @Inject
    public Ingest(
        LongRunningProcessRepository longRunningProcessRepository,
        OntologyRepository ontologyRepository,
        PrivilegeRepository privilegeRepository,
        WorkspaceRepository workspaceRepository,
        WorkspaceHelper workspaceHelper,
        StructuredIngestParserFactory structuredIngestParserFactory,
        WorkQueueRepository workQueueRepository,
        VisibilityTranslator visibilityTranslator,
        Graph graph
    ) {
        this.longRunningProcessRepository = longRunningProcessRepository;
        this.ontologyRepository = ontologyRepository;
        this.privilegeRepository = privilegeRepository;
        this.workspaceHelper = workspaceHelper;
        this.workspaceRepository = workspaceRepository;
        this.visibilityTranslator = visibilityTranslator;
        this.structuredIngestParserFactory = structuredIngestParserFactory;
        this.workQueueRepository = workQueueRepository;
        this.graph = graph;
    }

    @Handle
    public ClientApiObject handle(
            User user,
            @ActiveWorkspaceId String workspaceId,
            Authorizations authorizations,
            @Required(name = "graphVertexId") String graphVertexId,
            @Required(name = "mapping") String mapping,
            @Optional(name = "parseOptions") String optionsJson,
            @Optional(name = "publish", defaultValue = "false") boolean publish,
            @Optional(name = "preview", defaultValue = "true") boolean preview
    ) throws Exception {

        Vertex vertex = graph.getVertex(graphVertexId, authorizations);
        if (vertex == null) {
            throw new VisalloResourceNotFoundException("Could not find vertex:" + graphVertexId);
        }

        StreamingPropertyValue rawPropertyValue = VisalloProperties.RAW.getPropertyValue(vertex);
        if (rawPropertyValue == null) {
            throw new VisalloResourceNotFoundException("Could not find raw property on vertex:" + graphVertexId);
        }

        ParseMapping parseMapping = new ParseMapping(ontologyRepository, visibilityTranslator, workspaceId, mapping);
        ClientApiMappingErrors mappingErrors = parseMapping.validate(authorizations);
        if (mappingErrors.mappingErrors.size() > 0) {
            return mappingErrors;
        }


        if (preview) {
            return previewIngest(user, workspaceId, authorizations, optionsJson, publish, vertex, rawPropertyValue, parseMapping);
        } else {
            return enqueueIngest(user, workspaceId, authorizations, graphVertexId, mapping, optionsJson, publish);
        }
    }

    private ClientApiObject enqueueIngest(User user, String workspaceId, Authorizations authorizations, String graphVertexId, String mapping, String optionsJson, boolean publish) {
        StructuredIngestQueueItem queueItem = new StructuredIngestQueueItem(workspaceId, mapping, user.getUserId(), graphVertexId, StructuredIngestProcessWorker.TYPE, new ParseOptions(optionsJson), publish, authorizations);
        this.longRunningProcessRepository.enqueue(queueItem.toJson(), user, authorizations);
        return VisalloResponse.SUCCESS;
    }

    private ClientApiObject previewIngest(User user, String workspaceId, Authorizations authorizations, String optionsJson, boolean publish, Vertex vertex, StreamingPropertyValue rawPropertyValue, ParseMapping parseMapping) throws Exception {
        JSONObject data = new JSONObject();
        JSONObject permissions = new JSONObject();
        JSONArray users = new JSONArray();
        users.put(user.getUserId());
        permissions.put("users", users);

        ProgressReporter reporter = new ProgressReporter() {
            public void finishedRow(long row, long totalRows) {
                if (totalRows != -1) {
                    long total = Math.min(GraphBuilderParserHandler.MAX_DRY_RUN_ROWS, totalRows);
                    data.put("row", row);
                    data.put("total", total);

                    // Broadcast when we get this change in percent
                    int percent = (int) ((double)total * 0.01);

                    if (percent > 0 && row % percent == 0) {
                        workQueueRepository.broadcast("structuredImportDryrun", data, permissions);
                    }
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
                workspaceId,
                publish,
                vertex,
                parseMapping,
                reporter);

        parserHandler.dryRun = true;
        ParseOptions parseOptions = new ParseOptions(optionsJson);

        parse(vertex, rawPropertyValue, parseOptions, parserHandler);

        if (parserHandler.hasErrors()) {
            return parserHandler.parseErrors;
        }
        return parserHandler.clientApiIngestPreview;
    }

    private void parse(Vertex vertex, StreamingPropertyValue rawPropertyValue, ParseOptions parseOptions, BaseStructuredFileParserHandler parserHandler) throws Exception {
        String mimeType = (String) vertex.getPropertyValue(VisalloProperties.MIME_TYPE.getPropertyName());
        if (mimeType == null) {
            throw new VisalloException("No mimeType property found for vertex");
        }

        StructuredIngestParser structuredIngestParser = structuredIngestParserFactory.getParser(mimeType);
        if (structuredIngestParser == null) {
            throw new VisalloException("No parser registered for mimeType: " + mimeType);
        }

        try (InputStream in = rawPropertyValue.getInputStream()) {
            structuredIngestParser.ingest(in, parseOptions, parserHandler);
        }
    }
}