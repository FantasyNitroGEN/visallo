package org.visallo.web.parameterProviders;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.vertexium.Authorizations;
import org.vertexium.inmemory.InMemoryAuthorizations;
import org.visallo.core.exception.VisalloAccessDeniedException;
import org.visallo.core.model.user.AuthorizationRepository;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.user.ProxyUser;

import javax.servlet.http.HttpServletRequest;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AuthorizationsParameterProviderFactoryTest {
    @Mock
    private HttpServletRequest request;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthorizationRepository authorizationRepository;

    @Mock
    private WorkspaceRepository workspaceRepository;

    private ProxyUser proxyUser;

    @Before
    public void before() {
        proxyUser = new ProxyUser("user123", userRepository);
    }

    @Test
    public void testGetAuthorizations() {
        Authorizations authorizations = new InMemoryAuthorizations("a", "b", "workspace123");

        when(request.getAttribute(eq(VisalloBaseParameterProvider.WORKSPACE_ID_ATTRIBUTE_NAME))).thenReturn("workspace123");
        when(request.getAttribute(eq(VisalloBaseParameterProvider.USER_REQUEST_ATTRIBUTE_NAME))).thenReturn(proxyUser);
        when(authorizationRepository.getGraphAuthorizations(eq(proxyUser), eq("workspace123"))).thenReturn(authorizations);
        when(workspaceRepository.hasReadPermissions(eq("workspace123"), eq(proxyUser))).thenReturn(true);
        Authorizations auth = AuthorizationsParameterProviderFactory.getAuthorizations(
                request,
                userRepository,
                authorizationRepository,
                workspaceRepository
        );
        assertArrayEquals(new String[]{"a", "b", "workspace123"}, auth.getAuthorizations());
    }

    @Test
    public void testGetAuthorizationsNoWorkspaceAccess() {
        when(request.getAttribute(eq(VisalloBaseParameterProvider.WORKSPACE_ID_ATTRIBUTE_NAME))).thenReturn("workspace123");
        when(request.getAttribute(eq(VisalloBaseParameterProvider.USER_REQUEST_ATTRIBUTE_NAME))).thenReturn(proxyUser);
        when(workspaceRepository.hasReadPermissions(eq("workspace123"), eq(proxyUser))).thenReturn(false);
        try {
            AuthorizationsParameterProviderFactory.getAuthorizations(
                    request,
                    userRepository,
                    authorizationRepository,
                    workspaceRepository
            );
            fail("expected exception");
        } catch (VisalloAccessDeniedException ex) {
            assertTrue(ex.getMessage().contains("workspace123"));
        }
    }

    @Test
    public void testGetAuthorizationsNoWorkspace() {
        Authorizations authorizations = new InMemoryAuthorizations("a", "b");

        when(request.getAttribute(eq(VisalloBaseParameterProvider.WORKSPACE_ID_ATTRIBUTE_NAME))).thenReturn(null);
        when(request.getAttribute(eq(VisalloBaseParameterProvider.USER_REQUEST_ATTRIBUTE_NAME))).thenReturn(proxyUser);
        when(authorizationRepository.getGraphAuthorizations(eq(proxyUser))).thenReturn(authorizations);
        Authorizations auth = AuthorizationsParameterProviderFactory.getAuthorizations(
                request,
                userRepository,
                authorizationRepository,
                workspaceRepository
        );
        assertArrayEquals(new String[]{"a", "b"}, auth.getAuthorizations());
    }

    @Test
    public void testGetAuthorizationsNoUser() {
        when(request.getAttribute(eq(VisalloBaseParameterProvider.USER_REQUEST_ATTRIBUTE_NAME))).thenReturn(null);
        Authorizations auth = AuthorizationsParameterProviderFactory.getAuthorizations(
                request,
                userRepository,
                authorizationRepository,
                workspaceRepository
        );
        assertNull("expected null authorizations", auth);
        verify(authorizationRepository, never()).getGraphAuthorizations(any());
        verify(authorizationRepository, never()).getGraphAuthorizations(any(), any());
    }
}