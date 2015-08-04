package org.visallo.web.routes.workspace;

import com.google.inject.Inject;
import com.v5analytics.webster.HandlerChain;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.RelatedEdge;
import org.visallo.core.config.Configuration;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workspace.Workspace;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.BaseRequestHandler;
import org.visallo.web.clientapi.model.ClientApiWorkspaceEdges;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.List;

public class WorkspaceEdges extends BaseRequestHandler {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(WorkspaceEdges.class);
    private final Graph graph;
    private final WorkspaceRepository workspaceRepository;

    @Inject
    public WorkspaceEdges(
            final Graph graph,
            final UserRepository userRepository,
            final Configuration configuration,
            final WorkspaceRepository workspaceRepository) {
        super(userRepository, workspaceRepository, configuration);
        this.graph = graph;
        this.workspaceRepository = workspaceRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        String[] additionalVertexIds = getOptionalParameterAsStringArray(request, "ids[]"); // additional graph vertex ids to search for
        if (additionalVertexIds == null) {
            additionalVertexIds = new String[0];
        }

        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);
        String workspaceId = getActiveWorkspaceId(request);

        long startTime = System.nanoTime();

        Workspace workspace = workspaceRepository.findById(workspaceId, user);
        List<String> vertexIds = workspaceRepository.findEntityVertexIds(workspace, user);
        Collections.addAll(vertexIds, additionalVertexIds);

        ClientApiWorkspaceEdges results = getEdges(request, workspaceId, vertexIds, authorizations);

        long endTime = System.nanoTime();
        LOGGER.debug("Retrieved in %dms", (endTime - startTime) / 1000 / 1000);

        respondWithClientApiObject(response, results);
    }

    /**
     * This is overridable so web plugins can modify the resulting set of edges.
     */
    protected ClientApiWorkspaceEdges getEdges(
            HttpServletRequest request,
            String workspaceId,
            Iterable<String> vertexIds,
            Authorizations authorizations
    ) {
        ClientApiWorkspaceEdges edgeResult = new ClientApiWorkspaceEdges();
        Iterable<RelatedEdge> relatedEdges = graph.findRelatedEdgeSummary(vertexIds, authorizations);
        for (RelatedEdge relatedEdge : relatedEdges) {
            edgeResult.add(relatedEdge.getEdgeId(), relatedEdge.getLabel(), relatedEdge.getOutVertexId(), relatedEdge.getInVertexId());
        }
        return edgeResult;
    }
}
