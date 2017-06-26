package org.visallo.web.routes.vertex;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.vertexium.Authorizations;
import org.vertexium.Vertex;
import org.visallo.core.exception.VisalloAccessDeniedException;
import org.visallo.core.model.user.AuthorizationRepository;
import org.visallo.web.clientapi.model.ClientApiVertex;
import org.visallo.web.clientapi.model.ClientApiVertexMultipleResponse;
import org.visallo.web.clientapi.model.VisibilityJson;
import org.visallo.web.routes.RouteTestBase;

import java.io.IOException;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.visallo.web.parameterProviders.VisalloBaseParameterProvider.USER_REQUEST_ATTRIBUTE_NAME;
import static org.visallo.web.parameterProviders.VisalloBaseParameterProvider.WORKSPACE_ID_ATTRIBUTE_NAME;

@RunWith(MockitoJUnitRunner.class)
public class VertexMultipleTest extends RouteTestBase {
    private VertexMultiple route;

    private Authorizations userAuthorizations;
    private Authorizations workspaceAuthorizations;

    @Mock
    private AuthorizationRepository authorizationRepository;

    private Vertex publicVertex;
    private Vertex sandboxedVertex;

    @Before
    public void before() throws IOException {
        super.before();

        userAuthorizations = graph.createAuthorizations("junit");
        workspaceAuthorizations = graph.createAuthorizations(userAuthorizations, WORKSPACE_ID);

        publicVertex = graph.addVertex("v1", visibilityTranslator.getDefaultVisibility(), userAuthorizations);

        VisibilityJson visibilityJson = new VisibilityJson();
        visibilityJson.addWorkspace(WORKSPACE_ID);
        sandboxedVertex = graph.addVertex("v2", visibilityTranslator.toVisibility(visibilityJson).getVisibility(), workspaceAuthorizations);

        route = new VertexMultiple(graph, userRepository, workspaceRepository, authorizationRepository);
    }

    @Test
    public void testGetVerticesWithNoWorkspaceId() throws Exception {
        when(authorizationRepository.getGraphAuthorizations(user)).thenReturn(userAuthorizations);
        when(request.getAttribute(eq(WORKSPACE_ID_ATTRIBUTE_NAME))).thenReturn(null);

        ClientApiVertexMultipleResponse response = route.handle(request, new String[]{publicVertex.getId()}, true, user);

        assertFalse(response.isRequiredFallback());
        assertEquals(1, response.getVertices().size());
        assertEquals(publicVertex.getId(), response.getVertices().get(0).getId());
    }

    @Test(expected = VisalloAccessDeniedException.class)
    public void testGetVerticesWithNoWorkspaceAccessAndNoFallback() throws Exception {
        when(authorizationRepository.getGraphAuthorizations(user)).thenReturn(userAuthorizations);
        when(request.getAttribute(USER_REQUEST_ATTRIBUTE_NAME)).thenReturn(user);
        when(workspaceRepository.hasReadPermissions(WORKSPACE_ID, user)).thenReturn(false);

        route.handle(request, new String[]{publicVertex.getId()}, false, user);
    }

    @Test
    public void testGetVerticesWithNoWorkspaceAccessAndFallbackToPublic() throws Exception {
        when(authorizationRepository.getGraphAuthorizations(user)).thenReturn(userAuthorizations);
        when(request.getAttribute(USER_REQUEST_ATTRIBUTE_NAME)).thenReturn(user);
        when(workspaceRepository.hasReadPermissions(WORKSPACE_ID, user)).thenReturn(false);

        ClientApiVertexMultipleResponse response = route.handle(request, new String[]{publicVertex.getId(), sandboxedVertex.getId()}, true, user);

        assertTrue(response.isRequiredFallback());
        assertEquals(1, response.getVertices().size());
        assertEquals(publicVertex.getId(), response.getVertices().get(0).getId());
    }

    @Test
    public void testGetVertices() throws Exception {
        when(authorizationRepository.getGraphAuthorizations(user, WORKSPACE_ID)).thenReturn(workspaceAuthorizations);
        when(request.getAttribute(USER_REQUEST_ATTRIBUTE_NAME)).thenReturn(user);
        when(workspaceRepository.hasReadPermissions(WORKSPACE_ID, user)).thenReturn(true);

        ClientApiVertexMultipleResponse response = route.handle(request, new String[]{publicVertex.getId(), sandboxedVertex.getId()}, true, user);

        assertFalse(response.isRequiredFallback());
        assertEquals(2, response.getVertices().size());
        assertTrue(response.getVertices().stream().map(ClientApiVertex::getId).collect(Collectors.toList()).contains(publicVertex.getId()));
        assertTrue(response.getVertices().stream().map(ClientApiVertex::getId).collect(Collectors.toList()).contains(sandboxedVertex.getId()));
    }

    @Test
    public void testGetVerticesWithUnknownId() throws Exception {
        when(authorizationRepository.getGraphAuthorizations(user, WORKSPACE_ID)).thenReturn(workspaceAuthorizations);
        when(request.getAttribute(USER_REQUEST_ATTRIBUTE_NAME)).thenReturn(user);
        when(workspaceRepository.hasReadPermissions(WORKSPACE_ID, user)).thenReturn(true);

        ClientApiVertexMultipleResponse response = route.handle(request, new String[]{"no-vertex-id"}, true, user);

        assertFalse(response.isRequiredFallback());
        assertEquals(0, response.getVertices().size());
    }
    @Test
    public void testGetVerticesWithNoneSpecified() throws Exception {
        when(authorizationRepository.getGraphAuthorizations(user, WORKSPACE_ID)).thenReturn(workspaceAuthorizations);
        when(request.getAttribute(USER_REQUEST_ATTRIBUTE_NAME)).thenReturn(user);
        when(workspaceRepository.hasReadPermissions(WORKSPACE_ID, user)).thenReturn(true);

        ClientApiVertexMultipleResponse response = route.handle(request, new String[]{}, true, user);

        assertFalse(response.isRequiredFallback());
        assertEquals(0, response.getVertices().size());
    }
}
