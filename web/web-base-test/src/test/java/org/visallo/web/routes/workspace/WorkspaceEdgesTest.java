package org.visallo.web.routes.workspace;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.vertexium.Authorizations;
import org.vertexium.Vertex;
import org.vertexium.Visibility;
import org.visallo.core.model.workspace.WorkspaceEntity;
import org.visallo.vertexium.model.workspace.VertexiumWorkspace;
import org.visallo.web.routes.RouteTestBase;
import org.visallo.web.clientapi.model.ClientApiWorkspaceEdges;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class WorkspaceEdgesTest extends RouteTestBase {
    private WorkspaceEdges workspaceEdges;
    private VertexiumWorkspace workspace;
    private Vertex workspaceVertex;
    private Authorizations authorizations;

    @Before
    public void before() throws IOException {
        super.before();
        workspaceEdges = new WorkspaceEdges(graph, workspaceRepository) {
            @Override
            protected ClientApiWorkspaceEdges getEdges(HttpServletRequest request, String workspaceId, Iterable<String> vertexIds, Authorizations authorizations) {
                return super.getEdges(request, workspaceId, vertexIds, authorizations);
            }
        };

        Visibility workspaceVisibility = new Visibility("");
        Authorizations workspaceAuthorizations = graph.createAuthorizations("");
        Visibility visibility = new Visibility("");
        authorizations = graph.createAuthorizations("");

        workspaceVertex = graph.addVertex(WORKSPACE_ID, workspaceVisibility, workspaceAuthorizations);
        Vertex v1 = graph.addVertex("v1", visibility, authorizations);
        Vertex v2 = graph.addVertex("v2", visibility, authorizations);
        graph.addEdge("e1", v1, v2, "test", visibility, authorizations);

        workspace = new VertexiumWorkspace(workspaceVertex);
        when(workspaceRepository.findById(eq(WORKSPACE_ID), eq(user))).thenReturn(workspace);
    }

    @Test
    public void testHandleWithNoEntitiesOnWorkspace() throws Exception {
        List<WorkspaceEntity> workspaceEntities = new ArrayList<>();
        when(workspaceRepository.findEntities(workspace, user)).thenReturn(workspaceEntities);

        ClientApiWorkspaceEdges edges = workspaceEdges.handle(request, null, workspace.getWorkspaceId(), user, authorizations);
        assertEquals(0, edges.edges.size());
    }

    @Test
    public void testHandleWithEntitiesOnWorkspace() throws Exception {
        List<String> workspaceEntityVertexIds = new ArrayList<>();
        workspaceEntityVertexIds.add("v1");
        workspaceEntityVertexIds.add("v2");
        when(workspaceRepository.findEntityVertexIds(workspace, user)).thenReturn(workspaceEntityVertexIds);

        ClientApiWorkspaceEdges edges = workspaceEdges.handle(request, null, workspace.getWorkspaceId(), user, authorizations);
        assertEquals(1, edges.edges.size());
    }
}