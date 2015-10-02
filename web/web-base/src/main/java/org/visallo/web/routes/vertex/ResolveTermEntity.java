package org.visallo.web.routes.vertex;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Optional;
import com.v5analytics.webster.annotations.Required;
import org.vertexium.*;
import org.vertexium.mutation.ElementMutation;
import org.visallo.core.model.PropertyJustificationMetadata;
import org.visallo.core.model.ontology.Concept;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.termMention.TermMentionBuilder;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.model.workspace.Workspace;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.security.VisalloVisibility;
import org.visallo.core.security.VisibilityTranslator;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.BadRequestException;
import org.visallo.web.VisalloResponse;
import org.visallo.web.clientapi.model.ClientApiSourceInfo;
import org.visallo.web.clientapi.model.ClientApiSuccess;
import org.visallo.web.clientapi.model.VisibilityJson;
import org.visallo.web.parameterProviders.ActiveWorkspaceId;
import org.visallo.web.parameterProviders.JustificationText;

import java.util.ResourceBundle;

public class ResolveTermEntity implements ParameterizedHandler {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(ResolveTermEntity.class);
    private static final String MULTI_VALUE_KEY = ResolveTermEntity.class.getName();
    private final Graph graph;
    private final OntologyRepository ontologyRepository;
    private final VisibilityTranslator visibilityTranslator;
    private final WorkspaceRepository workspaceRepository;
    private final WorkQueueRepository workQueueRepository;
    private String artifactHasEntityIri;

    @Inject
    public ResolveTermEntity(
            final Graph graphRepository,
            final OntologyRepository ontologyRepository,
            final VisibilityTranslator visibilityTranslator,
            final WorkspaceRepository workspaceRepository,
            final WorkQueueRepository workQueueRepository
    ) {
        this.graph = graphRepository;
        this.ontologyRepository = ontologyRepository;
        this.visibilityTranslator = visibilityTranslator;
        this.workspaceRepository = workspaceRepository;
        this.workQueueRepository = workQueueRepository;
        this.artifactHasEntityIri = ontologyRepository.getRelationshipIRIByIntent("artifactHasEntity");
        if (this.artifactHasEntityIri == null) {
            LOGGER.warn("'artifactHasEntity' intent has not been defined. Please update your ontology.");
        }
    }

    @Handle
    public ClientApiSuccess handle(
            @Required(name = "artifactId") String artifactId,
            @Required(name = "propertyKey") String propertyKey,
            @Required(name = "mentionStart") long mentionStart,
            @Required(name = "mentionEnd") long mentionEnd,
            @Required(name = "sign") String title,
            @Required(name = "conceptId") String conceptId,
            @Required(name = "visibilitySource") String visibilitySource,
            @Optional(name = "resolvedVertexId") String resolvedVertexId,
            @Optional(name = "sourceInfo") String sourceInfoString,
            @JustificationText String justificationText,
            @ActiveWorkspaceId String workspaceId,
            ResourceBundle resourceBundle,
            User user,
            Authorizations authorizations
    ) throws Exception {
        if (this.artifactHasEntityIri == null) {
            this.artifactHasEntityIri = ontologyRepository.getRequiredRelationshipIRIByIntent("artifactHasEntity");
        }

        Workspace workspace = workspaceRepository.findById(workspaceId, user);

        VisibilityJson visibilityJson = VisibilityJson.updateVisibilitySourceAndAddWorkspaceId(null, visibilitySource, workspaceId);
        VisalloVisibility visibility = this.visibilityTranslator.toVisibility(visibilityJson);
        if (!graph.isVisibilityValid(visibility.getVisibility(), authorizations)) {
            LOGGER.warn("%s is not a valid visibility for %s user", visibilitySource, user.getDisplayName());
            throw new BadRequestException("visibilitySource", resourceBundle.getString("visibility.invalid"));
        }

        String id = resolvedVertexId == null ? graph.getIdGenerator().nextId() : resolvedVertexId;

        Concept concept = ontologyRepository.getConceptByIRI(conceptId);

        final Vertex artifactVertex = graph.getVertex(artifactId, authorizations);
        VisalloVisibility visalloVisibility = visibilityTranslator.toVisibility(visibilityJson);
        Metadata metadata = new Metadata();
        VisalloProperties.VISIBILITY_JSON_METADATA.setMetadata(metadata, visibilityJson, visibilityTranslator.getDefaultVisibility());
        ElementMutation<Vertex> vertexMutation;
        Vertex vertex;
        if (resolvedVertexId != null) {
            vertex = graph.getVertex(id, authorizations);
            vertexMutation = vertex.prepareMutation();
        } else {
            vertexMutation = graph.prepareVertex(id, visalloVisibility.getVisibility());
            VisalloProperties.CONCEPT_TYPE.setProperty(vertexMutation, conceptId, metadata, visalloVisibility.getVisibility());
            VisalloProperties.TITLE.addPropertyValue(vertexMutation, MULTI_VALUE_KEY, title, metadata, visalloVisibility.getVisibility());
            vertex = vertexMutation.save(authorizations);

            if (justificationText != null) {
                PropertyJustificationMetadata propertyJustificationMetadata = new PropertyJustificationMetadata(justificationText);
                VisalloProperties.JUSTIFICATION.setProperty(vertex, propertyJustificationMetadata, visalloVisibility.getVisibility(), authorizations);
            }

            VisalloProperties.VISIBILITY_JSON.setProperty(vertexMutation, visibilityJson, metadata, visalloVisibility.getVisibility());

            this.graph.flush();

            workspaceRepository.updateEntityOnWorkspace(workspace, vertex.getId(), null, null, user);
        }

        // TODO: a better way to check if the same edge exists instead of looking it up every time?
        Edge edge = graph.addEdge(artifactVertex, vertex, this.artifactHasEntityIri, visalloVisibility.getVisibility(), authorizations);
        VisalloProperties.VISIBILITY_JSON.setProperty(edge, visibilityJson, metadata, visalloVisibility.getVisibility(), authorizations);

        ClientApiSourceInfo sourceInfo = ClientApiSourceInfo.fromString(sourceInfoString);
        new TermMentionBuilder()
                .outVertex(artifactVertex)
                .propertyKey(propertyKey)
                .start(mentionStart)
                .end(mentionEnd)
                .title(title)
                .snippet(sourceInfo == null ? null : sourceInfo.snippet)
                .conceptIri(concept.getIRI())
                .visibilityJson(visibilityJson)
                .resolvedTo(vertex, edge)
                .process(getClass().getSimpleName())
                .save(this.graph, visibilityTranslator, authorizations);

        vertexMutation.save(authorizations);

        this.graph.flush();
        workQueueRepository.pushTextUpdated(artifactId);

        workQueueRepository.broadcastElement(edge, workspaceId);

        return VisalloResponse.SUCCESS;
    }
}
