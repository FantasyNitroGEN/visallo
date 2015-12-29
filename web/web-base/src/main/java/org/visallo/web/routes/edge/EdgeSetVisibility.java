package org.visallo.web.routes.edge;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Required;
import org.vertexium.Authorizations;
import org.vertexium.Direction;
import org.vertexium.Edge;
import org.vertexium.Graph;
import org.visallo.core.exception.VisalloResourceNotFoundException;
import org.visallo.core.model.graph.GraphRepository;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.security.VisibilityTranslator;
import org.visallo.core.user.User;
import org.visallo.core.util.ClientApiConverter;
import org.visallo.core.util.SandboxStatusUtil;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.BadRequestException;
import org.visallo.web.clientapi.model.ClientApiElement;
import org.visallo.web.parameterProviders.ActiveWorkspaceId;

import java.util.ResourceBundle;

public class EdgeSetVisibility implements ParameterizedHandler {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(EdgeSetVisibility.class);
    private final Graph graph;
    private final WorkQueueRepository workQueueRepository;
    private final WorkspaceRepository workspaceRepository;
    private final GraphRepository graphRepository;
    private final VisibilityTranslator visibilityTranslator;

    @Inject
    public EdgeSetVisibility(
            Graph graph,
            WorkQueueRepository workQueueRepository,
            WorkspaceRepository workspaceRepository,
            GraphRepository graphRepository,
            VisibilityTranslator visibilityTranslator
    ) {
        this.graph = graph;
        this.workQueueRepository = workQueueRepository;
        this.workspaceRepository = workspaceRepository;
        this.graphRepository = graphRepository;
        this.visibilityTranslator = visibilityTranslator;
    }

    @Handle
    public ClientApiElement handle(
            @Required(name = "graphEdgeId") String graphEdgeId,
            @Required(name = "visibilitySource") String visibilitySource,
            @ActiveWorkspaceId String workspaceId,
            ResourceBundle resourceBundle,
            User user,
            Authorizations authorizations
    ) throws Exception {
        Edge graphEdge = graph.getEdge(graphEdgeId, authorizations);
        if (graphEdge == null) {
            throw new VisalloResourceNotFoundException("Could not find edge: " + graphEdgeId);
        }

        if (!graph.isVisibilityValid(visibilityTranslator.toVisibility(visibilitySource).getVisibility(), authorizations)) {
            LOGGER.warn("%s is not a valid visibility for %s user", visibilitySource, user.getDisplayName());
            throw new BadRequestException("visibilitySource", resourceBundle.getString("visibility.invalid"));
        }

        // add the vertex to the workspace so that the changes show up in the diff panel
        workspaceRepository.updateEntityOnWorkspace(workspaceId, graphEdge.getVertexId(Direction.IN), null, null, user);
        workspaceRepository.updateEntityOnWorkspace(workspaceId, graphEdge.getVertexId(Direction.OUT), null, null, user);

        LOGGER.info("changing edge (%s) visibility source to %s", graphEdge.getId(), visibilitySource);

        graphRepository.updateElementVisibilitySource(
                graphEdge,
                SandboxStatusUtil.getSandboxStatus(graphEdge, workspaceId),
                visibilitySource,
                workspaceId,
                authorizations
        );
        this.graph.flush();

        this.workQueueRepository.pushGraphPropertyQueue(
                graphEdge,
                null,
                VisalloProperties.VISIBILITY_JSON.getPropertyName(),
                workspaceId,
                visibilitySource,
                Priority.HIGH
        );

        return ClientApiConverter.toClientApi(graphEdge, workspaceId, authorizations);
    }
}
