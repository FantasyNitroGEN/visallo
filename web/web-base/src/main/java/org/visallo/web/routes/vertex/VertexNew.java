package org.visallo.web.routes.vertex;

import com.google.inject.Inject;
import com.v5analytics.webster.HandlerChain;
import org.vertexium.*;
import org.visallo.core.config.Configuration;
import org.visallo.core.model.graph.GraphRepository;
import org.visallo.core.model.graph.VisibilityAndElementMutation;
import org.visallo.core.model.ontology.OntologyProperty;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.model.workspace.Workspace;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.security.VisibilityTranslator;
import org.visallo.core.user.User;
import org.visallo.core.util.ClientApiConverter;
import org.visallo.core.util.VertexiumMetadataUtil;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.BaseRequestHandler;
import org.visallo.web.clientapi.model.ClientApiAddElementProperties;
import org.visallo.web.clientapi.model.ClientApiElement;
import org.visallo.web.clientapi.model.ClientApiSourceInfo;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.ParseException;

import static com.google.common.base.Preconditions.checkNotNull;

public class VertexNew extends BaseRequestHandler {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(VertexNew.class);

    private final Graph graph;
    private final VisibilityTranslator visibilityTranslator;
    private final WorkQueueRepository workQueueRepository;
    private final OntologyRepository ontologyRepository;
    private final GraphRepository graphRepository;

    @Inject
    public VertexNew(
            final Graph graph,
            final UserRepository userRepository,
            final WorkspaceRepository workspaceRepository,
            final VisibilityTranslator visibilityTranslator,
            final WorkQueueRepository workQueueRepository,
            final Configuration configuration,
            final OntologyRepository ontologyRepository,
            final GraphRepository graphRepository
    ) {
        super(userRepository, workspaceRepository, configuration);
        this.graph = graph;
        this.visibilityTranslator = visibilityTranslator;
        this.workQueueRepository = workQueueRepository;
        this.ontologyRepository = ontologyRepository;
        this.graphRepository = graphRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final String vertexId = getOptionalParameter(request, "vertexId");
        final String conceptType = getRequiredParameter(request, "conceptType");
        final String visibilitySource = getRequiredParameter(request, "visibilitySource");
        final String justificationText = routeHelper.getJustificationText(request);
        final String sourceInfoString = getOptionalParameter(request, "sourceInfo");
        final String propertiesJsonString = getOptionalParameter(request, "properties");
        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);
        String workspaceId = getActiveWorkspaceId(request);

        if (!graph.isVisibilityValid(new Visibility(visibilitySource), authorizations)) {
            LOGGER.warn("%s is not a valid visibility for %s user", visibilitySource, user.getDisplayName());
            respondWithBadRequest(response, "visibilitySource", getString(request, "visibility.invalid"), visibilitySource);
            chain.next(request, response);
            return;
        }

        ClientApiElement element = handle(
                vertexId,
                conceptType,
                visibilitySource,
                justificationText,
                ClientApiSourceInfo.fromString(sourceInfoString),
                propertiesJsonString,
                user,
                workspaceId,
                authorizations
        );
        respondWithClientApiObject(response, element);
    }

    private ClientApiElement handle(
            String vertexId,
            String conceptType,
            String visibilitySource,
            String justificationText,
            ClientApiSourceInfo sourceInfo,
            String propertiesJsonString,
            User user,
            String workspaceId,
            Authorizations authorizations
    ) throws ParseException {
        Workspace workspace = getWorkspaceRepository().findById(workspaceId, user);

        Vertex vertex = graphRepository.addVertex(
                vertexId,
                conceptType,
                visibilitySource,
                workspaceId,
                justificationText,
                sourceInfo,
                authorizations
        );

        ClientApiAddElementProperties properties = null;
        if (propertiesJsonString != null && propertiesJsonString.length() > 0) {
            properties = ClientApiConverter.toClientApi(propertiesJsonString, ClientApiAddElementProperties.class);
            for (ClientApiAddElementProperties.Property property : properties.properties) {
                OntologyProperty ontologyProperty = ontologyRepository.getPropertyByIRI(property.propertyName);
                checkNotNull(ontologyProperty, "Could not find ontology property '" + property.propertyName + "'");
                Object value = ontologyProperty.convertString(property.value);
                Metadata metadata = VertexiumMetadataUtil.metadataStringToMap(property.metadataString, this.visibilityTranslator.getDefaultVisibility());
                VisibilityAndElementMutation<Vertex> setPropertyResult = graphRepository.setProperty(
                        vertex,
                        property.propertyName,
                        property.propertyKey,
                        value,
                        metadata,
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

        getWorkspaceRepository().updateEntityOnWorkspace(workspace, vertex.getId(), true, null, user);
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
        workQueueRepository.pushUserCurrentWorkspaceChange(user, workspaceId);
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
