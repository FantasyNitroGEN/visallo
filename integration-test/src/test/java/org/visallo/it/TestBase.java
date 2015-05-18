package org.visallo.it;

import com.google.common.base.Throwables;
import org.visallo.core.config.VisalloTestClusterConfigurationLoader;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.test.VisalloTestCluster;
import org.visallo.web.clientapi.VisalloApi;
import org.visallo.web.clientapi.UserNameOnlyVisalloApi;
import org.visallo.web.clientapi.codegen.ApiException;
import org.visallo.web.clientapi.model.ClientApiProperty;
import org.visallo.web.clientapi.model.ClientApiWorkspaceDiff;
import org.visallo.web.clientapi.model.ClientApiWorkspacePublishResponse;
import org.visallo.web.clientapi.model.ClientApiWorkspaceUndoResponse;
import org.visallo.web.clientapi.util.ObjectMapperFactory;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class TestBase {
    protected VisalloLogger LOGGER;
    protected VisalloTestCluster visalloTestCluster;
    protected int httpPort;
    protected int httpsPort;
    protected static final String USERNAME_TEST_USER_1 = "testUser1";
    protected static final String USERNAME_TEST_USER_2 = "testUser2";
    protected static final String USERNAME_TEST_USER_3 = "testUser3";
    public static final String TEST_MULTI_VALUE_KEY = TestBase.class.getName();

    @Before
    public void before() throws ApiException, IOException, NoSuchAlgorithmException, KeyManagementException, InterruptedException {
        VisalloTestClusterConfigurationLoader.set("repository.ontology.owl.1.iri", "http://visallo.org/test");
        VisalloTestClusterConfigurationLoader.set("repository.ontology.owl.1.dir", new File(VisalloTestCluster.getVisalloRootDir(), "integration-test/src/test/resources/org/visallo/it/").getAbsolutePath());

        disableSSLCertChecking();
        initVisalloTestCluster();
        LOGGER = VisalloLoggerFactory.getLogger(this.getClass());
    }

    public void initVisalloTestCluster() {
        httpPort = findOpenPort(10080);
        httpsPort = findOpenPort(10443);
        visalloTestCluster = new VisalloTestCluster(httpPort, httpsPort);
        visalloTestCluster.startup();
    }

    private int findOpenPort(int startingPort) {
        for (int port = startingPort; port < 65535; port++) {
            try {
                ServerSocket socket = new ServerSocket(port);
                socket.close();
                return port;
            } catch (IOException ex) {
                // try next port
            }
        }
        throw new RuntimeException("No free ports found");
    }

    public void disableSSLCertChecking() throws NoSuchAlgorithmException, KeyManagementException {
        HttpsURLConnection.setDefaultHostnameVerifier(new javax.net.ssl.HostnameVerifier() {
            public boolean verify(String hostname, javax.net.ssl.SSLSession sslSession) {
                return hostname.equals("localhost");
            }
        });

        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }

            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
        }};

        SSLContext sc = SSLContext.getInstance("TLS");
        sc.init(null, trustAllCerts, new SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
    }

    @After
    public void after() {
        visalloTestCluster.shutdown();
    }

    public void addUserAuths(VisalloApi visalloApi, String username, String... authorizations) throws ApiException {
        for (String auth : authorizations) {
            Map<String, String> queryParameters = new HashMap<>();
            queryParameters.put("user-name", username);
            queryParameters.put("auth", auth);
            visalloApi.invokeAPI("/user/auth/add", "POST", queryParameters, null, null, null, null);
        }
    }

    VisalloApi login(String username) throws ApiException {
        UserNameOnlyVisalloApi visalloApi = new UserNameOnlyVisalloApi("https://localhost:" + httpsPort, username);
        visalloApi.loginAndGetCurrentWorkspace();
        return visalloApi;
    }

    protected void assertHasProperty(Iterable<ClientApiProperty> properties, String propertyKey, String propertyName) {
        ClientApiProperty property = getProperty(properties, propertyKey, propertyName);
        assertNotNull("could not find property " + propertyKey + ":" + propertyName, property);
    }

    protected ClientApiProperty getProperty(Iterable<ClientApiProperty> properties, String propertyKey, String propertyName) {
        for (ClientApiProperty property : properties) {
            if (propertyKey.equals(property.getKey()) && propertyName.equals(property.getName())) {
                return property;
            }
        }
        return null;
    }

    protected List<ClientApiProperty> getProperties(Iterable<ClientApiProperty> properties, String propertyKey, String propertyName) {
        List<ClientApiProperty> propertyList = new ArrayList<>();
        for (ClientApiProperty property : properties) {
            if (propertyKey.equals(property.getKey()) && propertyName.equals(property.getName())) {
                propertyList.add(property);
            }
        }
        return propertyList;
    }

    protected void assertHasProperty(Iterable<ClientApiProperty> properties, String propertyKey, String propertyName, Object expectedValue) {
        ClientApiProperty property = getProperty(properties, propertyKey, propertyName);
        assertNotNull("could not find property " + propertyKey + ":" + propertyName, property);
        Object value = property.getValue();
        if (value instanceof Map) {
            try {
                value = ObjectMapperFactory.getInstance().writeValueAsString(value);
                expectedValue = ObjectMapperFactory.getInstance().writeValueAsString(expectedValue);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        assertEquals("property value does not match for property " + propertyKey + ":" + propertyName, expectedValue, value);
    }

    protected void assertPublishAll(VisalloApi visalloApi, int expectedDiffsBeforePublish) throws ApiException {
        ClientApiWorkspaceDiff diff = visalloApi.getWorkspaceApi().getDiff();
        LOGGER.info("diff before publish: %s", diff.toString());
        assertEquals(expectedDiffsBeforePublish, diff.getDiffs().size());
        ClientApiWorkspacePublishResponse publishAllResult = visalloApi.getWorkspaceApi().publishAll(diff.getDiffs());
        LOGGER.info("publish all results: %s", publishAllResult.toString());
        assertTrue("publish all failed: " + publishAllResult, publishAllResult.isSuccess());
        assertEquals("publish all expected 0 failures: " + publishAllResult, 0, publishAllResult.getFailures().size());

        diff = visalloApi.getWorkspaceApi().getDiff();
        LOGGER.info("diff after publish: %s", diff.toString());
        assertEquals(0, diff.getDiffs().size());
    }

    protected void assertUndoAll(VisalloApi visalloApi, int expectedDiffsBeforeUndo) throws ApiException {
        ClientApiWorkspaceDiff diff = visalloApi.getWorkspaceApi().getDiff();
        LOGGER.info("diff before undo: %s", diff.toString());
        assertEquals(expectedDiffsBeforeUndo, diff.getDiffs().size());
        ClientApiWorkspaceUndoResponse undoAllResult = visalloApi.getWorkspaceApi().undoAll(diff.getDiffs());
        LOGGER.info("undo all results: %s", undoAllResult.toString());
        assertTrue("undo all failed: " + undoAllResult, undoAllResult.isSuccess());
        assertEquals("undo all expected 0 failures: " + undoAllResult, 0, undoAllResult.getFailures().size());

        diff = visalloApi.getWorkspaceApi().getDiff();
        LOGGER.info("diff after undo: %s", diff.toString());
        assertEquals(0, diff.getDiffs().size());
    }

    protected static String getResourceString(String resourceName) {
        try {
            try (InputStream resource = TestBase.class.getResourceAsStream(resourceName)) {
                if (resource == null) {
                    throw new RuntimeException("Could not find resource: " + resourceName);
                }
                return IOUtils.toString(resource);
            }
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }
}
