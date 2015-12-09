package org.visallo.core.bootstrap;

import com.google.inject.Binder;
import com.google.inject.Provider;
import com.google.inject.Scopes;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

public class ZookeeperBootstrapBindingProvider implements BootstrapBindingProvider {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(ZookeeperBootstrapBindingProvider.class);

    @Override
    public void addBindings(Binder binder, Configuration configuration) {
        LOGGER.debug("binding %s", CuratorFrameworkProvider.class.getName());
        binder.bind(CuratorFramework.class)
                .toProvider(new CuratorFrameworkProvider(configuration))
                .in(Scopes.SINGLETON);
    }

    private static class CuratorFrameworkProvider implements Provider<CuratorFramework> {
        private String zookeeperConnectionString;
        private RetryPolicy retryPolicy;

        public CuratorFrameworkProvider(Configuration configuration) {
            zookeeperConnectionString = configuration.get(Configuration.ZK_SERVERS, null);
            if (zookeeperConnectionString == null) {
                throw new VisalloException("Could not find configuration item: " + Configuration.ZK_SERVERS);
            }
            retryPolicy = new ExponentialBackoffRetry(1000, 6);
        }

        @Override
        public CuratorFramework get() {
            CuratorFramework client = CuratorFrameworkFactory.newClient(zookeeperConnectionString, retryPolicy);
            client.getConnectionStateListenable().addListener(new ConnectionStateListener() {
                @Override
                public void stateChanged(CuratorFramework client, ConnectionState newState) {
                    LOGGER.debug("curator connection state changed to " + newState.name());
                }
            });
            client.start();
            return client;
        }
    }
}
