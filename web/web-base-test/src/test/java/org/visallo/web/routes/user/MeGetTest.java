package org.visallo.web.routes.user;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.workspace.Workspace;
import org.visallo.core.user.ProxyUser;
import org.visallo.core.user.User;
import org.visallo.web.routes.RouteTestBase;
import org.visallo.web.clientapi.model.ClientApiUser;

import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MeGetTest extends RouteTestBase {
    private MeGet meGet;

    private Workspace workspaceShared1;
    private Workspace workspaceShared2;
    private Workspace workspaceCreator1;
    private Workspace workspaceCreator2;

    @Before
    public void before() throws IOException {
        super.before();
        meGet = new MeGet(userRepository, workspaceRepository);
        User otherUser = new ProxyUser("other-user", userRepository);

        workspaceShared1 = new TestWorkspace("junit-id-1", "B-junit-display-title");
        workspaceShared2 = new TestWorkspace("junit-id-2", "a-junit-display-title");
        workspaceCreator1 = new TestWorkspace("junit-id-3", "b-junit-display-title");
        workspaceCreator2 = new TestWorkspace("junit-id-4", "A-junit-display-title");
        when(workspaceRepository.getCreatorUserId(workspaceShared1.getWorkspaceId(), user)).thenReturn(otherUser.getUserId());
        when(workspaceRepository.getCreatorUserId(workspaceShared2.getWorkspaceId(), user)).thenReturn(otherUser.getUserId());
        when(workspaceRepository.getCreatorUserId(workspaceCreator1.getWorkspaceId(), user)).thenReturn(user.getUserId());
        when(workspaceRepository.getCreatorUserId(workspaceCreator2.getWorkspaceId(), user)).thenReturn(user.getUserId());
    }

    @Test
    public void testNoCurrentWorkspace() throws Exception {
        ClientApiUser clientApiUser = new ClientApiUser();
        clientApiUser.setCurrentWorkspaceId(null);
        when(userRepository.toClientApiPrivate(eq(user))).thenReturn(clientApiUser);

        Workspace workspace = new TestWorkspace("junit-id", "junit-display-title");
        when(workspaceRepository.add(user)).thenReturn(workspace);

        ClientApiUser response = meGet.handle(request, user);
        assertEquals(workspace.getWorkspaceId(), response.getCurrentWorkspaceId());
        assertEquals(workspace.getDisplayTitle(), response.getCurrentWorkspaceName());
    }

    @Test
    public void testValidCurrentWorkspace() throws Exception {
        ClientApiUser clientApiUser = new ClientApiUser();
        clientApiUser.setCurrentWorkspaceId("WORKSPACE_123");
        when(userRepository.toClientApiPrivate(eq(user))).thenReturn(clientApiUser);
        when(workspaceRepository.hasReadPermissions(eq("WORKSPACE_123"), eq(user))).thenReturn(true);

        ClientApiUser response = meGet.handle(request, user);
        assertEquals("WORKSPACE_123", response.getCurrentWorkspaceId());
    }

    @Test
    public void testNoReadPermissionsOnCurrentWorkspaceAddNew() throws Exception {
        ClientApiUser clientApiUser = new ClientApiUser();
        clientApiUser.setCurrentWorkspaceId("WORKSPACE_123");
        when(userRepository.toClientApiPrivate(eq(user))).thenReturn(clientApiUser);
        when(workspaceRepository.hasReadPermissions(eq("WORKSPACE_123"), eq(user))).thenReturn(false);

        Workspace workspace = new TestWorkspace("junit-id", "junit-display-title");
        when(workspaceRepository.add(user)).thenReturn(workspace);

        ClientApiUser response = meGet.handle(request, user);
        assertEquals(workspace.getWorkspaceId(), response.getCurrentWorkspaceId());
        assertEquals(workspace.getDisplayTitle(), response.getCurrentWorkspaceName());
    }

    @Test
    public void testNoReadPermissionsOnCurrentWorkspaceWithOtherAsCreator() throws Exception {
        ClientApiUser clientApiUser = new ClientApiUser();
        clientApiUser.setCurrentWorkspaceId("WORKSPACE_123");
        when(userRepository.toClientApiPrivate(eq(user))).thenReturn(clientApiUser);
        when(workspaceRepository.hasReadPermissions(eq("WORKSPACE_123"), eq(user))).thenReturn(false);

        when(workspaceRepository.findAllForUser(user)).thenReturn(Arrays.asList(workspaceShared1, workspaceCreator1, workspaceCreator2));

        ClientApiUser response = meGet.handle(request, user);
        assertEquals(workspaceCreator2.getWorkspaceId(), response.getCurrentWorkspaceId());
        assertEquals(workspaceCreator2.getDisplayTitle(), response.getCurrentWorkspaceName());
    }

    @Test
    public void testNoReadPermissionsOnCurrentWorkspaceWithOtherAsShared() throws Exception {
        ClientApiUser clientApiUser = new ClientApiUser();
        clientApiUser.setCurrentWorkspaceId("WORKSPACE_123");
        when(userRepository.toClientApiPrivate(eq(user))).thenReturn(clientApiUser);
        when(workspaceRepository.hasReadPermissions(eq("WORKSPACE_123"), eq(user))).thenReturn(false);

        when(workspaceRepository.findAllForUser(user)).thenReturn(Arrays.asList(workspaceShared1, workspaceShared2));

        ClientApiUser response = meGet.handle(request, user);
        assertEquals(workspaceShared2.getWorkspaceId(), response.getCurrentWorkspaceId());
        assertEquals(workspaceShared2.getDisplayTitle(), response.getCurrentWorkspaceName());
    }

    @Test
    public void testHasReadPermissionsThrowsExceptionAddNew() throws Exception {
        ClientApiUser clientApiUser = new ClientApiUser();
        clientApiUser.setCurrentWorkspaceId("WORKSPACE_123");
        when(userRepository.toClientApiPrivate(eq(user))).thenReturn(clientApiUser);
        when(workspaceRepository.hasReadPermissions(eq("WORKSPACE_123"), eq(user))).thenThrow(new VisalloException("boom"));

        Workspace workspace = new TestWorkspace("junit-id", "junit-display-title");
        when(workspaceRepository.add(user)).thenReturn(workspace);

        ClientApiUser response = meGet.handle(request, user);
        assertEquals(workspace.getWorkspaceId(), response.getCurrentWorkspaceId());
        assertEquals(workspace.getDisplayTitle(), response.getCurrentWorkspaceName());
    }

    @Test
    public void testHasReadPermissionsThrowsExceptionWithOtherAsCreator() throws Exception {
        ClientApiUser clientApiUser = new ClientApiUser();
        clientApiUser.setCurrentWorkspaceId("WORKSPACE_123");
        when(userRepository.toClientApiPrivate(eq(user))).thenReturn(clientApiUser);
        when(workspaceRepository.hasReadPermissions(eq("WORKSPACE_123"), eq(user))).thenThrow(new VisalloException("boom"));

        when(workspaceRepository.findAllForUser(user)).thenReturn(Arrays.asList(workspaceCreator1, workspaceShared1, workspaceCreator2));

        ClientApiUser response = meGet.handle(request, user);
        assertEquals(workspaceCreator2.getWorkspaceId(), response.getCurrentWorkspaceId());
        assertEquals(workspaceCreator2.getDisplayTitle(), response.getCurrentWorkspaceName());
    }

    @Test
    public void testHasReadPermissionsThrowsExceptionWithOtherAsShared() throws Exception {
        ClientApiUser clientApiUser = new ClientApiUser();
        clientApiUser.setCurrentWorkspaceId("WORKSPACE_123");
        when(userRepository.toClientApiPrivate(eq(user))).thenReturn(clientApiUser);
        when(workspaceRepository.hasReadPermissions(eq("WORKSPACE_123"), eq(user))).thenThrow(new VisalloException("boom"));

        when(workspaceRepository.findAllForUser(user)).thenReturn(Arrays.asList(workspaceShared1, workspaceShared2));

        ClientApiUser response = meGet.handle(request, user);
        assertEquals(workspaceShared2.getWorkspaceId(), response.getCurrentWorkspaceId());
        assertEquals(workspaceShared2.getDisplayTitle(), response.getCurrentWorkspaceName());
    }

    private class TestWorkspace implements Workspace {
        private String id;
        private String title;

        TestWorkspace(String id, String title) {
            this.id = id;
            this.title = title;
        }

        @Override
        public String getWorkspaceId() { return id; }

        @Override
        public String getDisplayTitle() { return title; }
    }
}