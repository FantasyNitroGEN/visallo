package org.visallo.web.routes.vertex;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Required;
import org.vertexium.Authorizations;
import org.vertexium.Edge;
import org.vertexium.Graph;
import org.vertexium.Vertex;
import org.visallo.core.ingest.ArtifactDetectedObject;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.model.workspace.WorkspaceHelper;
import org.visallo.core.user.User;
import org.visallo.core.util.ClientApiConverter;
import org.visallo.core.util.SandboxStatusUtil;
import org.visallo.web.BadRequestException;
import org.visallo.web.clientapi.model.ClientApiElement;
import org.visallo.web.clientapi.model.SandboxStatus;
import org.visallo.web.clientapi.model.VisibilityJson;
import org.visallo.web.parameterProviders.ActiveWorkspaceId;

public class UnresolveDetectedObject implements ParameterizedHandler {
    private final Graph graph;
    private final WorkQueueRepository workQueueRepository;
    private final WorkspaceHelper workspaceHelper;

    @Inject
    public UnresolveDetectedObject(
            final Graph graph,
            WorkQueueRepository workQueueRepository,
            final WorkspaceHelper workspaceHelper
    ) {
        this.graph = graph;
        this.workQueueRepository = workQueueRepository;
        this.workspaceHelper = workspaceHelper;
    }

    @Handle
    public ClientApiElement handle(
            @Required(name = "vertexId") String vertexId,
            @Required(name = "multiValueKey") String multiValueKey,
            @ActiveWorkspaceId String workspaceId,
            User user,
            Authorizations authorizations
    ) throws Exception {
        Vertex artifactVertex = graph.getVertex(vertexId, authorizations);
        ArtifactDetectedObject artifactDetectedObject = VisalloProperties.DETECTED_OBJECT.getPropertyValue(artifactVertex, multiValueKey);
        Edge edge = graph.getEdge(artifactDetectedObject.getEdgeId(), authorizations);
        Vertex resolvedVertex = edge.getOtherVertex(artifactVertex.getId(), authorizations);

        SandboxStatus edgeSandboxStatus = SandboxStatusUtil.getSandboxStatus(edge, workspaceId);
        boolean isPublicEdge = edgeSandboxStatus == SandboxStatus.PUBLIC;

        workspaceHelper.deleteEdge(workspaceId, edge, artifactVertex, resolvedVertex, isPublicEdge, Priority.HIGH, authorizations, user);

        return ClientApiConverter.toClientApi(artifactVertex, workspaceId, authorizations);
    }
}
