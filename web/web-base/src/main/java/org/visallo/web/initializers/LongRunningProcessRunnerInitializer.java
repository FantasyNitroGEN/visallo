package org.visallo.web.initializers;

import com.google.inject.Inject;
import org.visallo.core.config.Configuration;
import org.visallo.core.model.longRunningProcess.LongRunningProcessRunner;
import org.visallo.core.util.*;

import javax.servlet.ServletContext;
import java.util.ArrayList;
import java.util.List;

public class LongRunningProcessRunnerInitializer extends ApplicationBootstrapInitializer implements ShutdownListener {
    private static final String CONFIG_THREAD_COUNT = LongRunningProcessRunnerInitializer.class.getName() + ".threadCount";
    private static final int DEFAULT_THREAD_COUNT = 1;
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(LongRunningProcessRunnerInitializer.class);

    private final Configuration config;
    private final List<StoppableRunnable> stoppables = new ArrayList<>();

    @Inject
    public LongRunningProcessRunnerInitializer(Configuration config, ShutdownService shutdownService) {
        this.config = config;
        shutdownService.register(this);
    }

    @Override
    public void initialize(ServletContext context) {
        LOGGER.debug("Starting LongRunningProcessRunnerInitializer");

        int threadCount = config.getInt(CONFIG_THREAD_COUNT, DEFAULT_THREAD_COUNT);

        this.stoppables.addAll(LongRunningProcessRunner.startThreaded(threadCount, config));
    }

    @Override
    public void shutdown() {
        stoppables.forEach(StoppableRunnable::stop);
    }
}
