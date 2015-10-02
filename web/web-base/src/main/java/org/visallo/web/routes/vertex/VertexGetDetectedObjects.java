package org.visallo.web.routes.vertex;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Required;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.Property;
import org.vertexium.Vertex;
import org.vertexium.util.IterableUtils;
import org.visallo.core.exception.VisalloResourceNotFoundException;
import org.visallo.core.util.ClientApiConverter;
import org.visallo.web.clientapi.model.ClientApiDetectedObjects;


// This route will no longer be needed once we refactor detected objects.
public class VertexGetDetectedObjects implements ParameterizedHandler {
    private final Graph graph;

    @Inject
    public VertexGetDetectedObjects(
            Graph graph
    ) {
        this.graph = graph;
    }

    @Handle
    public ClientApiDetectedObjects handle(
            @Required(name = "graphVertexId") String graphVertexId,
            @Required(name = "propertyName") String propertyName,
            @Required(name = "workspaceId") String workspaceId,
            Authorizations authorizations
    ) throws Exception {
        Vertex vertex = graph.getVertex(graphVertexId, authorizations);
        if (vertex == null) {
            throw new VisalloResourceNotFoundException(String.format("vertex %s not found", graphVertexId));
        }

        ClientApiDetectedObjects detectedObjects = new ClientApiDetectedObjects();
        Iterable<Property> detectedObjectProperties = vertex.getProperties(propertyName);
        if (detectedObjectProperties == null || IterableUtils.count(detectedObjectProperties) == 0) {
            throw new VisalloResourceNotFoundException(String.format("property %s not found on vertex %s", propertyName, vertex.getId()));
        }
        detectedObjects.addDetectedObjects(ClientApiConverter.toClientApiProperties(detectedObjectProperties, workspaceId));

        return detectedObjects;
    }

}
