package org.visallo.lrp.yarn;

import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.yarn.ApplicationMasterBase;

public class LongRunningProcessYarnApplicationMaster extends ApplicationMasterBase {
    public static void main(String[] args) throws Exception {
        VisalloLoggerFactory.setProcessType("lrp-app");
        new LongRunningProcessYarnApplicationMaster().run(args);
    }

    @Override
    protected Class getTaskClass() {
        return LongRunningProcessYarnTask.class;
    }
}
