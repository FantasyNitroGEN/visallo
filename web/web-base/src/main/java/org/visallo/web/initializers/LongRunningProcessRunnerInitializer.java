package org.visallo.web.initializers;

import com.google.inject.Inject;
import org.visallo.core.config.Configuration;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LongRunningProcessRunnerInitializer extends ApplicationBootstrapInitializer {
    public static final String CONFIG_THREAD_COUNT = LongRunningProcessRunnerInitializer.class.getName() + ".threadCount";
    public static final int DEFAULT_THREAD_COUNT = 1;
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(LongRunningProcessRunnerInitializer.class);
    private final Configuration config;
    private List<LongRunningProcessRunnerHelper> runnerHelpers = new ArrayList<>();

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
            LongRunningProcessRunnerHelper runnerHelper = new LongRunningProcessRunnerHelper(config);
            runnerHelpers.add(runnerHelper);
            Thread t = new Thread(runnerHelper);
            t.setName("long-running-process-runner-" + t.getId());
            t.setDaemon(true);
            LOGGER.debug("starting long running process runner thread: %s", t.getName());
            t.start();
        }
    }

    @Override
    public void close() throws IOException {
        for (LongRunningProcessRunnerHelper runnerHelper : runnerHelpers) {
            runnerHelper.stop();
        }
    }
}
