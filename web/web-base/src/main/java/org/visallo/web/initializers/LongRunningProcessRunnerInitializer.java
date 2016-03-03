package org.visallo.web.initializers;

import com.google.inject.Inject;
import org.visallo.core.bootstrap.InjectHelper;
import org.visallo.core.config.Configuration;
import org.visallo.core.model.longRunningProcess.LongRunningProcessRunner;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

public class LongRunningProcessRunnerInitializer extends ApplicationBootstrapInitializer {
    public static final String CONFIG_THREAD_COUNT = LongRunningProcessRunnerInitializer.class.getName() + ".threadCount";
    public static final int DEFAULT_THREAD_COUNT = 1;
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(LongRunningProcessRunnerInitializer.class);
    private final Configuration config;

    @Inject
    public LongRunningProcessRunnerInitializer(Configuration config) {
        this.config = config;
    }

    @Override
    public void initialize() {
        LOGGER.debug("setupLongRunningProcessRunner");

        int threadCount = config.getInt(CONFIG_THREAD_COUNT, DEFAULT_THREAD_COUNT);

        LOGGER.debug("long running process runners: %d", threadCount);
        for (int i = 0; i < threadCount; i++) {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    delayStart();
                    final LongRunningProcessRunner longRunningProcessRunner = InjectHelper.getInstance(LongRunningProcessRunner.class);
                    longRunningProcessRunner.prepare(config.toMap());
                    try {
                        longRunningProcessRunner.run();
                    } catch (Exception ex) {
                        LOGGER.error("Failed running long running process runner", ex);
                    }
                }
            });
            t.setName("long-running-process-runner-" + t.getId());
            t.setDaemon(true);
            LOGGER.debug("starting long running process runner thread: %s", t.getName());
            t.start();
        }
    }
}
