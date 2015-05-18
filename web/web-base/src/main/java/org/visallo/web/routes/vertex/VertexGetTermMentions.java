package org.visallo.web.routes.vertex;

import com.google.inject.Inject;
import org.visallo.core.config.Configuration;
import org.visallo.core.model.termMention.TermMentionRepository;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.user.User;
import org.visallo.core.util.ClientApiConverter;
import com.v5analytics.webster.HandlerChain;
import org.visallo.web.BaseRequestHandler;
import org.visallo.web.clientapi.model.ClientApiTermMentionsResponse;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.Property;
import org.vertexium.Vertex;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class VertexGetTermMentions extends BaseRequestHandler {
    private final Graph graph;
    private final TermMentionRepository termMentionRepository;

    @Inject
    public VertexGetTermMentions(
            Graph graph,
            UserRepository userRepository,
            Configuration configuration,
            final WorkspaceRepository workspaceRepository,
            TermMentionRepository termMentionRepository) {
        super(userRepository, workspaceRepository, configuration);
        this.graph = graph;
        this.termMentionRepository = termMentionRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        String graphVertexId = getRequiredParameter(request, "graphVertexId");
        String propertyName = getRequiredParameter(request, "propertyName");
        String propertyKey = getRequiredParameter(request, "propertyKey");

        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);
        String workspaceId = getActiveWorkspaceId(request);

        Vertex vertex = graph.getVertex(graphVertexId, authorizations);
        if (vertex == null) {
            respondWithNotFound(response, String.format("vertex %s not found", graphVertexId));
            return;
        }

        Property property = vertex.getProperty(propertyKey, propertyName);
        if (property == null) {
            respondWithNotFound(response, String.format("property %s:%s not found on vertex %s", propertyKey, propertyName, vertex.getId()));
            return;
        }

        Iterable<Vertex> termMentions = termMentionRepository.findBySourceGraphVertexAndPropertyKey(graphVertexId, propertyKey, authorizations);
        ClientApiTermMentionsResponse termMentionsResponse = ClientApiConverter.toTermMentionsResponse(termMentions, workspaceId, authorizations);
        respondWithClientApiObject(response, termMentionsResponse);
    }
}
