package org.visallo.externalResource.yarn;

import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.yarn.ApplicationMasterBase;

public class ExternalResourceWorkerYarnApplicationMaster extends ApplicationMasterBase {
    public static void main(String[] args) throws Exception {
        VisalloLoggerFactory.setProcessType("erw-app");
        new ExternalResourceWorkerYarnApplicationMaster().run(args);
    }

    @Override
    protected Class getTaskClass() {
        return ExternalResourceWorkerYarnTask.class;
    }
}
