package org.visallo.web.routes.edge;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Optional;
import com.v5analytics.webster.annotations.Required;
import org.vertexium.*;
import org.vertexium.util.IterableUtils;
import org.visallo.core.exception.VisalloAccessDeniedException;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.graph.GraphRepository;
import org.visallo.core.model.graph.VisibilityAndElementMutation;
import org.visallo.core.model.ontology.OntologyProperty;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.security.ACLProvider;
import org.visallo.core.security.VisibilityTranslator;
import org.visallo.core.user.User;
import org.visallo.core.util.VertexiumMetadataUtil;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.BadRequestException;
import org.visallo.web.VisalloResponse;
import org.visallo.web.clientapi.model.ClientApiSourceInfo;
import org.visallo.web.clientapi.model.ClientApiSuccess;
import org.visallo.web.parameterProviders.ActiveWorkspaceId;
import org.visallo.web.parameterProviders.JustificationText;

import javax.servlet.http.HttpServletRequest;
import java.util.ResourceBundle;

public class SetEdgeProperty implements ParameterizedHandler {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(SetEdgeProperty.class);

    private final Graph graph;
    private final OntologyRepository ontologyRepository;
    private VisibilityTranslator visibilityTranslator;
    private final WorkQueueRepository workQueueRepository;
    private final WorkspaceRepository workspaceRepository;
    private final GraphRepository graphRepository;
    private final ACLProvider aclProvider;

    @Inject
    public SetEdgeProperty(
            final OntologyRepository ontologyRepository,
            final Graph graph,
            final VisibilityTranslator visibilityTranslator,
            final WorkQueueRepository workQueueRepository,
            final WorkspaceRepository workspaceRepository,
            final GraphRepository graphRepository,
            final ACLProvider aclProvider
    ) {
        this.ontologyRepository = ontologyRepository;
        this.graph = graph;
        this.visibilityTranslator = visibilityTranslator;
        this.workQueueRepository = workQueueRepository;
        this.workspaceRepository = workspaceRepository;
        this.graphRepository = graphRepository;
        this.aclProvider = aclProvider;
    }

    @Handle
    public ClientApiSuccess handle(
            HttpServletRequest request,
            @Required(name = "edgeId") String edgeId,
            @Optional(name = "propertyKey") String propertyKey,
            @Required(name = "propertyName") String propertyName,
            @Required(name = "value") String valueStr,
            @Required(name = "visibilitySource") String visibilitySource,
            @Optional(name = "sourceInfo") String sourceInfo,
            @Optional(name = "metadata") String metadataString,
            @JustificationText String justificationText,
            @ActiveWorkspaceId String workspaceId,
            ResourceBundle resourceBundle,
            User user,
            Authorizations authorizations
    ) throws Exception {
        boolean isComment = VisalloProperties.COMMENT.getPropertyName().equals(propertyName);

        if (propertyKey == null) {
            propertyKey = this.graph.getIdGenerator().nextId();
        }

        Metadata metadata = VertexiumMetadataUtil.metadataStringToMap(metadataString, visibilityTranslator.getDefaultVisibility());

        if (!graph.isVisibilityValid(new Visibility(visibilitySource), authorizations)) {
            LOGGER.warn("%s is not a valid visibility for %s user", visibilitySource, user.getDisplayName());
            throw new BadRequestException("visibilitySource", resourceBundle.getString("visibility.invalid"));
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

        Edge edge = graph.getEdge(edgeId, authorizations);

        // TODO: add and update property both come through here. Currently, we're only enforcing update.
        if (!isComment) {
            int propCount = IterableUtils.count(edge.getProperties(propertyKey, propertyName));
            if (!aclProvider.canUpdateElement(edge, user) ||
                    (propCount > 0 && !aclProvider.canUpdateProperty(edge, propertyKey, propertyName, user))) {
                throw new VisalloAccessDeniedException(propertyName + " is not updateable", user, workspaceId);
            }
        }

        Object value;
        try {
            value = property.convertString(valueStr);
        } catch (Exception ex) {
            LOGGER.warn(String.format("Validation error propertyName: %s, valueStr: %s", propertyName, valueStr), ex);
            throw new BadRequestException(ex.getMessage());
        }


        // add the vertex to the workspace so that the changes show up in the diff panel
        workspaceRepository.updateEntityOnWorkspace(workspaceId, edge.getVertexId(Direction.IN), null, null, user);
        workspaceRepository.updateEntityOnWorkspace(workspaceId, edge.getVertexId(Direction.OUT), null, null, user);

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

        return VisalloResponse.SUCCESS;
    }
}
