package org.visallo.lrp.yarn;

import org.visallo.core.bootstrap.InjectHelper;
import org.visallo.core.model.longRunningProcess.LongRunningProcessRunner;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.yarn.TaskBase;

public class LongRunningProcessYarnTask extends TaskBase {
    public static void main(String[] args) {
        VisalloLoggerFactory.setProcessType("lrp-task");
        new LongRunningProcessYarnTask().run(args);
    }

    public void run() throws Exception {
        LongRunningProcessRunner longRunningProcessRunner = InjectHelper.getInstance(LongRunningProcessRunner.class, getModuleMaker(), getConfiguration());
        longRunningProcessRunner.prepare(getConfiguration().toMap());
        longRunningProcessRunner.run();
    }
}
