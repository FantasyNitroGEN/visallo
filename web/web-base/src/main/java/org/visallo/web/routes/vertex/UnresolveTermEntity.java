package org.visallo.web.routes.vertex;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Required;
import org.vertexium.*;
import org.visallo.core.exception.VisalloResourceNotFoundException;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.termMention.TermMentionRepository;
import org.visallo.core.model.workspace.WorkspaceHelper;
import org.visallo.core.util.SandboxStatusUtil;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.BadRequestException;
import org.visallo.web.VisalloResponse;
import org.visallo.web.clientapi.model.ClientApiSuccess;
import org.visallo.web.clientapi.model.SandboxStatus;
import org.visallo.web.clientapi.model.VisibilityJson;
import org.visallo.web.parameterProviders.ActiveWorkspaceId;

import static org.vertexium.util.IterableUtils.singleOrDefault;

public class UnresolveTermEntity implements ParameterizedHandler {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(UnresolveTermEntity.class);
    private final TermMentionRepository termMentionRepository;
    private final Graph graph;
    private final WorkspaceHelper workspaceHelper;

    @Inject
    public UnresolveTermEntity(
            final TermMentionRepository termMentionRepository,
            final Graph graph,
            final WorkspaceHelper workspaceHelper
    ) {
        this.termMentionRepository = termMentionRepository;
        this.graph = graph;
        this.workspaceHelper = workspaceHelper;
    }

    @Handle
    public ClientApiSuccess handle(
            @Required(name = "termMentionId") String termMentionId,
            @ActiveWorkspaceId String workspaceId,
            Authorizations authorizations
    ) throws Exception {
        LOGGER.debug("UnresolveTermEntity (termMentionId: %s)", termMentionId);

        Vertex termMention = termMentionRepository.findById(termMentionId, authorizations);
        if (termMention == null) {
            throw new VisalloResourceNotFoundException("Could not find term mention with id: " + termMentionId);
        }

        Vertex resolvedVertex = singleOrDefault(termMention.getVertices(Direction.OUT, VisalloProperties.TERM_MENTION_LABEL_RESOLVED_TO, authorizations), null);
        if (resolvedVertex == null) {
            throw new VisalloResourceNotFoundException("Could not find resolved vertex from term mention: " + termMentionId);
        }

        String edgeId = VisalloProperties.TERM_MENTION_RESOLVED_EDGE_ID.getPropertyValue(termMention);
        Edge edge = graph.getEdge(edgeId, authorizations);
        if (edge == null) {
            throw new VisalloResourceNotFoundException("Could not find edge " + edgeId + " from term mention: " + termMentionId);
        }

        SandboxStatus vertexSandboxStatus = SandboxStatusUtil.getSandboxStatus(resolvedVertex, workspaceId);
        SandboxStatus edgeSandboxStatus = SandboxStatusUtil.getSandboxStatus(edge, workspaceId);
        if (vertexSandboxStatus == SandboxStatus.PUBLIC && edgeSandboxStatus == SandboxStatus.PUBLIC) {
            throw new BadRequestException("Can not unresolve a public entity");
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
        return VisalloResponse.SUCCESS;
    }
}
