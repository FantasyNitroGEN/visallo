package org.visallo.externalResource.yarn;

import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.yarn.ClientBase;

public class ExternalResourceWorkerYarnClient extends ClientBase {
    public static final String APP_NAME = "external-resource-worker";

    public static void main(String[] args) throws Exception {
        VisalloLoggerFactory.setProcessType("erw-client");
        new ExternalResourceWorkerYarnClient().run(args);
    }

    @Override
    protected String getAppName() {
        return APP_NAME;
    }

    @Override
    protected Class getApplicationMasterClass() {
        return ExternalResourceWorkerYarnApplicationMaster.class;
    }
}
