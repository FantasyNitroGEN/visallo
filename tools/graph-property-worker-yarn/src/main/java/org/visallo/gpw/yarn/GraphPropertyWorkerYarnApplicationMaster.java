package org.visallo.gpw.yarn;

import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.yarn.ApplicationMasterBase;

public class GraphPropertyWorkerYarnApplicationMaster extends ApplicationMasterBase {
    public static void main(String[] args) throws Exception {
        VisalloLoggerFactory.setProcessType("gpw-app");
        new GraphPropertyWorkerYarnApplicationMaster().run(args);
    }

    @Override
    protected Class getTaskClass() {
        return GraphPropertyWorkerYarnTask.class;
    }
}
