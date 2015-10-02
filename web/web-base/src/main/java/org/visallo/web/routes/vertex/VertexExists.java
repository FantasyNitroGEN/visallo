package org.visallo.web.routes.vertex;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Required;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.visallo.web.VisalloResponse;
import org.visallo.web.clientapi.model.ClientApiVerticesExistsResponse;

import java.util.Map;

public class VertexExists implements ParameterizedHandler {
    private final Graph graph;

    @Inject
    public VertexExists(final Graph graph) {
        this.graph = graph;
    }

    @Handle
    public ClientApiVerticesExistsResponse handle(
            @Required(name = "vertexIds[]") String[] vertexIds,
            VisalloResponse response,
            Authorizations authorizations
    ) throws Exception {
        Map<String, Boolean> graphVertices = graph.doVerticesExist(Lists.newArrayList(vertexIds), authorizations);
        ClientApiVerticesExistsResponse result = new ClientApiVerticesExistsResponse();
        result.getExists().putAll(graphVertices);
        return result;
    }
}
