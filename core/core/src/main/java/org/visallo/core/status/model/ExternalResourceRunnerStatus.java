package org.visallo.core.status.model;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("externalResourceRunner")
public class ExternalResourceRunnerStatus extends WorkerRunnerStatus {
    @JsonTypeName("externalResourceWorkerStatus")
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
