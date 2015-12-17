package org.visallo.web.auth;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.v5analytics.webster.HandlerChain;
import org.apache.commons.lang.StringUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.vertexium.Graph;
import org.visallo.core.config.Configuration;
import org.visallo.core.config.HashMapConfigurationLoader;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.user.User;
import org.visallo.model.directory.LdapSearchConfiguration;
import org.visallo.model.directory.LdapSearchService;
import org.visallo.model.directory.LdapTestHelpers;
import org.visallo.web.AuthenticationHandler;
import org.visallo.web.X509AuthenticationHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Matchers.isNull;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class LdapX509AuthenticationHandlerTest {
    private static InMemoryDirectoryServer ldapServer;

    @Mock
    private UserRepository userRepository;
    @Mock
    private User user;
    @Mock
    private Graph graph;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private HttpSession httpSession;
    @Mock
    private HandlerChain chain;

    @BeforeClass
    public static void setUp() throws Exception {
        ldapServer = LdapTestHelpers.configureInMemoryDirectoryServer();
        ldapServer.startListening();
    }

    @AfterClass
    public static void tearDown() {
        ldapServer.shutDown(true);
    }

    @Test
    public void testUserWithRequiredRole() throws Exception {
        LdapSearchService ldapSearchService = new LdapSearchService(LdapTestHelpers.getServerConfig(ldapServer), getSearchConfigWithExtraUserAttribute("role"));
        Configuration configuration = getConfigurationWithRequiredAttribute("role", "visallo_administrator");
        AuthenticationHandler authenticationHandler = new LdapX509AuthenticationHandler(userRepository, graph, ldapSearchService, configuration);

        X509Certificate cert = LdapTestHelpers.getPersonCertificate("alice");
        when(request.getAttribute(X509AuthenticationHandler.CERTIFICATE_REQUEST_ATTRIBUTE)).thenReturn(new X509Certificate[]{cert});
        when(userRepository.findOrAddUser((String) notNull(), (String) notNull(), (String) isNull(), (String) notNull(), (String[]) notNull())).thenReturn(user);
        when(user.toString()).thenReturn("alice");
        when(user.getUserId()).thenReturn("USER_alice");
        when(request.getSession()).thenReturn(httpSession);

        authenticationHandler.handle(request, response, chain);

        verify(chain).next(request, response);
    }

    @Test
    public void testUserWithoutRequiredRole() throws Exception {
        LdapSearchService ldapSearchService = new LdapSearchService(LdapTestHelpers.getServerConfig(ldapServer), getSearchConfigWithExtraUserAttribute("role"));
        Configuration configuration = getConfigurationWithRequiredAttribute("role", "visallo_administrator");
        AuthenticationHandler authenticationHandler = new LdapX509AuthenticationHandler(userRepository, graph, ldapSearchService, configuration);

        X509Certificate cert = LdapTestHelpers.getPersonCertificate("bob");
        when(request.getAttribute(X509AuthenticationHandler.CERTIFICATE_REQUEST_ATTRIBUTE)).thenReturn(new X509Certificate[]{cert});

        authenticationHandler.handle(request, response, chain);

        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN);
        verify(chain, never()).next(request, response);
    }

    @Test
    public void testUserWithoutAnyRoles() throws Exception {
        LdapSearchService ldapSearchService = new LdapSearchService(LdapTestHelpers.getServerConfig(ldapServer), getSearchConfigWithExtraUserAttribute("role"));
        Configuration configuration = getConfigurationWithRequiredAttribute("role", "visallo_administrator");
        AuthenticationHandler authenticationHandler = new LdapX509AuthenticationHandler(userRepository, graph, ldapSearchService, configuration);

        X509Certificate cert = LdapTestHelpers.getPersonCertificate("carlos");
        when(request.getAttribute(X509AuthenticationHandler.CERTIFICATE_REQUEST_ATTRIBUTE)).thenReturn(new X509Certificate[]{cert});

        authenticationHandler.handle(request, response, chain);

        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN);
        verify(chain, never()).next(request, response);
    }

    @Test
    public void testUserWithRequiredGroup() throws Exception {
        LdapSearchService ldapSearchService = new LdapSearchService(LdapTestHelpers.getServerConfig(ldapServer), LdapTestHelpers.getSearchConfig());
        Configuration configuration = getConfigurationWithRequiredGroups("managers");
        AuthenticationHandler authenticationHandler = new LdapX509AuthenticationHandler(userRepository, graph, ldapSearchService, configuration);

        X509Certificate cert = LdapTestHelpers.getPersonCertificate("carlos");
        when(request.getAttribute(X509AuthenticationHandler.CERTIFICATE_REQUEST_ATTRIBUTE)).thenReturn(new X509Certificate[]{cert});
        when(userRepository.findOrAddUser((String) notNull(), (String) notNull(), (String) isNull(), (String) notNull(), (String[]) notNull())).thenReturn(user);
        when(user.toString()).thenReturn("carlos");
        when(user.getUserId()).thenReturn("USER_carlos");
        when(request.getSession()).thenReturn(httpSession);

        authenticationHandler.handle(request, response, chain);

        verify(chain).next(request, response);
    }

    @Test
    public void testUserWithoutRequiredGroup() throws Exception {
        LdapSearchService ldapSearchService = new LdapSearchService(LdapTestHelpers.getServerConfig(ldapServer), LdapTestHelpers.getSearchConfig());
        Configuration configuration = getConfigurationWithRequiredGroups("managers");
        AuthenticationHandler authenticationHandler = new LdapX509AuthenticationHandler(userRepository, graph, ldapSearchService, configuration);

        X509Certificate cert = LdapTestHelpers.getPersonCertificate("bob");
        when(request.getAttribute(X509AuthenticationHandler.CERTIFICATE_REQUEST_ATTRIBUTE)).thenReturn(new X509Certificate[]{cert});

        authenticationHandler.handle(request, response, chain);

        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN);
        verify(chain, never()).next(request, response);
    }

    // TODO: user without any groups?

    @Test(expected = VisalloException.class)
    public void testUserWithoutLdapEntry() throws Exception {
        LdapSearchService ldapSearchService = new LdapSearchService(LdapTestHelpers.getServerConfig(ldapServer), LdapTestHelpers.getSearchConfig());
        Map<String, String> map = new HashMap<>();
        Configuration configuration = new HashMapConfigurationLoader(map).createConfiguration();
        AuthenticationHandler authenticationHandler = new LdapX509AuthenticationHandler(userRepository, graph, ldapSearchService, configuration);

        X509Certificate cert = LdapTestHelpers.getPersonCertificate("diane");
        when(request.getAttribute(X509AuthenticationHandler.CERTIFICATE_REQUEST_ATTRIBUTE)).thenReturn(new X509Certificate[]{cert});

        authenticationHandler.handle(request, response, chain);
    }

    private LdapSearchConfiguration getSearchConfigWithExtraUserAttribute(String extraAttribute) {
        LdapSearchConfiguration searchConfiguration = LdapTestHelpers.getSearchConfig();
        List<String> userAttributes = new ArrayList<>(searchConfiguration.getUserAttributes());
        userAttributes.add(extraAttribute);
        searchConfiguration.setUserAttributes(StringUtils.join(userAttributes, ","));
        return searchConfiguration;
    }

    private Configuration getConfigurationWithRequiredAttribute(String attribute, String values) {
        Map<String, String> map = new HashMap<>();
        map.put("ldap.x509Authentication.requiredAttribute", attribute);
        map.put("ldap.x509Authentication.requiredAttributeValues", values);
        return new HashMapConfigurationLoader(map).createConfiguration();
    }

    private Configuration getConfigurationWithRequiredGroups(String groups) {
        Map<String, String> map = new HashMap<>();
        map.put("ldap.x509Authentication.requiredGroups", groups);
        return new HashMapConfigurationLoader(map).createConfiguration();
    }
}
