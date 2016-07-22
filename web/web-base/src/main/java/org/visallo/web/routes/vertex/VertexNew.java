package org.visallo.web.routes.vertex;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Optional;
import com.v5analytics.webster.annotations.Required;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.Metadata;
import org.vertexium.Vertex;
import org.visallo.core.model.graph.GraphRepository;
import org.visallo.core.model.graph.VisibilityAndElementMutation;
import org.visallo.core.model.ontology.OntologyProperty;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.model.workspace.Workspace;
import org.visallo.core.model.workspace.WorkspaceHelper;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.security.VisibilityTranslator;
import org.visallo.core.user.User;
import org.visallo.core.util.ClientApiConverter;
import org.visallo.core.util.VertexiumMetadataUtil;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.BadRequestException;
import org.visallo.web.clientapi.model.ClientApiAddElementProperties;
import org.visallo.web.clientapi.model.ClientApiElement;
import org.visallo.web.clientapi.model.ClientApiSourceInfo;
import org.visallo.web.parameterProviders.ActiveWorkspaceId;
import org.visallo.web.parameterProviders.JustificationText;

import java.util.ResourceBundle;

import static com.google.common.base.Preconditions.checkNotNull;

public class VertexNew implements ParameterizedHandler {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(VertexNew.class);

    private final Graph graph;
    private final VisibilityTranslator visibilityTranslator;
    private final WorkspaceRepository workspaceRepository;
    private final WorkQueueRepository workQueueRepository;
    private final OntologyRepository ontologyRepository;
    private final GraphRepository graphRepository;
    private final WorkspaceHelper workspaceHelper;

    @Inject
    public VertexNew(
            Graph graph,
            VisibilityTranslator visibilityTranslator,
            WorkspaceRepository workspaceRepository,
            WorkQueueRepository workQueueRepository,
            OntologyRepository ontologyRepository,
            GraphRepository graphRepository,
            WorkspaceHelper workspaceHelper
    ) {
        this.graph = graph;
        this.visibilityTranslator = visibilityTranslator;
        this.workspaceRepository = workspaceRepository;
        this.workQueueRepository = workQueueRepository;
        this.ontologyRepository = ontologyRepository;
        this.graphRepository = graphRepository;
        this.workspaceHelper = workspaceHelper;
    }

    @Handle
    public ClientApiElement handle(
            @Optional(name = "vertexId", allowEmpty = false) String vertexId,
            @Required(name = "conceptType", allowEmpty = false) String conceptType,
            @Required(name = "visibilitySource") String visibilitySource,
            @Optional(name = "properties", allowEmpty = false) String propertiesJsonString,
            @Optional(name = "publish", defaultValue = "false") boolean shouldPublish,
            @JustificationText String justificationText,
            ClientApiSourceInfo sourceInfo,
            @ActiveWorkspaceId(required = false) String workspaceId,
            ResourceBundle resourceBundle,
            User user,
            Authorizations authorizations
    ) throws Exception {
        if (!graph.isVisibilityValid(
                visibilityTranslator.toVisibility(visibilitySource).getVisibility(),
                authorizations
        )) {
            LOGGER.warn("%s is not a valid visibility for %s user", visibilitySource, user.getDisplayName());
            throw new BadRequestException("visibilitySource", resourceBundle.getString("visibility.invalid"));
        }

        workspaceId = workspaceHelper.getWorkspaceIdOrNullIfPublish(workspaceId, shouldPublish, user);

        Vertex vertex = graphRepository.addVertex(
                vertexId,
                conceptType,
                visibilitySource,
                workspaceId,
                justificationText,
                sourceInfo,
                user,
                authorizations
        );

        ClientApiAddElementProperties properties = null;
        if (propertiesJsonString != null && propertiesJsonString.length() > 0) {
            properties = ClientApiConverter.toClientApi(propertiesJsonString, ClientApiAddElementProperties.class);
            for (ClientApiAddElementProperties.Property property : properties.properties) {
                OntologyProperty ontologyProperty = ontologyRepository.getPropertyByIRI(property.propertyName);
                checkNotNull(ontologyProperty, "Could not find ontology property '" + property.propertyName + "'");
                Object value = ontologyProperty.convertString(property.value);
                Metadata metadata = VertexiumMetadataUtil.metadataStringToMap(
                        property.metadataString,
                        this.visibilityTranslator.getDefaultVisibility()
                );
                VisibilityAndElementMutation<Vertex> setPropertyResult = graphRepository.setProperty(
                        vertex,
                        property.propertyName,
                        property.propertyKey,
                        value,
                        metadata,
                        null,
                        property.visibilitySource,
                        workspaceId,
                        justificationText,
                        sourceInfo,
                        user,
                        authorizations
                );
                setPropertyResult.elementMutation.save(authorizations);
            }
        }
        this.graph.flush();

        LOGGER.debug("Created new empty vertex with id: %s", vertex.getId());

        workQueueRepository.broadcastElement(vertex, workspaceId);
        workQueueRepository.pushGraphPropertyQueue(
                vertex,
                null,
                VisalloProperties.CONCEPT_TYPE.getPropertyName(),
                workspaceId,
                visibilitySource,
                Priority.HIGH
        );

        if (workspaceId != null) {
            Workspace workspace = workspaceRepository.findById(workspaceId, user);
            workspaceRepository.updateEntityOnWorkspace(workspace, vertex.getId(), true, null, user);
            workQueueRepository.pushUserCurrentWorkspaceChange(user, workspaceId);
            this.graph.flush();
        }

        if (properties != null) {
            for (ClientApiAddElementProperties.Property property : properties.properties) {
                workQueueRepository.pushGraphPropertyQueue(
                        vertex,
                        property.propertyKey,
                        property.propertyName,
                        workspaceId,
                        property.visibilitySource,
                        Priority.HIGH
                );
            }
        }

        return ClientApiConverter.toClientApi(vertex, workspaceId, authorizations);
    }
}
