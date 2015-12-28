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

    @Inject
    public UnresolveDetectedObject(
            final Graph graph,
            WorkQueueRepository workQueueRepository
    ) {
        this.graph = graph;
        this.workQueueRepository = workQueueRepository;
    }

    @Handle
    public ClientApiElement handle(
            @Required(name = "vertexId") String vertexId,
            @Required(name = "multiValueKey") String multiValueKey,
            @ActiveWorkspaceId String workspaceId,
            Authorizations authorizations
    ) throws Exception {
        Vertex artifactVertex = graph.getVertex(vertexId, authorizations);
        ArtifactDetectedObject artifactDetectedObject = VisalloProperties.DETECTED_OBJECT.getPropertyValue(artifactVertex, multiValueKey);
        Edge edge = graph.getEdge(artifactDetectedObject.getEdgeId(), authorizations);
        Vertex resolvedVertex = edge.getOtherVertex(artifactVertex.getId(), authorizations);

        SandboxStatus vertexSandboxStatus = SandboxStatusUtil.getSandboxStatus(resolvedVertex, workspaceId);
        SandboxStatus edgeSandboxStatus = SandboxStatusUtil.getSandboxStatus(edge, workspaceId);
        if (vertexSandboxStatus == SandboxStatus.PUBLIC && edgeSandboxStatus == SandboxStatus.PUBLIC) {
            throw new BadRequestException("Can not unresolve a public entity");
        }

        VisibilityJson visibilityJson;
        if (vertexSandboxStatus == SandboxStatus.PUBLIC) {
            visibilityJson = VisalloProperties.VISIBILITY_JSON.getPropertyValue(edge);
            visibilityJson = VisibilityJson.removeFromWorkspace(visibilityJson, workspaceId);
        } else {
            visibilityJson = VisalloProperties.VISIBILITY_JSON.getPropertyValue(resolvedVertex);
            visibilityJson = VisibilityJson.removeFromWorkspace(visibilityJson, workspaceId);
        }

        long timestamp = System.currentTimeMillis();
        // remove edge
        graph.softDeleteEdge(edge, authorizations);

        // remove property
        VisalloProperties.DETECTED_OBJECT.removeProperty(artifactVertex, multiValueKey, authorizations);

        graph.flush();

        this.workQueueRepository.pushEdgeDeletion(edge, timestamp, Priority.HIGH);
        this.workQueueRepository.pushGraphPropertyQueue(
                artifactVertex,
                multiValueKey,
                VisalloProperties.DETECTED_OBJECT.getPropertyName(),
                workspaceId,
                visibilityJson.getSource(),
                Priority.HIGH
        );

        return ClientApiConverter.toClientApi(artifactVertex, workspaceId, authorizations);
    }
}
