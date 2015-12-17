package org.visallo.model.directory;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldif.LDIFException;
import com.unboundid.ldif.LDIFReader;
import com.unboundid.util.ssl.KeyStoreKeyManager;
import com.unboundid.util.ssl.SSLUtil;
import com.unboundid.util.ssl.TrustAllTrustManager;
import com.unboundid.util.ssl.TrustStoreTrustManager;
import org.visallo.core.exception.VisalloException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class LdapTestHelpers {
    public static final String BIND_DN = "cn=root,dc=test,dc=visallo,dc=org";
    public static final String BIND_PASSWORD = "visallo";

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

    public static LdapServerConfiguration getServerConfig(InMemoryDirectoryServer ldapServer) throws LDAPException {
        LdapServerConfiguration serverConfig = new LdapServerConfiguration();
        serverConfig.setPrimaryLdapServerHostname(ldapServer.getConnection().getConnectedAddress());
        serverConfig.setPrimaryLdapServerPort(ldapServer.getListenPort("LDAPS"));
        serverConfig.setConnectionType("LDAPS");
        serverConfig.setMaxConnections(1);
        serverConfig.setBindDn(LdapTestHelpers.BIND_DN);
        serverConfig.setBindPassword(LdapTestHelpers.BIND_PASSWORD);
        serverConfig.setTrustStore(LdapTestHelpers.classpathResource("/truststore.jks"));
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

    public static LdapSearchConfiguration getSearchConfigForGroups() {
        LdapSearchConfiguration config = getSearchConfig();
        config.setGroupSearchFilter("(cn=${cn})");
        return config;
    }

    public static X509Certificate getPersonCertificate(String personName) throws IOException, LDIFException, CertificateException {
        LDIFReader reader = new LDIFReader(LdapTestHelpers.classpathResource("/people-" + personName + ".ldif"));
        Attribute certAttr = reader.readEntry().getAttribute(getSearchConfig().getUserCertificateAttribute());
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        return (X509Certificate) certificateFactory.generateCertificate(new ByteArrayInputStream(certAttr.getValueByteArray()));
    }

    public static String classpathResource(String name) {
        URL resource = LdapSearchServiceTest.class.getResource(name);
        if (resource == null) {
            throw new VisalloException("Could not find resource: " + name);
        }
        return resource.getPath();
    }
}
