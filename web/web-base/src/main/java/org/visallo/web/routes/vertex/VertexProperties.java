package org.visallo.web.routes.vertex;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Required;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.Vertex;
import org.visallo.core.exception.VisalloResourceNotFoundException;
import org.visallo.core.util.ClientApiConverter;
import org.visallo.web.clientapi.model.ClientApiElement;
import org.visallo.web.parameterProviders.ActiveWorkspaceId;

public class VertexProperties implements ParameterizedHandler {
    private final Graph graph;

    @Inject
    public VertexProperties(final Graph graph) {
        this.graph = graph;
    }

    @Handle
    public ClientApiElement handle(
            @Required(name = "graphVertexId") String graphVertexId,
            @ActiveWorkspaceId String workspaceId,
            Authorizations authorizations
    ) throws Exception {
        Vertex vertex = graph.getVertex(graphVertexId, authorizations);
        if (vertex == null) {
            throw new VisalloResourceNotFoundException("Could not find vertex: " + graphVertexId);
        }
        return ClientApiConverter.toClientApi(vertex, workspaceId, authorizations);
    }
}
