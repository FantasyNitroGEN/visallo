package org.visallo.web;

import org.atmosphere.cpr.AtmosphereResource;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.visallo.core.model.user.UserRepository;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MessagingFilterTest {
    private MessagingFilter messagingFilter;

    @Mock
    private AtmosphereResource atmosphereResource;

    @Mock
    private UserRepository userRepository;

    @Mock
    private org.atmosphere.cpr.AtmosphereRequest request;

    @Mock
    private javax.servlet.http.HttpSession session;

    @Mock
    private SessionUser sessionUser;

    @Before
    public void before() {
        messagingFilter = new MessagingFilter();
        messagingFilter.setUserRepository(userRepository);
    }

    @Test
    public void testShouldNotSendSetActiveWorkspaceMessage() {
        JSONObject message = new JSONObject();
        message.put("type", MessagingFilter.TYPE_SET_ACTIVE_WORKSPACE);

        assertFalse(messagingFilter.shouldSendMessage(message, null));
    }

    @Test
    public void testShouldSendMessageSessionNull() {
        JSONObject message = new JSONObject();

        assertFalse(messagingFilter.shouldSendMessage(message, null));
    }

    @Test
    public void testShouldSendMessageSessionNullAndSessionExpiration() {
        JSONObject message = new JSONObject();
        message.put("type", MessagingFilter.TYPE_SESSION_EXPIRATION);

        assertTrue(messagingFilter.shouldSendMessage(message, null));
    }

    @Test
    public void testShouldSendMessageSessionNotNull() {
        JSONObject message = new JSONObject();

        assertTrue(messagingFilter.shouldSendMessage(message, session));
    }

    @Test
    public void testShouldSendMessageBasedOnUserPermissions() {
        when(sessionUser.getUserId()).thenReturn("user123");
        when(session.getAttribute(CurrentUser.SESSIONUSER_ATTRIBUTE_NAME)).thenReturn(sessionUser);

        JSONObject message = new JSONObject("{ permissions: { users: ['user123'] } }");

        assertTrue(messagingFilter.shouldSendMessage(message, session));
    }

    @Test
    public void testShouldNotSendMessageBasedOnUserPermissions() {
        when(sessionUser.getUserId()).thenReturn("user123");
        when(session.getAttribute(CurrentUser.SESSIONUSER_ATTRIBUTE_NAME)).thenReturn(sessionUser);

        JSONObject message = new JSONObject("{ permissions: { users: ['user456'] } }");

        assertFalse(messagingFilter.shouldSendMessage(message, session));
    }

    @Test
    public void testShouldSendMessageBasedOnSessionIdPermissions() {
        when(session.getId()).thenReturn("session123");

        JSONObject message = new JSONObject("{ permissions: { sessionIds: ['session123'] } }");

        assertTrue(messagingFilter.shouldSendMessage(message, session));
    }

    @Test
    public void testShouldNotSendMessageBasedOnSessionIdPermissions() {
        when(session.getId()).thenReturn("session123");

        JSONObject message = new JSONObject("{ permissions: { sessionIds: ['session456'] } }");

        assertFalse(messagingFilter.shouldSendMessage(message, session));
    }

    @Test
    public void testShouldSendMessageBasedOnWorkspacesPermissions() {
        when(userRepository.getCurrentWorkspaceId("user123")).thenReturn("workspace123");
        when(sessionUser.getUserId()).thenReturn("user123");
        when(session.getAttribute(CurrentUser.SESSIONUSER_ATTRIBUTE_NAME)).thenReturn(sessionUser);

        JSONObject message = new JSONObject("{ permissions: { workspaces: ['workspace123'] } }");

        assertTrue(messagingFilter.shouldSendMessage(message, session));
    }

    @Test
    public void testShouldNotSendMessageBasedOnWorkspacesPermissions() {
        when(userRepository.getCurrentWorkspaceId("user123")).thenReturn("workspace123");
        when(sessionUser.getUserId()).thenReturn("user123");
        when(session.getAttribute(CurrentUser.SESSIONUSER_ATTRIBUTE_NAME)).thenReturn(sessionUser);

        JSONObject message = new JSONObject("{ permissions: { workspaces: ['workspace456'] } }");

        assertFalse(messagingFilter.shouldSendMessage(message, session));
    }
}