package org.visallo.web.routes.edge;

import com.google.inject.Inject;
import com.v5analytics.webster.HandlerChain;
import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloResourceNotFoundException;
import org.visallo.core.model.termMention.TermMentionRepository;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.user.User;
import org.visallo.web.BaseRequestHandler;
import org.visallo.web.clientapi.model.ClientApiEdgeDetails;
import org.visallo.web.clientapi.model.ClientApiSourceInfo;
import org.vertexium.Authorizations;
import org.vertexium.Edge;
import org.vertexium.Graph;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class EdgeDetails extends BaseRequestHandler {
    private final Graph graph;
    private final TermMentionRepository termMentionRepository;

    @Inject
    public EdgeDetails(
            UserRepository userRepository,
            WorkspaceRepository workspaceRepository,
            Configuration configuration,
            Graph graph,
            TermMentionRepository termMentionRepository
    ) {
        super(userRepository, workspaceRepository, configuration);
        this.graph = graph;
        this.termMentionRepository = termMentionRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        String edgeId = getRequiredParameter(request, "edgeId");
        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);

        Edge edge = this.graph.getEdge(edgeId, authorizations);
        if (edge == null) {
            throw new VisalloResourceNotFoundException("Could not find edge with id: " + edgeId, edgeId);
        }

        ClientApiSourceInfo sourceInfo = termMentionRepository.getSourceInfoForEdge(edge, authorizations);

        ClientApiEdgeDetails result = new ClientApiEdgeDetails();
        result.sourceInfo = sourceInfo;

        respondWithClientApiObject(response, result);
    }
}
