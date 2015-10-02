package org.visallo.web.importExportWorkspaces;

import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Required;
import org.vertexium.Authorizations;
import org.vertexium.Edge;
import org.vertexium.Graph;
import org.vertexium.Vertex;
import org.vertexium.tools.GraphBackup;
import org.vertexium.util.ConvertingIterable;
import org.visallo.core.exception.VisalloResourceNotFoundException;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workspace.Workspace;
import org.visallo.core.model.workspace.WorkspaceEntity;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.user.User;
import org.visallo.vertexium.model.workspace.VertexiumWorkspaceRepository;
import org.visallo.web.VisalloResponse;

import javax.inject.Inject;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import static org.vertexium.util.IterableUtils.toList;

public class Export implements ParameterizedHandler {
    private final UserRepository userRepository;
    private final VertexiumWorkspaceRepository workspaceRepository;
    private final Graph graph;

    @Inject
    public Export(
            UserRepository userRepository,
            WorkspaceRepository workspaceRepository,
            Graph graph) {
        this.userRepository = userRepository;
        this.workspaceRepository =  (VertexiumWorkspaceRepository) workspaceRepository;
        this.graph = graph;
    }

    @Handle
    public void handle(
            User user,
            @Required(name = "workspaceId") String workspaceId,
            VisalloResponse response
    ) throws Exception {
        Workspace workspace = this.workspaceRepository.findById(workspaceId, user);
        if (workspace == null) {
            throw new VisalloResourceNotFoundException("workspace not found");
        }

        Authorizations authorizations = userRepository.getAuthorizations(user, UserRepository.VISIBILITY_STRING, WorkspaceRepository.VISIBILITY_STRING, workspace.getWorkspaceId());

        List<String> workspaceEntityIds = toList(getWorkspaceEntityIds(user, workspace));

        // create this array so that we get the relationships from workspace to entities
        ArrayList<String> workspaceEntityIdsAndWorkspaceId = new ArrayList<>(workspaceEntityIds);
        workspaceEntityIdsAndWorkspaceId.add(workspace.getWorkspaceId());

        Vertex workspaceVertex = workspaceRepository.getVertex(workspace.getWorkspaceId(), user);
        Iterable<Vertex> vertices = graph.getVertices(workspaceEntityIds, authorizations);
        Iterable<Edge> edges = graph.getEdges(graph.findRelatedEdgeIds(workspaceEntityIdsAndWorkspaceId, authorizations), authorizations);

        response.addHeader("Content-Disposition", "attachment; filename=" + workspace.getDisplayTitle() + ".visalloWorkspace");

        try (OutputStream out = response.getOutputStream()) {
            GraphBackup graphBackup = new GraphBackup();
            graphBackup.saveVertex(workspaceVertex, out);
            graphBackup.save(vertices, edges, out);
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
