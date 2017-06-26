package org.visallo.web.parameterProviders;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.visallo.core.exception.VisalloAccessDeniedException;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.user.ProxyUser;
import org.visallo.core.user.User;
import org.visallo.web.CurrentUser;
import org.visallo.web.SessionUser;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;
import static org.visallo.web.parameterProviders.VisalloBaseParameterProvider.*;

@RunWith(MockitoJUnitRunner.class)
public class VisalloBaseParameterProviderTest {
    private static final String USER_ID = "user123";
    private static final String WORKSPACE_ID = "workspace123";

    @Mock
    private HttpServletRequest request;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private HttpSession session;

    @Before
    public void before() {
        when(request.getSession()).thenReturn(session);
        when(request.getSession(false)).thenReturn(session);
    }

    @Test
    public void testGetUserWhenSetInRequestAlready() {
        ProxyUser user = new ProxyUser(USER_ID, userRepository);
        when(request.getAttribute(VisalloBaseParameterProvider.USER_REQUEST_ATTRIBUTE_NAME)).thenReturn(user);
        User foundUser = VisalloBaseParameterProvider.getUser(request, userRepository);
        assertEquals(user, foundUser);
    }

    @Test
    public void testGetUserWhenSetInSession() {
        SessionUser sessionUser = new SessionUser(USER_ID);
        when(session.getAttribute(CurrentUser.SESSIONUSER_ATTRIBUTE_NAME)).thenReturn(sessionUser);
        User foundUser = VisalloBaseParameterProvider.getUser(request, userRepository);
        assertEquals(USER_ID, foundUser.getUserId());
    }

    @Test
    public void testGetUserWhenNotSet() {
        User foundUser = VisalloBaseParameterProvider.getUser(request, userRepository);
        assertNull("user should be null", foundUser);
    }

    @Test
    public void testGetActiveWorkspaceIdOrDefaultFromAttribute() {
        ProxyUser user = new ProxyUser(USER_ID, userRepository);
        when(request.getAttribute(VisalloBaseParameterProvider.USER_REQUEST_ATTRIBUTE_NAME)).thenReturn(user);
        when(request.getAttribute(WORKSPACE_ID_ATTRIBUTE_NAME)).thenReturn(WORKSPACE_ID);
        when(workspaceRepository.hasReadPermissions(WORKSPACE_ID, user)).thenReturn(true);

        String workspaceId = getActiveWorkspaceIdOrDefault(request, workspaceRepository, userRepository);
        assertEquals(WORKSPACE_ID, workspaceId);
    }

    @Test
    public void testGetActiveWorkspaceIdOrDefaultFromHeader() {
        ProxyUser user = new ProxyUser(USER_ID, userRepository);
        when(request.getAttribute(VisalloBaseParameterProvider.USER_REQUEST_ATTRIBUTE_NAME)).thenReturn(user);
        when(request.getHeader(VISALLO_WORKSPACE_ID_HEADER_NAME)).thenReturn(WORKSPACE_ID);
        when(workspaceRepository.hasReadPermissions(WORKSPACE_ID, user)).thenReturn(true);

        String workspaceId = getActiveWorkspaceIdOrDefault(request, workspaceRepository, userRepository);
        assertEquals(WORKSPACE_ID, workspaceId);
    }

    @Test
    public void testGetActiveWorkspaceIdOrDefaultFromParameter() {
        ProxyUser user = new ProxyUser(USER_ID, userRepository);
        when(request.getAttribute(VisalloBaseParameterProvider.USER_REQUEST_ATTRIBUTE_NAME)).thenReturn(user);
        when(request.getParameter(WORKSPACE_ID_ATTRIBUTE_NAME)).thenReturn(WORKSPACE_ID);
        when(workspaceRepository.hasReadPermissions(WORKSPACE_ID, user)).thenReturn(true);

        String workspaceId = getActiveWorkspaceIdOrDefault(request, workspaceRepository, userRepository);
        assertEquals(WORKSPACE_ID, workspaceId);
    }

    @Test
    public void testGetActiveWorkspaceIdOrDefaultWithMissingWorkspaceId() {
        String workspaceId = getActiveWorkspaceIdOrDefault(request, workspaceRepository, userRepository);
        assertNull(workspaceId);
    }

    @Test(expected = VisalloAccessDeniedException.class)
    public void testGetActiveWorkspaceIdOrDefaultWithNoUser() {
        when(request.getParameter(WORKSPACE_ID_ATTRIBUTE_NAME)).thenReturn(WORKSPACE_ID);
        getActiveWorkspaceIdOrDefault(request, workspaceRepository, userRepository);
    }

    @Test(expected = VisalloAccessDeniedException.class)
    public void testGetActiveWorkspaceIdOrDefaultWithNoAccess() {
        ProxyUser user = new ProxyUser(USER_ID, userRepository);
        when(request.getAttribute(VisalloBaseParameterProvider.USER_REQUEST_ATTRIBUTE_NAME)).thenReturn(user);
        when(request.getParameter(WORKSPACE_ID_ATTRIBUTE_NAME)).thenReturn(WORKSPACE_ID);
        when(workspaceRepository.hasReadPermissions(WORKSPACE_ID, user)).thenReturn(false);

        getActiveWorkspaceIdOrDefault(request, workspaceRepository, userRepository);
    }

    @Test
    public void testGetActiveWorkspaceId() {
        ProxyUser user = new ProxyUser(USER_ID, userRepository);
        when(request.getAttribute(VisalloBaseParameterProvider.USER_REQUEST_ATTRIBUTE_NAME)).thenReturn(user);
        when(request.getParameter(WORKSPACE_ID_ATTRIBUTE_NAME)).thenReturn(WORKSPACE_ID);
        when(workspaceRepository.hasReadPermissions(WORKSPACE_ID, user)).thenReturn(true);

        String workspaceId = getActiveWorkspaceId(request, workspaceRepository, userRepository);
        assertEquals(WORKSPACE_ID, workspaceId);
    }

    @Test(expected = VisalloException.class)
    public void testGetActiveWorkspaceIdWithMissingWorkspaceId() {
        getActiveWorkspaceId(request, workspaceRepository, userRepository);
    }

    @Test(expected = VisalloAccessDeniedException.class)
    public void testGetActiveWorkspaceIdNoAccess() {
        ProxyUser user = new ProxyUser(USER_ID, userRepository);
        when(request.getAttribute(VisalloBaseParameterProvider.USER_REQUEST_ATTRIBUTE_NAME)).thenReturn(user);
        when(request.getParameter(WORKSPACE_ID_ATTRIBUTE_NAME)).thenReturn(WORKSPACE_ID);
        when(workspaceRepository.hasReadPermissions(WORKSPACE_ID, user)).thenReturn(false);

        getActiveWorkspaceId(request, workspaceRepository, userRepository);
    }
}