package org.visallo.web.initializers;

import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

public abstract class ApplicationBootstrapInitializer {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(ApplicationBootstrapInitializer.class);

    public abstract void initialize();

    /**
     * Delay the start of GPW and long running processes so the web app comes up faster
     */
    protected void delayStart() {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            LOGGER.error("Could not sleep", e);
        }
    }
}
