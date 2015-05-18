package org.visallo.web.routes.edge;

import com.google.inject.Inject;
import com.v5analytics.webster.HandlerChain;
import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloResourceNotFoundException;
import org.visallo.core.model.termMention.TermMentionRepository;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.BaseRequestHandler;
import org.visallo.web.clientapi.model.ClientApiEdgePropertyDetails;
import org.visallo.web.clientapi.model.ClientApiSourceInfo;
import org.vertexium.Authorizations;
import org.vertexium.Edge;
import org.vertexium.Graph;
import org.vertexium.Visibility;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class EdgePropertyDetails extends BaseRequestHandler {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(EdgePropertyDetails.class);
    private final Graph graph;
    private final TermMentionRepository termMentionRepository;

    @Inject
    public EdgePropertyDetails(
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
        String propertyName = getRequiredParameter(request, "propertyName");
        String visibilitySource = getRequiredParameter(request, "visibilitySource");
        String propertyKey = getOptionalParameter(request, "propertyKey");
        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);

        Visibility visibility = new Visibility(visibilitySource);
        if (!graph.isVisibilityValid(visibility, authorizations)) {
            LOGGER.warn("%s is not a valid visibility for %s user", visibilitySource, user.getDisplayName());
            respondWithBadRequest(response, "visibilitySource", getString(request, "visibility.invalid"));
            chain.next(request, response);
            return;
        }

        Edge edge = this.graph.getEdge(edgeId, authorizations);
        if (edge == null) {
            throw new VisalloResourceNotFoundException("Could not find edge with id: " + edgeId, edgeId);
        }

        ClientApiSourceInfo sourceInfo = termMentionRepository.getSourceInfoForEdgeProperty(edge, propertyKey, propertyName, visibility, authorizations);

        ClientApiEdgePropertyDetails result = new ClientApiEdgePropertyDetails();
        result.sourceInfo = sourceInfo;

        respondWithClientApiObject(response, result);
    }
}
