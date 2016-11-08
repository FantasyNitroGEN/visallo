package org.visallo.web.initializers;

import com.google.inject.Inject;
import org.visallo.core.config.Configuration;
import org.visallo.core.ingest.graphProperty.GraphPropertyRunner;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.user.User;
import org.visallo.core.util.*;

import javax.servlet.ServletContext;
import java.util.ArrayList;
import java.util.List;

public class GraphPropertyWorkerRunnerInitializer extends ApplicationBootstrapInitializer implements ShutdownListener {
    private static final String CONFIG_THREAD_COUNT = GraphPropertyWorkerRunnerInitializer.class.getName() + ".threadCount";
    private static final int DEFAULT_THREAD_COUNT = 1;
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(GraphPropertyWorkerRunnerInitializer.class);

    private final Configuration config;
    private final UserRepository userRepository;
    private final List<StoppableRunnable> stoppables = new ArrayList<>();

    @Inject
    public GraphPropertyWorkerRunnerInitializer(
            Configuration config,
            UserRepository userRepository,
            ShutdownService shutdownService
    ) {
        this.config = config;
        this.userRepository = userRepository;
        shutdownService.register(this);
    }

    @Override
    public void initialize(ServletContext context) {
        LOGGER.debug("Starting GraphPropertyWorkerRunnerInitializer");

        int threadCount = config.getInt(CONFIG_THREAD_COUNT, DEFAULT_THREAD_COUNT);
        User user = userRepository.getSystemUser();

        this.stoppables.addAll(GraphPropertyRunner.startThreaded(threadCount, user));
    }

    @Override
    public void shutdown() {
        stoppables.forEach(StoppableRunnable::stop);
    }
}
