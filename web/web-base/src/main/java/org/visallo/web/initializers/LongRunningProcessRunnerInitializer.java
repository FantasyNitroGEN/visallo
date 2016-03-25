package org.visallo.web.initializers;

import com.google.inject.Inject;
import org.visallo.core.bootstrap.InjectHelper;
import org.visallo.core.config.Configuration;
import org.visallo.core.model.longRunningProcess.LongRunningProcessRunner;
import org.visallo.core.util.StoppableRunnable;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

import javax.servlet.ServletContext;
import java.util.ArrayList;
import java.util.List;

public class LongRunningProcessRunnerInitializer extends ApplicationBootstrapInitializer {
    private static final String CONFIG_THREAD_COUNT = LongRunningProcessRunnerInitializer.class.getName() + ".threadCount";
    private static final int DEFAULT_THREAD_COUNT = 1;
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(LongRunningProcessRunnerInitializer.class);

    private final Configuration config;
    private final List<StoppableRunnable> stoppables = new ArrayList<>();

    @Inject
    public LongRunningProcessRunnerInitializer(Configuration config) {
        this.config = config;
    }

    @Override
    public void initialize(ServletContext context) {
        LOGGER.debug("Starting LongRunningProcessRunnerInitializer");

        int threadCount = config.getInt(CONFIG_THREAD_COUNT, DEFAULT_THREAD_COUNT);

        LOGGER.debug("Starting LongRunningProcessRunners on %d threads", threadCount);
        for (int i = 0; i < threadCount; i++) {
            StoppableRunnable stoppable = new StoppableRunnable() {
                private LongRunningProcessRunner longRunningProcessRunner = null;

                @Override
                public void run() {
                    try {
                        longRunningProcessRunner = InjectHelper.getInstance(LongRunningProcessRunner.class);
                        longRunningProcessRunner.prepare(config.toMap());
                        longRunningProcessRunner.run();
                    } catch (Exception ex) {
                        LOGGER.error("Failed running LongRunningProcessRunner", ex);
                    }
                }

                @Override
                public void stop() {
                    try {
                        if (longRunningProcessRunner != null){
                            LOGGER.debug("Stopping LongRunningProcessRunner");
                            longRunningProcessRunner.stop();
                        }
                    } catch (Exception ex) {
                        LOGGER.error("Failed stopping LongRunningProcessRunner", ex);
                    }
                }
            };
            stoppables.add(stoppable);
            Thread t = new Thread(stoppable);
            t.setName("long-running-process-runner-" + t.getId());
            t.setDaemon(true);
            LOGGER.debug("Starting LongRunningProcessRunner thread: %s", t.getName());
            t.start();
        }
    }

    @Override
    public void close() {
        stoppables.forEach(StoppableRunnable::stop);
    }
}
