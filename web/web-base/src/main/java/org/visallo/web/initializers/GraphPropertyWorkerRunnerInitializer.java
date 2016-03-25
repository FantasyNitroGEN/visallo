package org.visallo.web.initializers;

import com.google.inject.Inject;
import org.visallo.core.bootstrap.InjectHelper;
import org.visallo.core.config.Configuration;
import org.visallo.core.ingest.graphProperty.GraphPropertyRunner;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.user.User;
import org.visallo.core.util.StoppableRunnable;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

import javax.servlet.ServletContext;
import java.util.ArrayList;
import java.util.List;

public class GraphPropertyWorkerRunnerInitializer extends ApplicationBootstrapInitializer {
    private static final String CONFIG_THREAD_COUNT = GraphPropertyWorkerRunnerInitializer.class.getName() + ".threadCount";
    private static final int DEFAULT_THREAD_COUNT = 1;
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(GraphPropertyWorkerRunnerInitializer.class);

    private final Configuration config;
    private final UserRepository userRepository;
    private final List<StoppableRunnable> stoppables = new ArrayList<>();

    @Inject
    public GraphPropertyWorkerRunnerInitializer(
            Configuration config,
            UserRepository userRepository
    ) {
        this.config = config;
        this.userRepository = userRepository;
    }

    @Override
    public void initialize(ServletContext context) {
        LOGGER.debug("Starting GraphPropertyWorkerRunnerInitializer");

        int threadCount = config.getInt(CONFIG_THREAD_COUNT, DEFAULT_THREAD_COUNT);
        User user = userRepository.getSystemUser();

        LOGGER.debug("Starting GraphPropertyRunners on %d threads", threadCount);
        for (int i = 0; i < threadCount; i++) {
            StoppableRunnable stoppable = new StoppableRunnable() {
                private GraphPropertyRunner graphPropertyRunner = null;

                @Override
                public void run() {
                    try {
                        graphPropertyRunner = InjectHelper.getInstance(GraphPropertyRunner.class);
                        graphPropertyRunner.prepare(user);
                        graphPropertyRunner.run();
                    } catch (Exception ex) {
                        LOGGER.error("Failed running GraphPropertyRunner", ex);
                    }
                }

                @Override
                public void stop() {
                    try {
                        if (graphPropertyRunner != null) {
                            LOGGER.debug("Stopping GraphPropertyRunner");
                            graphPropertyRunner.stop();
                        }
                    } catch (Exception ex) {
                        LOGGER.error("Failed stopping GraphPropertyRunner", ex);
                    }
                }
            };
            stoppables.add(stoppable);
            Thread t = new Thread(stoppable);
            t.setName("graph-property-runner-" + t.getId());
            t.setDaemon(true);
            LOGGER.debug("Starting GraphPropertyRunner thread: %s", t.getName());
            t.start();
        }
    }

    @Override
    public void close() {
        stoppables.forEach(StoppableRunnable::stop);
    }
}
