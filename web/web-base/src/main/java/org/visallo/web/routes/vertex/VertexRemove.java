package org.visallo.web.routes.vertex;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Required;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.Vertex;
import org.visallo.core.exception.VisalloAccessDeniedException;
import org.visallo.core.exception.VisalloResourceNotFoundException;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.model.workspace.WorkspaceHelper;
import org.visallo.core.security.ACLProvider;
import org.visallo.core.user.User;
import org.visallo.core.util.SandboxStatusUtil;
import org.visallo.web.VisalloResponse;
import org.visallo.web.clientapi.model.ClientApiSuccess;
import org.visallo.web.clientapi.model.SandboxStatus;
import org.visallo.web.parameterProviders.ActiveWorkspaceId;

public class VertexRemove implements ParameterizedHandler {
    private final Graph graph;
    private final WorkspaceHelper workspaceHelper;
    private final ACLProvider aclProvider;
    private final WorkQueueRepository workQueueRepository;

    @Inject
    public VertexRemove(
            final Graph graph,
            final WorkspaceHelper workspaceHelper,
            final ACLProvider aclProvider,
            final WorkQueueRepository workQueueRepository
    ) {
        this.graph = graph;
        this.workspaceHelper = workspaceHelper;
        this.aclProvider = aclProvider;
        this.workQueueRepository = workQueueRepository;
    }

    @Handle
    public ClientApiSuccess handle(
            @Required(name = "graphVertexId") String graphVertexId,
            @ActiveWorkspaceId String workspaceId,
            User user,
            Authorizations authorizations
    ) throws Exception {
        Vertex vertex = graph.getVertex(graphVertexId, authorizations);
        if (vertex == null) {
            throw new VisalloResourceNotFoundException("Could not find vertex with id: " + graphVertexId);
        }

        if (!aclProvider.canDeleteElement(vertex, user, workspaceId)) {
            throw new VisalloAccessDeniedException("Vertex " + graphVertexId + " is not deleteable", user,
                    graphVertexId);
        }

        SandboxStatus sandboxStatus = SandboxStatusUtil.getSandboxStatus(vertex, workspaceId);

        boolean isPublicVertex = sandboxStatus == SandboxStatus.PUBLIC;

        workspaceHelper.deleteVertex(vertex, workspaceId, isPublicVertex, Priority.HIGH, authorizations, user);
        return VisalloResponse.SUCCESS;
    }
}
