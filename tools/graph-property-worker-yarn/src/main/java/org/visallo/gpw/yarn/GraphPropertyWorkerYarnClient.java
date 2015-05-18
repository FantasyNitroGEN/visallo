package org.visallo.gpw.yarn;

import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.yarn.ClientBase;

public class GraphPropertyWorkerYarnClient extends ClientBase {
    public static final String APP_NAME = "graph-property-worker";

    public static void main(String[] args) throws Exception {
        VisalloLoggerFactory.setProcessType("gpw-client");
        new GraphPropertyWorkerYarnClient().run(args);
    }

    @Override
    protected String getAppName() {
        return APP_NAME;
    }

    @Override
    protected Class getApplicationMasterClass() {
        return GraphPropertyWorkerYarnApplicationMaster.class;
    }
}
