package org.visallo.web.initializers;

import org.visallo.core.bootstrap.InjectHelper;
import org.visallo.core.ingest.graphProperty.GraphPropertyRunner;
import org.visallo.core.user.User;
import org.visallo.core.util.StoppableRunnable;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

public class GraphPropertyWorkerRunnerHelper implements StoppableRunnable {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(GraphPropertyWorkerRunnerHelper.class);

    private GraphPropertyRunner graphPropertyRunner = null;
    private User user;

    public GraphPropertyWorkerRunnerHelper(User user){
        this.user = user;
    }

    @Override
    public void stop() {
        if (graphPropertyRunner != null) {
            LOGGER.debug("Stopping GraphPropertyWorkerRunnerHelper...");
            graphPropertyRunner.stop();
            LOGGER.debug("Stopped GraphPropertyWorkerRunnerHelper");
        }
    }

    @Override
    public void run() {
        graphPropertyRunner = InjectHelper.getInstance(GraphPropertyRunner.class);
        graphPropertyRunner.prepare(user);
        try {
            graphPropertyRunner.run();
        } catch (Exception ex) {
            LOGGER.error("Failed running graph property runner. " + ex.getMessage());
        }

    }
}
