package org.visallo.web.routes.edge;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.visallo.web.clientapi.model.ClientApiVertexCount;

public class EdgeGetCount implements ParameterizedHandler {
    private final Graph graph;

    @Inject
    public EdgeGetCount(Graph graph) {
        this.graph = graph;
    }

    @Handle
    public ClientApiVertexCount handle(Authorizations authorizations) throws Exception {
        long vertexCount = graph.getEdgeCount(authorizations);
        return new ClientApiVertexCount(vertexCount);
    }
}
