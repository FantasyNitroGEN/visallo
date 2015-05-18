package org.visallo.externalResource.yarn;

import org.visallo.core.externalResource.ExternalResourceRunner;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.yarn.TaskBase;

public class ExternalResourceWorkerYarnTask extends TaskBase {
    public static void main(String[] args) {
        VisalloLoggerFactory.setProcessType("erw-task");
        new ExternalResourceWorkerYarnTask().run(args);
    }

    public void run() {
        new ExternalResourceRunner(getConfiguration(), getUser()).startAllAndWait();
    }
}
