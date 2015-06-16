package org.visallo.web.routes.user;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.vertexium.Authorizations;
import org.vertexium.Visibility;
import org.visallo.core.exception.VisalloException;
import org.visallo.web.RouteTestBase;
import org.visallo.web.clientapi.model.ClientApiUser;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MeGetTest extends RouteTestBase {
    private MeGet meGet;
    private Visibility visibility;
    private Authorizations authorizations;

    @Before
    public void setUp() throws IOException {
        super.setUp();

        visibility = new Visibility("");
        authorizations = graph.createAuthorizations("");

        meGet = new MeGet(userRepository, workspaceRepository, configuration);
    }

    @Test
    public void testNoCurrentWorkspace() throws Exception {
        ClientApiUser clientApiUser = new ClientApiUser();
        clientApiUser.setCurrentWorkspaceId(null);
        when(userRepository.toClientApiPrivate(eq(user))).thenReturn(clientApiUser);

        ClientApiUser response = handle(meGet, ClientApiUser.class);
        assertEquals(null, response.getCurrentWorkspaceId());
    }

    @Test
    public void testValidCurrentWorkspace() throws Exception {
        ClientApiUser clientApiUser = new ClientApiUser();
        clientApiUser.setCurrentWorkspaceId("WORKSPACE_123");
        when(userRepository.toClientApiPrivate(eq(user))).thenReturn(clientApiUser);
        when(workspaceRepository.hasReadPermissions(eq("WORKSPACE_123"), eq(user))).thenReturn(true);

        ClientApiUser response = handle(meGet, ClientApiUser.class);
        assertEquals("WORKSPACE_123", response.getCurrentWorkspaceId());
    }

    @Test
    public void testNoReadPermissionsOnCurrentWorkspace() throws Exception {
        ClientApiUser clientApiUser = new ClientApiUser();
        clientApiUser.setCurrentWorkspaceId("WORKSPACE_123");
        when(userRepository.toClientApiPrivate(eq(user))).thenReturn(clientApiUser);
        when(workspaceRepository.hasReadPermissions(eq("WORKSPACE_123"), eq(user))).thenReturn(false);

        ClientApiUser response = handle(meGet, ClientApiUser.class);
        assertEquals(null, response.getCurrentWorkspaceId());
    }

    @Test
    public void testHasReadPermissionsThrowsException() throws Exception {
        ClientApiUser clientApiUser = new ClientApiUser();
        clientApiUser.setCurrentWorkspaceId("WORKSPACE_123");
        when(userRepository.toClientApiPrivate(eq(user))).thenReturn(clientApiUser);
        when(workspaceRepository.hasReadPermissions(eq("WORKSPACE_123"), eq(user))).thenThrow(new VisalloException("boom"));

        ClientApiUser response = handle(meGet, ClientApiUser.class);
        assertEquals(null, response.getCurrentWorkspaceId());
    }
}