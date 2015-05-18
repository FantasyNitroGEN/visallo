package org.visallo.ldap;

import org.visallo.core.exception.VisalloException;
import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldif.LDIFException;
import com.unboundid.ldif.LDIFReader;
import com.unboundid.util.ssl.KeyStoreKeyManager;
import com.unboundid.util.ssl.SSLUtil;
import com.unboundid.util.ssl.TrustAllTrustManager;
import com.unboundid.util.ssl.TrustStoreTrustManager;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Set;

import static org.junit.Assert.*;

public class LdapSearchServiceTest {
    private static final String BIND_DN = "cn=root,dc=test,dc=visallo,dc=org";
    private static final String BIND_PASSWORD = "visallo";
    private static InMemoryDirectoryServer ldapServer;

    @BeforeClass
    public static void setUp() throws Exception {
        ldapServer = configureInMemoryDirectoryServer();
        ldapServer.startListening();
    }

    public static InMemoryDirectoryServer configureInMemoryDirectoryServer() throws Exception {
        KeyStoreKeyManager ksManager = new KeyStoreKeyManager(classpathResource("/keystore.jks"), "password".toCharArray());
        TrustStoreTrustManager tsManager = new TrustStoreTrustManager(classpathResource("/truststore.jks"));
        SSLUtil serverSslUtil = new SSLUtil(ksManager, tsManager);
        SSLUtil clientSslUtil = new SSLUtil(new TrustAllTrustManager());
        InMemoryListenerConfig sslConfig = InMemoryListenerConfig.createLDAPSConfig(
                "LDAPS",
                null,
                4636,
                serverSslUtil.createSSLServerSocketFactory(),
                clientSslUtil.createSSLSocketFactory()
        );

        InMemoryDirectoryServerConfig ldapConfig = new InMemoryDirectoryServerConfig("dc=test,dc=visallo,dc=org");
        ldapConfig.addAdditionalBindCredentials(BIND_DN, BIND_PASSWORD);
        ldapConfig.setListenerConfigs(sslConfig);
        ldapConfig.setSchema(null);

        InMemoryDirectoryServer ldapServer = new InMemoryDirectoryServer(ldapConfig);

        ldapServer.importFromLDIF(false, classpathResource("/init.ldif"));
        ldapServer.importFromLDIF(false, classpathResource("/people.ldif"));
        ldapServer.importFromLDIF(false, classpathResource("/people-alice.ldif"));
        ldapServer.importFromLDIF(false, classpathResource("/people-bob.ldif"));
        ldapServer.importFromLDIF(false, classpathResource("/people-carlos.ldif"));
        ldapServer.importFromLDIF(false, classpathResource("/groups.ldif"));
        ldapServer.importFromLDIF(false, classpathResource("/groups-admins.ldif"));
        ldapServer.importFromLDIF(false, classpathResource("/groups-managers.ldif"));

        return ldapServer;
    }

    @AfterClass
    public static void tearDown() {
        ldapServer.shutDown(true);
    }

    @Test
    public void searchForAliceWithMatchingCert() throws Exception {
        LdapSearchService service = new LdapSearchServiceImpl(getServerConfig(ldapServer), getSearchConfig());
        SearchResultEntry result = service.searchPeople(getPersonCertificate("alice"));

        assertNotNull(result);
        assertEquals("cn=Alice Smith,ou=people,dc=test,dc=visallo,dc=org", result.getDN());
        assertEquals("Alice Smith-Y-", result.getAttributeValue("displayName"));
        assertEquals("3", result.getAttributeValue("employeeNumber"));
        assertArrayEquals(getPersonCertificate("alice").getEncoded(), result.getAttributeValueBytes("userCertificate;binary"));

        Set<String> groups = service.searchGroups(result);
        assertNotNull(groups);
        assertEquals(2, groups.size());
        assertTrue(groups.contains("admins"));
        assertTrue(groups.contains("managers"));

        System.out.println("groups = " + groups);
        System.out.println(result.toLDIFString());
    }

    @Test
    public void searchForBob() throws Exception {
        LdapSearchService service = new LdapSearchServiceImpl(getServerConfig(ldapServer), getSearchConfig());
        SearchResultEntry result = service.searchPeople(getPersonCertificate("bob"));

        assertNotNull(result);
        assertEquals("cn=Bob Maluga,ou=people,dc=test,dc=visallo,dc=org", result.getDN());
        assertEquals("Bob Maluga-Y-", result.getAttributeValue("displayName"));
        assertEquals("4", result.getAttributeValue("employeeNumber"));

        Set<String> groups = service.searchGroups(result);
        assertNotNull(groups);
        assertEquals(1, groups.size());
        assertTrue(groups.contains("admins"));

        System.out.println("groups = " + groups);
        System.out.println(result.toLDIFString());
    }

    @Test(expected = VisalloException.class)
    public void searchForNonExistentPerson() throws Exception {
        LdapSearchService service = new LdapSearchServiceImpl(getServerConfig(ldapServer), getSearchConfig());
        service.searchPeople(getPersonCertificate("diane"));
    }

    private static String classpathResource(String name) {
        return LdapSearchServiceTest.class.getResource(name).getPath();
    }

    public static LdapServerConfiguration getServerConfig(InMemoryDirectoryServer ldapServer) throws LDAPException {
        LdapServerConfiguration serverConfig = new LdapServerConfiguration();
        serverConfig.setPrimaryLdapServerHostname(ldapServer.getConnection().getConnectedAddress());
        serverConfig.setPrimaryLdapServerPort(ldapServer.getListenPort("LDAPS"));
        serverConfig.setConnectionType("LDAPS");
        serverConfig.setMaxConnections(1);
        serverConfig.setBindDn(BIND_DN);
        serverConfig.setBindPassword(BIND_PASSWORD);
        serverConfig.setTrustStore(classpathResource("/truststore.jks"));
        serverConfig.setTrustStorePassword("password");
        return serverConfig;
    }

    public static LdapSearchConfiguration getSearchConfig() {
        LdapSearchConfiguration searchConfig = new LdapSearchConfiguration();
        searchConfig.setUserSearchBase("dc=test,dc=visallo,dc=org");
        searchConfig.setUserSearchScope("sub");
        searchConfig.setUserAttributes("displayName,employeeNumber,telephoneNumber");
        searchConfig.setUserCertificateAttribute("userCertificate;binary");
        searchConfig.setGroupSearchBase("dc=test,dc=visallo,dc=org");
        searchConfig.setGroupSearchScope("sub");
        searchConfig.setUserSearchFilter("(cn=${cn})");
        searchConfig.setGroupNameAttribute("cn");
        searchConfig.setGroupSearchBase("ou=groups,dc=test,dc=visallo,dc=org");
        searchConfig.setGroupSearchFilter("(uniqueMember=${dn})");
        searchConfig.setGroupSearchScope("sub");
        return searchConfig;
    }

    public static X509Certificate getPersonCertificate(String personName) throws IOException, LDIFException, CertificateException {
        LDIFReader reader = new LDIFReader(classpathResource("/people-" + personName + ".ldif"));
        Attribute certAttr = reader.readEntry().getAttribute(getSearchConfig().getUserCertificateAttribute());
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        return (X509Certificate) certificateFactory.generateCertificate(new ByteArrayInputStream(certAttr.getValueByteArray()));
    }
}
