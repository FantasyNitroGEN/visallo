package org.visallo.web.initializers;

import com.google.inject.Inject;
import org.visallo.core.bootstrap.InjectHelper;
import org.visallo.core.config.Configuration;
import org.visallo.core.model.longRunningProcess.LongRunningProcessRunner;
import org.visallo.core.util.StoppableRunnable;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

public class LongRunningProcessRunnerHelper implements StoppableRunnable {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(LongRunningProcessRunnerHelper.class);

    private Configuration config;
    private LongRunningProcessRunner longRunningProcessRunner = null;

    @Inject
    public LongRunningProcessRunnerHelper(Configuration config) {
        this.config = config;
    }

    @Override
    public void stop() {
        if (longRunningProcessRunner != null){
            LOGGER.debug("Stopping LongRunningProcessRunner...");
            longRunningProcessRunner.stop();
            LOGGER.debug("Stopped LongRunningProcessRunner");
        }
    }

    @Override
    public void run() {
        longRunningProcessRunner = InjectHelper.getInstance(LongRunningProcessRunner.class);
        longRunningProcessRunner.prepare(config.toMap());
        try {
            longRunningProcessRunner.run();
        } catch (Exception ex) {
            LOGGER.error("Failed running long running process runner. " + ex.getMessage());
        }
    }
}
