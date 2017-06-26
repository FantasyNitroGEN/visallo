package org.visallo.web.routes.edge;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.vertexium.Authorizations;
import org.vertexium.Edge;
import org.visallo.core.model.user.AuthorizationRepository;
import org.visallo.web.clientapi.model.*;
import org.visallo.web.routes.RouteTestBase;

import java.io.IOException;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EdgeMultipleTest  extends RouteTestBase {
    private EdgeMultiple route;

    private Authorizations userAuthorizations;
    private Authorizations workspaceAuthorizations;

    @Mock
    private AuthorizationRepository authorizationRepository;

    private Edge publicEdge;
    private Edge sandboxedEdge;

    @Before
    public void before() throws IOException {
        super.before();

        userAuthorizations = graph.createAuthorizations("junit");
        workspaceAuthorizations = graph.createAuthorizations(userAuthorizations, WORKSPACE_ID);

        publicEdge = graph.addEdge("e1", "v1", "v2", visibilityTranslator.getDefaultVisibility(), userAuthorizations);

        VisibilityJson visibilityJson = new VisibilityJson();
        visibilityJson.addWorkspace(WORKSPACE_ID);
        sandboxedEdge = graph.addEdge("e2", "v1", "v2", visibilityTranslator.toVisibility(visibilityJson).getVisibility(), workspaceAuthorizations);

        route = new EdgeMultiple(graph, authorizationRepository);
    }

    @Test
    public void testGetEdgesWithNoneFound() throws Exception {
        when(authorizationRepository.getGraphAuthorizations(user)).thenReturn(userAuthorizations);

        ClientApiEdgeMultipleResponse response = route.handle(new String[]{"no-edge-id"}, null, request, user);

        assertEquals(0, response.getEdges().size());
    }

    @Test
    public void testGetEdgesWithNoneSpecfied() throws Exception {
        when(authorizationRepository.getGraphAuthorizations(user)).thenReturn(userAuthorizations);

        ClientApiEdgeMultipleResponse response = route.handle(new String[]{}, null, request, user);

        assertEquals(0, response.getEdges().size());
    }

    @Test
    public void testGetEdgesWithNoWorkspaceId() throws Exception {
        when(authorizationRepository.getGraphAuthorizations(user)).thenReturn(userAuthorizations);

        ClientApiEdgeMultipleResponse response = route.handle(new String[]{publicEdge.getId()}, null, request, user);

        assertEquals(1, response.getEdges().size());
        assertEquals(publicEdge.getId(), response.getEdges().get(0).getId());
    }

    @Test
    public void testGetEdges() throws Exception {
        when(authorizationRepository.getGraphAuthorizations(user, WORKSPACE_ID)).thenReturn(workspaceAuthorizations);

        ClientApiEdgeMultipleResponse response = route.handle(new String[]{publicEdge.getId(), sandboxedEdge.getId()}, WORKSPACE_ID, request, user);

        assertEquals(2, response.getEdges().size());
        assertTrue(response.getEdges().stream().map(ClientApiEdge::getId).collect(Collectors.toList()).contains(publicEdge.getId()));
        assertTrue(response.getEdges().stream().map(ClientApiEdge::getId).collect(Collectors.toList()).contains(sandboxedEdge.getId()));
    }
}
