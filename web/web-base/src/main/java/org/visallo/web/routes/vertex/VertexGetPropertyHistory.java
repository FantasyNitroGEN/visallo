package org.visallo.web.routes.vertex;

import com.google.inject.Inject;
import com.v5analytics.webster.HandlerChain;
import org.visallo.core.config.Configuration;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.user.User;
import org.visallo.core.util.ClientApiConverter;
import org.visallo.web.BaseRequestHandler;
import org.visallo.web.clientapi.model.ClientApiHistoricalPropertyValues;
import org.vertexium.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class VertexGetPropertyHistory extends BaseRequestHandler {
    private Graph graph;

    @Inject
    public VertexGetPropertyHistory(
            final Graph graph,
            final UserRepository userRepository,
            final WorkspaceRepository workspaceRepository,
            final Configuration configuration) {
        super(userRepository, workspaceRepository, configuration);
        this.graph = graph;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        String graphVertexId = getRequiredParameter(request, "graphVertexId");
        String propertyName = getRequiredParameter(request, "propertyName");
        String propertyKey = getRequiredParameter(request, "propertyKey");
        Long startTime = getOptionalParameterLong(request, "startTime", null);
        Long endTime = getOptionalParameterLong(request, "endTime", null);
        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);

        ClientApiHistoricalPropertyValues historicalPropertyValues = handle(response, graphVertexId, propertyName, propertyKey, startTime, endTime, authorizations);
        respondWithClientApiObject(response, historicalPropertyValues);
    }

    private ClientApiHistoricalPropertyValues handle(HttpServletResponse response, String graphVertexId, String propertyName, String propertyKey, Long startTime, Long endTime, Authorizations authorizations) throws IOException {
        Vertex vertex = graph.getVertex(graphVertexId, authorizations);
        if (vertex == null) {
            respondWithNotFound(response, String.format("vertex %s not found", graphVertexId));
            return null;
        }

        Property property = vertex.getProperty(propertyKey, propertyName);
        if (property == null) {
            respondWithNotFound(response, String.format("property %s:%s not found on vertex %s", propertyKey, propertyName, vertex.getId()));
            return null;
        }

        Iterable<HistoricalPropertyValue> historicalPropertyValues = vertex.getHistoricalPropertyValues(
                property.getKey(),
                property.getName(),
                property.getVisibility(),
                startTime,
                endTime,
                authorizations
        );
        return ClientApiConverter.toClientApi(historicalPropertyValues);
    }
}
