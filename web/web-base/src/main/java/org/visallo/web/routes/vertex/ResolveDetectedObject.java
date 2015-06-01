package org.visallo.web.routes.vertex;

import com.google.inject.Inject;
import com.v5analytics.webster.HandlerChain;
import org.vertexium.*;
import org.vertexium.mutation.ElementMutation;
import org.visallo.core.config.Configuration;
import org.visallo.core.ingest.ArtifactDetectedObject;
import org.visallo.core.model.ontology.Concept;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.termMention.TermMentionRepository;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.model.workspace.Workspace;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.security.VisalloVisibility;
import org.visallo.core.security.VisibilityTranslator;
import org.visallo.core.user.User;
import org.visallo.core.util.ClientApiConverter;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.BaseRequestHandler;
import org.visallo.web.WebConfiguration;
import org.visallo.web.clientapi.model.ClientApiElement;
import org.visallo.web.clientapi.model.ClientApiSourceInfo;
import org.visallo.web.clientapi.model.VisibilityJson;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ResolveDetectedObject extends BaseRequestHandler {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(ResolveDetectedObject.class);
    private static final String MULTI_VALUE_KEY_PREFIX = ResolveDetectedObject.class.getName();
    private static final String MULTI_VALUE_KEY = ResolveDetectedObject.class.getName();
    private final Graph graph;
    private final OntologyRepository ontologyRepository;
    private final WorkQueueRepository workQueueRepository;
    private final VisibilityTranslator visibilityTranslator;
    private final WorkspaceRepository workspaceRepository;
    private final TermMentionRepository termMentionRepository;
    private String artifactContainsImageOfEntityIri;

    @Inject
    public ResolveDetectedObject(
            final Graph graph,
            final OntologyRepository ontologyRepository,
            final UserRepository userRepository,
            final Configuration configuration,
            final WorkQueueRepository workQueueRepository,
            final VisibilityTranslator visibilityTranslator,
            final WorkspaceRepository workspaceRepository,
            final TermMentionRepository termMentionRepository
    ) {
        super(userRepository, workspaceRepository, configuration);
        this.graph = graph;
        this.ontologyRepository = ontologyRepository;
        this.workQueueRepository = workQueueRepository;
        this.visibilityTranslator = visibilityTranslator;
        this.workspaceRepository = workspaceRepository;
        this.termMentionRepository = termMentionRepository;

        this.artifactContainsImageOfEntityIri = ontologyRepository.getRelationshipIRIByIntent("artifactContainsImageOfEntity");
        if (this.artifactContainsImageOfEntityIri == null) {
            LOGGER.warn("'artifactContainsImageOfEntity' intent has not been defined. Please update your ontology.");
        }
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        if (this.artifactContainsImageOfEntityIri == null) {
            this.artifactContainsImageOfEntityIri = ontologyRepository.getRequiredRelationshipIRIByIntent("artifactContainsImageOfEntity");
        }

        final String artifactId = getRequiredParameter(request, "artifactId");
        final String title = getRequiredParameter(request, "title");
        final String conceptId = getRequiredParameter(request, "conceptId");
        final String visibilitySource = getRequiredParameter(request, "visibilitySource");
        final String graphVertexId = getOptionalParameter(request, "graphVertexId");
        final String justificationText = WebConfiguration.justificationRequired(getConfiguration()) ? getRequiredParameter(request, "justificationText") : getOptionalParameter(request, "justificationText");
        final String sourceInfoString = getOptionalParameter(request, "sourceInfo");
        String originalPropertyKey = getOptionalParameter(request, "originalPropertyKey");
        double x1 = Double.parseDouble(getRequiredParameter(request, "x1"));
        double x2 = Double.parseDouble(getRequiredParameter(request, "x2"));
        double y1 = Double.parseDouble(getRequiredParameter(request, "y1"));
        double y2 = Double.parseDouble(getRequiredParameter(request, "y2"));

        User user = getUser(request);
        String workspaceId = getActiveWorkspaceId(request);
        Workspace workspace = workspaceRepository.findById(workspaceId, user);
        Authorizations authorizations = getAuthorizations(request, user);

        if (!graph.isVisibilityValid(new Visibility(visibilitySource), authorizations)) {
            LOGGER.warn("%s is not a valid visibility for %s user", visibilitySource, user.getDisplayName());
            respondWithBadRequest(response, "visibilitySource", getString(request, "visibility.invalid"));
            chain.next(request, response);
            return;
        }

        VisibilityJson visibilityJson = VisibilityJson.updateVisibilitySourceAndAddWorkspaceId(null, visibilitySource, workspaceId);
        VisalloVisibility visalloVisibility = visibilityTranslator.toVisibility(visibilityJson);

        Concept concept = ontologyRepository.getConceptByIRI(conceptId);
        Vertex artifactVertex = graph.getVertex(artifactId, authorizations);
        ElementMutation<Vertex> resolvedVertexMutation;

        Metadata metadata = new Metadata();
        VisalloProperties.VISIBILITY_JSON_METADATA.setMetadata(metadata, visibilityJson, visibilityTranslator.getDefaultVisibility());

        Vertex resolvedVertex;
        if (graphVertexId == null || graphVertexId.equals("")) {
            resolvedVertexMutation = graph.prepareVertex(visalloVisibility.getVisibility());

            VisalloProperties.CONCEPT_TYPE.setProperty(resolvedVertexMutation, concept.getIRI(), metadata, visalloVisibility.getVisibility());
            VisalloProperties.TITLE.addPropertyValue(resolvedVertexMutation, MULTI_VALUE_KEY, title, metadata, visalloVisibility.getVisibility());

            resolvedVertex = resolvedVertexMutation.save(authorizations);
            ClientApiSourceInfo sourceInfo = ClientApiSourceInfo.fromString(sourceInfoString);
            termMentionRepository.addJustification(resolvedVertex, justificationText, sourceInfo, visalloVisibility, authorizations);

            resolvedVertex = resolvedVertexMutation.save(authorizations);

            VisalloProperties.VISIBILITY_JSON.setProperty(resolvedVertexMutation, visibilityJson, metadata, visalloVisibility.getVisibility());

            graph.flush();

            workspaceRepository.updateEntityOnWorkspace(workspace, resolvedVertex.getId(), null, null, user);
        } else {
            resolvedVertex = graph.getVertex(graphVertexId, authorizations);
            resolvedVertexMutation = resolvedVertex.prepareMutation();
        }

        Edge edge = graph.addEdge(artifactVertex, resolvedVertex, artifactContainsImageOfEntityIri, visalloVisibility.getVisibility(), authorizations);
        VisalloProperties.VISIBILITY_JSON.setProperty(edge, visibilityJson, metadata, visalloVisibility.getVisibility(), authorizations);

        ArtifactDetectedObject artifactDetectedObject = new ArtifactDetectedObject(
                x1,
                y1,
                x2,
                y2,
                concept.getIRI(),
                "user",
                edge.getId(),
                resolvedVertex.getId(),
                originalPropertyKey);
        String propertyKey = artifactDetectedObject.getMultivalueKey(MULTI_VALUE_KEY_PREFIX);
        VisalloProperties.DETECTED_OBJECT.addPropertyValue(artifactVertex, propertyKey, artifactDetectedObject, visalloVisibility.getVisibility(), authorizations);

        resolvedVertexMutation.addPropertyValue(resolvedVertex.getId(), VisalloProperties.ROW_KEY.getPropertyName(), propertyKey, visalloVisibility.getVisibility());
        resolvedVertexMutation.save(authorizations);

        graph.flush();

        workQueueRepository.pushElement(edge, Priority.HIGH);
        workQueueRepository.pushGraphPropertyQueue(artifactVertex, propertyKey, VisalloProperties.DETECTED_OBJECT.getPropertyName(), Priority.HIGH);

        ClientApiElement result = ClientApiConverter.toClientApi(artifactVertex, workspaceId, authorizations);
        respondWithClientApiObject(response, result);
    }
}
