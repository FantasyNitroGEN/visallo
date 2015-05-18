package org.visallo.web.session;

import com.google.inject.Provider;
import com.v5analytics.simpleorm.SimpleOrmJettySessionManager;
import com.v5analytics.simpleorm.SimpleOrmSession;
import org.visallo.core.bootstrap.VisalloBootstrap;
import org.visallo.core.config.Configuration;
import org.visallo.core.config.ConfigurationLoader;

public class VisalloSimpleOrmJettySessionManager extends SimpleOrmJettySessionManager {
    public VisalloSimpleOrmJettySessionManager() {
        super(createSession());
    }

    private static SimpleOrmSession createSession() {
        Configuration configuration = ConfigurationLoader.load();
        Provider<? extends SimpleOrmSession> simpleOrmSessionProvider = VisalloBootstrap.getConfigurableProvider(configuration, Configuration.SIMPLE_ORM_SESSION);
        return simpleOrmSessionProvider.get();
    }
}
