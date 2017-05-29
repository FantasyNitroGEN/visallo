package org.visallo.web.ingest.cloud.s3;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.google.inject.Inject;
import org.json.JSONObject;
import org.visallo.core.bootstrap.InjectHelper;
import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloException;
import org.visallo.web.ingest.cloud.s3.authentication.AuthProvider;

import java.util.Collection;

public class AmazonS3ClientFactory {
    private static final String PREFIX = "ingest.cloud.s3.";
    private final Configuration configuration;

    @Inject
    public AmazonS3ClientFactory(Configuration configuration) {
        this.configuration = configuration;
    }

    public AmazonS3 getClient(String providerClass, String credentials) {
        JSONObject obj = credentials == null ?
                new JSONObject() :
                new JSONObject(credentials);
        return getClient(providerClass, obj);
    }

    public AmazonS3 getClient(String providerClass, JSONObject credentials) {
        AuthProvider authProvider = getAuthProvider(providerClass);
        ClientConfiguration clientConfiguration = getClientConfiguration();

        return new AmazonS3Client(authProvider.getCredentials(credentials), clientConfiguration);
    }

    private ClientConfiguration getClientConfiguration() {
        ClientConfiguration clientConfiguration = new ClientConfiguration();

        clientConfiguration.setProxyDomain(getConfig("proxy.domain"));
        clientConfiguration.setProxyHost(getConfig("proxy.host"));
        String port = getConfig("proxy.port");
        if (port != null) {
            clientConfiguration.setProxyPort(Integer.parseInt(port));
        }
        clientConfiguration.setProxyUsername(getConfig("proxy.username"));
        clientConfiguration.setProxyPassword(getConfig("proxy.password"));

        return clientConfiguration;
    }

    private AuthProvider getAuthProvider(String className) {
        Collection<AuthProvider> destinations = InjectHelper.getInjectedServices(AuthProvider.class, configuration);
        for (AuthProvider authProvider: destinations) {
            if (authProvider.getClass().getName().equals(className)) {
                return authProvider;
            }
        }

        throw new VisalloException("No AuthProvider found for: " + className);
    }

    private String getConfig(String suffix) {
        return configuration.get(PREFIX + suffix, null);
    }

}
