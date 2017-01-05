package org.visallo.core.status.model;

public class ExternalResourceRunnerStatus extends WorkerRunnerStatus {
    public static class ExternalResourceWorkerStatus extends WorkerStatus {
        private String threadName;

        public void setThreadName(String threadName) {
            this.threadName = threadName;
        }

        public String getThreadName() {
            return threadName;
        }
    }
}
