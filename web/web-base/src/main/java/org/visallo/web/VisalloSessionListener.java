package org.visallo.web;

import org.visallo.core.bootstrap.InjectHelper;
import org.visallo.core.config.Configuration;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

public class VisalloSessionListener implements HttpSessionListener {
    private Integer maxSessionInactiveIntervalSeconds;
    private Configuration configuration;

    @Override
    public void sessionCreated(HttpSessionEvent event) {
        HttpSession session = event.getSession();
        if (session != null) {
            session.setMaxInactiveInterval(getMaxSessionInactiveIntervalSeconds());
        }
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent event) {

    }

    public int getMaxSessionInactiveIntervalSeconds() {
        if (maxSessionInactiveIntervalSeconds == null) {
            maxSessionInactiveIntervalSeconds = getConfiguration().getInt(
                    WebConfiguration.MAX_SESSION_INACTIVE_INTERVAL_SECONDS
            );
            // Logger needs to be late bound because this class is created very early in the lifecycle
            VisalloLogger logger = VisalloLoggerFactory.getLogger(VisalloSessionListener.class);
            logger.info("Session timeout set to %d seconds", maxSessionInactiveIntervalSeconds);
        }
        return maxSessionInactiveIntervalSeconds;
    }

    public Configuration getConfiguration() {
        if (configuration == null) {
            configuration = InjectHelper.getInstance(Configuration.class);
        }
        return configuration;
    }
}
