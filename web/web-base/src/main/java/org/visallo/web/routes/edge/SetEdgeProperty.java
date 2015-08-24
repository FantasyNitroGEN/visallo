package org.visallo.web.routes.edge;

import com.google.inject.Inject;
import com.v5analytics.webster.HandlerChain;
import org.vertexium.*;
import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.graph.GraphRepository;
import org.visallo.core.model.graph.VisibilityAndElementMutation;
import org.visallo.core.model.ontology.OntologyProperty;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.security.VisibilityTranslator;
import org.visallo.core.user.User;
import org.visallo.core.util.VertexiumMetadataUtil;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.BaseRequestHandler;
import org.visallo.web.clientapi.model.ClientApiSourceInfo;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class SetEdgeProperty extends BaseRequestHandler {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(SetEdgeProperty.class);

    private final Graph graph;
    private final OntologyRepository ontologyRepository;
    private VisibilityTranslator visibilityTranslator;
    private final WorkQueueRepository workQueueRepository;
    private final GraphRepository graphRepository;

    @Inject
    public SetEdgeProperty(
            final OntologyRepository ontologyRepository,
            final Graph graph,
            final VisibilityTranslator visibilityTranslator,
            final UserRepository userRepository,
            final Configuration configuration,
            final WorkspaceRepository workspaceRepository,
            final WorkQueueRepository workQueueRepository,
            final GraphRepository graphRepository
    ) {
        super(userRepository, workspaceRepository, configuration);
        this.ontologyRepository = ontologyRepository;
        this.graph = graph;
        this.visibilityTranslator = visibilityTranslator;
        this.workQueueRepository = workQueueRepository;
        this.graphRepository = graphRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final String edgeId = getRequiredParameter(request, "edgeId");
        final String propertyName = getRequiredParameter(request, "propertyName");
        String propertyKey = getOptionalParameter(request, "propertyKey");
        final String valueStr = getRequiredParameter(request, "value");
        final String visibilitySource = getRequiredParameter(request, "visibilitySource");
        boolean isComment = VisalloProperties.COMMENT.getPropertyName().equals(propertyName);
        final String sourceInfo = getOptionalParameter(request, "sourceInfo");
        final String justificationText = routeHelper.getJustificationText(isComment, sourceInfo, request);
        final String metadataString = getOptionalParameter(request, "metadata");

        String workspaceId = getActiveWorkspaceId(request);

        if (propertyKey == null) {
            propertyKey = this.graph.getIdGenerator().nextId();
        }

        Metadata metadata = VertexiumMetadataUtil.metadataStringToMap(metadataString, visibilityTranslator.getDefaultVisibility());

        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);

        if (!graph.isVisibilityValid(new Visibility(visibilitySource), authorizations)) {
            LOGGER.warn("%s is not a valid visibility for %s user", visibilitySource, user.getDisplayName());
            respondWithBadRequest(response, "visibilitySource", getString(request, "visibility.invalid"));
            chain.next(request, response);
            return;
        }

        if (isComment && request.getPathInfo().equals("/edge/property")) {
            throw new VisalloException("Use /edge/comment to save comment properties");
        } else if (!isComment && request.getPathInfo().equals("/edge/comment")) {
            throw new VisalloException("Use /edge/property to save non-comment properties");
        }

        OntologyProperty property = ontologyRepository.getPropertyByIRI(propertyName);
        if (property == null) {
            throw new RuntimeException("Could not find property: " + propertyName);
        }

        Object value;
        try {
            value = property.convertString(valueStr);
        } catch (Exception ex) {
            LOGGER.warn(String.format("Validation error propertyName: %s, valueStr: %s", propertyName, valueStr), ex);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
            return;
        }

        Edge edge = graph.getEdge(edgeId, authorizations);

        // add the vertex to the workspace so that the changes show up in the diff panel
        getWorkspaceRepository().updateEntityOnWorkspace(workspaceId, edge.getVertexId(Direction.IN), null, null, user);
        getWorkspaceRepository().updateEntityOnWorkspace(workspaceId, edge.getVertexId(Direction.OUT), null, null, user);

        VisibilityAndElementMutation<Edge> setPropertyResult = graphRepository.setProperty(
                edge,
                propertyName,
                propertyKey,
                value,
                metadata,
                null,
                visibilitySource,
                workspaceId,
                justificationText,
                ClientApiSourceInfo.fromString(sourceInfo),
                user,
                authorizations
        );
        setPropertyResult.elementMutation.save(authorizations);

        this.workQueueRepository.pushGraphPropertyQueue(edge, null, propertyName, workspaceId, visibilitySource, Priority.HIGH);

        respondWithSuccessJson(response);
    }
}
