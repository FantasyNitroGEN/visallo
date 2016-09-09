package org.visallo.web.routes.vertex;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Required;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.Vertex;
import org.visallo.core.exception.VisalloResourceNotFoundException;
import org.visallo.core.model.graph.GraphRepository;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.model.workspace.Workspace;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.security.VisibilityTranslator;
import org.visallo.core.user.User;
import org.visallo.core.util.ClientApiConverter;
import org.visallo.core.util.SandboxStatusUtil;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.clientapi.model.ClientApiElement;
import org.visallo.web.clientapi.model.ClientApiWorkspace;
import org.visallo.web.parameterProviders.ActiveWorkspaceId;
import org.visallo.web.parameterProviders.SourceGuid;
import org.visallo.web.util.VisibilityValidator;

import java.util.ResourceBundle;

public class VertexSetVisibility implements ParameterizedHandler {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(VertexSetVisibility.class);
    private final Graph graph;
    private final WorkspaceRepository workspaceRepository;
    private final WorkQueueRepository workQueueRepository;
    private final GraphRepository graphRepository;
    private final VisibilityTranslator visibilityTranslator;

    @Inject
    public VertexSetVisibility(
            Graph graph,
            WorkspaceRepository workspaceRepository,
            WorkQueueRepository workQueueRepository,
            GraphRepository graphRepository,
            VisibilityTranslator visibilityTranslator
    ) {
        this.graph = graph;
        this.workspaceRepository = workspaceRepository;
        this.workQueueRepository = workQueueRepository;
        this.graphRepository = graphRepository;
        this.visibilityTranslator = visibilityTranslator;
    }

    @Handle
    public ClientApiElement handle(
            @Required(name = "graphVertexId") String graphVertexId,
            @Required(name = "visibilitySource") String visibilitySource,
            @ActiveWorkspaceId String workspaceId,
            @SourceGuid String sourceGuid,
            ResourceBundle resourceBundle,
            User user,
            Authorizations authorizations
    ) throws Exception {
        VisibilityValidator.validate(graph, visibilityTranslator, resourceBundle, visibilitySource, user, authorizations);

        Vertex graphVertex = graph.getVertex(graphVertexId, authorizations);
        if (graphVertex == null) {
            throw new VisalloResourceNotFoundException("Could not find vertex: " + graphVertexId);
        }

        // add the vertex to the workspace so that the changes show up in the diff panel
        workspaceRepository.updateEntityOnWorkspace(workspaceId, graphVertexId, null, null, user);

        LOGGER.info("changing vertex (%s) visibility source to %s", graphVertex.getId(), visibilitySource);

        graphRepository.updateElementVisibilitySource(
                graphVertex,
                SandboxStatusUtil.getSandboxStatus(graphVertex, workspaceId),
                visibilitySource,
                workspaceId,
                authorizations
        );

        this.graph.flush();

        this.workQueueRepository.pushGraphPropertyQueue(
                graphVertex,
                null,
                VisalloProperties.VISIBILITY_JSON.getPropertyName(),
                workspaceId,
                visibilitySource,
                Priority.HIGH
        );

        Workspace workspace = workspaceRepository.findById(workspaceId, user);
        ClientApiWorkspace clientApiWorkspace = workspaceRepository.toClientApi(workspace, user, true, authorizations);
        workQueueRepository.pushWorkspaceChange(clientApiWorkspace, clientApiWorkspace.getUsers(), user.getUserId(), sourceGuid);

        return ClientApiConverter.toClientApi(graphVertex, workspaceId, authorizations);
    }
}
