package org.visallo.web.routes.vertex;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Optional;
import com.v5analytics.webster.annotations.Required;
import org.vertexium.*;
import org.vertexium.mutation.ElementMutation;
import org.visallo.core.ingest.ArtifactDetectedObject;
import org.visallo.core.model.ontology.Concept;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.termMention.TermMentionRepository;
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
import org.visallo.web.BadRequestException;
import org.visallo.web.clientapi.model.ClientApiElement;
import org.visallo.web.clientapi.model.ClientApiSourceInfo;
import org.visallo.web.clientapi.model.VisibilityJson;
import org.visallo.web.parameterProviders.ActiveWorkspaceId;
import org.visallo.web.parameterProviders.JustificationText;

import java.util.ResourceBundle;

public class ResolveDetectedObject implements ParameterizedHandler {
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
            final WorkQueueRepository workQueueRepository,
            final VisibilityTranslator visibilityTranslator,
            final WorkspaceRepository workspaceRepository,
            final TermMentionRepository termMentionRepository
    ) {
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

    @Handle
    public ClientApiElement handle(
            @Required(name = "artifactId") String artifactId,
            @Required(name = "title") String title,
            @Required(name = "conceptId") String conceptId,
            @Required(name = "visibilitySource") String visibilitySource,
            @Optional(name = "graphVertexId") String graphVertexId,
            @JustificationText String justificationText,
            @Optional(name = "sourceInfo") String sourceInfoString,
            @Optional(name = "originalPropertyKey") String originalPropertyKey,
            @Required(name = "x1") double x1,
            @Required(name = "x2") double x2,
            @Required(name = "y1") double y1,
            @Required(name = "y2") double y2,
            ResourceBundle resourceBundle,
            @ActiveWorkspaceId String workspaceId,
            User user,
            Authorizations authorizations
    ) throws Exception {
        if (this.artifactContainsImageOfEntityIri == null) {
            this.artifactContainsImageOfEntityIri = ontologyRepository.getRequiredRelationshipIRIByIntent("artifactContainsImageOfEntity");
        }

        Workspace workspace = workspaceRepository.findById(workspaceId, user);

        if (!graph.isVisibilityValid(visibilityTranslator.toVisibility(visibilitySource).getVisibility(), authorizations)) {
            LOGGER.warn("%s is not a valid visibility for %s user", visibilitySource, user.getDisplayName());
            throw new BadRequestException("visibilitySource", resourceBundle.getString("visibility.invalid"));
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

            graph.flush();

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

        workQueueRepository.broadcastElement(edge, workspaceId);
        workQueueRepository.pushGraphPropertyQueue(artifactVertex, propertyKey, VisalloProperties.DETECTED_OBJECT.getPropertyName(), Priority.HIGH);

        return ClientApiConverter.toClientApi(artifactVertex, workspaceId, authorizations);
    }
}
