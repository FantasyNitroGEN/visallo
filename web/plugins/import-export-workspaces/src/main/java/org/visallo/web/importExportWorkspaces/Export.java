package org.visallo.web.importExportWorkspaces;

import com.v5analytics.webster.HandlerChain;
import org.visallo.core.config.Configuration;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workspace.Workspace;
import org.visallo.core.model.workspace.WorkspaceEntity;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.user.User;
import org.visallo.vertexium.model.workspace.VertexiumWorkspaceRepository;
import org.visallo.web.BaseRequestHandler;
import org.vertexium.Authorizations;
import org.vertexium.Edge;
import org.vertexium.Graph;
import org.vertexium.Vertex;
import org.vertexium.tools.GraphBackup;
import org.vertexium.util.ConvertingIterable;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import static org.vertexium.util.IterableUtils.toList;

public class Export extends BaseRequestHandler {
    private final VertexiumWorkspaceRepository workspaceRepository;
    private final Graph graph;

    @Inject
    public Export(
            UserRepository userRepository,
            Configuration configuration,
            WorkspaceRepository workspaceRepository,
            Graph graph) {
        super(userRepository, workspaceRepository, configuration);
        this.workspaceRepository = (VertexiumWorkspaceRepository) workspaceRepository;
        this.graph = graph;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        String workspaceId = getRequiredParameter(request, "workspaceId");

        User user = getUser(request);
        Workspace workspace = this.workspaceRepository.findById(workspaceId, user);
        if (workspace == null) {
            respondWithNotFound(response);
            return;
        }

        Authorizations authorizations = getUserRepository().getAuthorizations(user, UserRepository.VISIBILITY_STRING, WorkspaceRepository.VISIBILITY_STRING, workspace.getWorkspaceId());

        List<String> workspaceEntityIds = toList(getWorkspaceEntityIds(user, workspace));

        // create this array so that we get the relationships from workspace to entities
        ArrayList<String> workspaceEntityIdsAndWorkspaceId = new ArrayList<String>(workspaceEntityIds);
        workspaceEntityIdsAndWorkspaceId.add(workspace.getWorkspaceId());

        Vertex workspaceVertex = this.workspaceRepository.getVertex(workspace.getWorkspaceId(), user);
        Iterable<Vertex> vertices = graph.getVertices(workspaceEntityIds, authorizations);
        Iterable<Edge> edges = graph.getEdges(graph.findRelatedEdges(workspaceEntityIdsAndWorkspaceId, authorizations), authorizations);

        response.addHeader("Content-Disposition", "attachment; filename=" + workspace.getDisplayTitle() + ".visalloWorkspace");

        OutputStream out = response.getOutputStream();
        try {
            GraphBackup graphBackup = new GraphBackup();
            graphBackup.saveVertex(workspaceVertex, out);
            graphBackup.save(vertices, edges, out);
        } finally {
            out.close();
        }
    }

    private Iterable<String> getWorkspaceEntityIds(final User user, final Workspace workspace) {
        return new ConvertingIterable<WorkspaceEntity, String>(this.workspaceRepository.findEntities(workspace, user)) {
            @Override
            protected String convert(WorkspaceEntity o) {
                return o.getEntityVertexId();
            }
        };
    }
}
