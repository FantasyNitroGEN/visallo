package org.visallo.lrp.yarn;

import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.yarn.ClientBase;

public class LongRunningProcessYarnClient extends ClientBase {
    public static final String APP_NAME = "long-running-process";

    public static void main(String[] args) throws Exception {
        VisalloLoggerFactory.setProcessType("lrp-client");
        new LongRunningProcessYarnClient().run(args);
    }

    @Override
    protected String getAppName() {
        return APP_NAME;
    }

    @Override
    protected Class getApplicationMasterClass() {
        return LongRunningProcessYarnApplicationMaster.class;
    }
}
