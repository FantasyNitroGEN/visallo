package org.visallo.web.routes.vertex;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Optional;
import com.v5analytics.webster.annotations.Required;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.Vertex;
import org.visallo.core.model.longRunningProcess.FindPathLongRunningProcessQueueItem;
import org.visallo.core.model.longRunningProcess.LongRunningProcessRepository;
import org.visallo.core.user.User;
import org.visallo.web.VisalloResponse;
import org.visallo.web.clientapi.model.ClientApiLongRunningProcessSubmitResponse;
import org.visallo.web.parameterProviders.ActiveWorkspaceId;

public class VertexFindPath implements ParameterizedHandler {
    private final Graph graph;
    private final LongRunningProcessRepository longRunningProcessRepository;

    @Inject
    public VertexFindPath(
            final Graph graph,
            final LongRunningProcessRepository longRunningProcessRepository
    ) {
        this.graph = graph;
        this.longRunningProcessRepository = longRunningProcessRepository;
    }

    @Handle
    public void handle(
            User user,
            @ActiveWorkspaceId String workspaceId,
            @Required(name = "outVertexId") String outVertexId,
            @Required(name = "inVertexId") String inVertexId,
            @Required(name = "hops") int hops,
            @Optional(name = "labels[]") String[] labels,
            Authorizations authorizations,
            VisalloResponse response
    ) throws Exception {
        Vertex outVertex = graph.getVertex(outVertexId, authorizations);
        if (outVertex == null) {
            response.respondWithNotFound("Source vertex not found");
            return;
        }

        Vertex inVertex = graph.getVertex(inVertexId, authorizations);
        if (inVertex == null) {
            response.respondWithNotFound("Destination vertex not found");
            return;
        }

        FindPathLongRunningProcessQueueItem findPathQueueItem = new FindPathLongRunningProcessQueueItem(outVertex.getId(), inVertex.getId(), labels, hops, workspaceId, authorizations);
        String id = this.longRunningProcessRepository.enqueue(findPathQueueItem.toJson(), user, authorizations);

        response.respondWithClientApiObject(new ClientApiLongRunningProcessSubmitResponse(id));
    }
}

