package org.visallo.core.util;

import com.google.common.collect.Lists;
import com.google.inject.Singleton;
import com.v5analytics.simpleorm.SimpleOrmSession;
import org.vertexium.Graph;
import org.visallo.core.bootstrap.InjectHelper;
import org.visallo.core.bootstrap.VisalloBootstrap;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Singleton
public class ShutdownService {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(ShutdownService.class);
    private LinkedHashSet<ShutdownListener> shutdownListeners = new LinkedHashSet<>();

    public void shutdown() {
        // shutdown in reverse order to better handle dependencies
        List<ShutdownListener> shutdownListenersList = Lists.reverse(new ArrayList<>(shutdownListeners));
        for (ShutdownListener shutdownListener : shutdownListenersList) {
            try {
                LOGGER.info("Shutdown: " + shutdownListener.getClass().getName());
                shutdownListener.shutdown();
            } catch (Exception e) {
                LOGGER.error("Unable to shutdown: " + shutdownListener.getClass().getName(), e);
            }
        }

        LOGGER.info("Shutdown: InjectHelper");
        InjectHelper.shutdown();

        LOGGER.info("Shutdown: VisalloBootstrap");
        VisalloBootstrap.shutdown();
    }

    /**
     * Classes that implement {@link ShutdownListener} call this method to be notified of
     * a Visallo shutdown. We can not use the service locator pattern to find shutdown listeners because that
     * may cause an inadvertent initialization of that class.
     */
    public void register(ShutdownListener shutdownListener) {
        this.shutdownListeners.add(shutdownListener);
    }
}
