package org.visallo.web.parameterProviders;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.user.ProxyUser;
import org.visallo.core.user.User;
import org.visallo.web.CurrentUser;
import org.visallo.web.SessionUser;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class VisalloBaseParameterProviderTest {
    @Mock
    private HttpServletRequest request;

    @Mock
    private UserRepository userRepository;

    @Mock
    private HttpSession session;

    @Before
    public void before() {
        when(request.getSession()).thenReturn(session);
        when(request.getSession(false)).thenReturn(session);
    }

    @Test
    public void testGetUserWhenSetInRequestAlready() {
        ProxyUser user = new ProxyUser("user123", userRepository);
        when(request.getAttribute(VisalloBaseParameterProvider.USER_REQUEST_ATTRIBUTE_NAME)).thenReturn(user);
        User foundUser = VisalloBaseParameterProvider.getUser(request, userRepository);
        assertEquals(user, foundUser);
    }

    @Test
    public void testGetUserWhenSetInSession() {
        SessionUser sessionUser = new SessionUser("user123");
        when(session.getAttribute(CurrentUser.SESSIONUSER_ATTRIBUTE_NAME)).thenReturn(sessionUser);
        User foundUser = VisalloBaseParameterProvider.getUser(request, userRepository);
        assertEquals("user123", foundUser.getUserId());
    }

    @Test
    public void testGetUSerWhenNotSet() {
        User foundUser = VisalloBaseParameterProvider.getUser(request, userRepository);
        assertNull("user should be null", foundUser);
    }
}