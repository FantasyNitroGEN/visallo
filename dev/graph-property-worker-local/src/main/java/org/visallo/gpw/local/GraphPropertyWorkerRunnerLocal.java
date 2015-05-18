package org.visallo.gpw.local;

import org.visallo.core.bootstrap.InjectHelper;
import org.visallo.core.cmdline.CommandLineTool;
import org.visallo.core.ingest.graphProperty.GraphPropertyRunner;

public class GraphPropertyWorkerRunnerLocal extends CommandLineTool {
    public static void main(String[] args) throws Exception {
        CommandLineTool.main(new GraphPropertyWorkerRunnerLocal(), args);
    }

    @Override
    protected int run() throws Exception {
        GraphPropertyRunner graphPropertyRunner = prepareGraphPropertyRunner();
        graphPropertyRunner.run();
        return 0;
    }

    private GraphPropertyRunner prepareGraphPropertyRunner() {
        GraphPropertyRunner graphPropertyRunner = InjectHelper.getInstance(GraphPropertyRunner.class);
        graphPropertyRunner.prepare(getUser());
        return graphPropertyRunner;
    }
}
