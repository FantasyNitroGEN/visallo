package org.visallo.gpw.yarn;

import org.visallo.core.bootstrap.InjectHelper;
import org.visallo.core.ingest.graphProperty.GraphPropertyRunner;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.yarn.TaskBase;

public class GraphPropertyWorkerYarnTask extends TaskBase {
    public static void main(String[] args) {
        VisalloLoggerFactory.setProcessType("gpw-task");
        new GraphPropertyWorkerYarnTask().run(args);
    }

    public void run() throws Exception {
        GraphPropertyRunner graphPropertyRunner = InjectHelper.getInstance(GraphPropertyRunner.class);
        graphPropertyRunner.prepare(getUser());
        graphPropertyRunner.run();
    }
}
