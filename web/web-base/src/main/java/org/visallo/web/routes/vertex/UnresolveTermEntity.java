package org.visallo.web.routes.vertex;

import com.google.inject.Inject;
import com.v5analytics.webster.HandlerChain;
import org.vertexium.*;
import org.visallo.core.config.Configuration;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.termMention.TermMentionRepository;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.user.User;
import org.visallo.core.util.SandboxStatusUtil;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.BaseRequestHandler;
import org.visallo.web.clientapi.model.SandboxStatus;
import org.visallo.web.clientapi.model.VisibilityJson;
import org.visallo.web.routes.workspace.WorkspaceHelper;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.vertexium.util.IterableUtils.singleOrDefault;

public class UnresolveTermEntity extends BaseRequestHandler {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(UnresolveTermEntity.class);
    private final TermMentionRepository termMentionRepository;
    private final Graph graph;
    private final WorkspaceHelper workspaceHelper;

    @Inject
    public UnresolveTermEntity(
            final TermMentionRepository termMentionRepository,
            final Graph graph,
            final UserRepository userRepository,
            final WorkspaceRepository workspaceRepository,
            final Configuration configuration,
            final WorkspaceHelper workspaceHelper
    ) {
        super(userRepository, workspaceRepository, configuration);
        this.termMentionRepository = termMentionRepository;
        this.graph = graph;
        this.workspaceHelper = workspaceHelper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final String termMentionId = getRequiredParameter(request, "termMentionId");

        LOGGER.debug("UnresolveTermEntity (termMentionId: %s)", termMentionId);

        String workspaceId = getActiveWorkspaceId(request);
        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);

        Vertex termMention = termMentionRepository.findById(termMentionId, authorizations);
        if (termMention == null) {
            respondWithNotFound(response, "Could not find term mention with id: " + termMentionId);
            return;
        }

        Vertex resolvedVertex = singleOrDefault(termMention.getVertices(Direction.OUT, VisalloProperties.TERM_MENTION_LABEL_RESOLVED_TO, authorizations), null);
        if (resolvedVertex == null) {
            respondWithNotFound(response, "Could not find resolved vertex from term mention: " + termMentionId);
            return;
        }

        String edgeId = VisalloProperties.TERM_MENTION_RESOLVED_EDGE_ID.getPropertyValue(termMention);
        Edge edge = graph.getEdge(edgeId, authorizations);
        if (edge == null) {
            respondWithNotFound(response, "Could not find edge " + edgeId + " from term mention: " + termMentionId);
            return;
        }

        SandboxStatus vertexSandboxStatus = SandboxStatusUtil.getSandboxStatus(resolvedVertex, workspaceId);
        SandboxStatus edgeSandboxStatus = SandboxStatusUtil.getSandboxStatus(edge, workspaceId);
        if (vertexSandboxStatus == SandboxStatus.PUBLIC && edgeSandboxStatus == SandboxStatus.PUBLIC) {
            LOGGER.warn("Can not unresolve a public entity");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            chain.next(request, response);
            return;
        }

        VisibilityJson visibilityJson;
        if (vertexSandboxStatus == SandboxStatus.PUBLIC) {
            visibilityJson = VisalloProperties.VISIBILITY_JSON.getPropertyValue(edge);
            VisibilityJson.removeFromWorkspace(visibilityJson, workspaceId);
        } else {
            visibilityJson = VisalloProperties.VISIBILITY_JSON.getPropertyValue(resolvedVertex);
            VisibilityJson.removeFromWorkspace(visibilityJson, workspaceId);
        }

        workspaceHelper.unresolveTerm(termMention, authorizations);
        respondWithSuccessJson(response);
    }
}
