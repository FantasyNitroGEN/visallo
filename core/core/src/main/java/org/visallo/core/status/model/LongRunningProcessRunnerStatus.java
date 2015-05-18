package org.visallo.core.status.model;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("longRunningProcessRunner")
public class LongRunningProcessRunnerStatus extends WorkerRunnerStatus {
    @JsonTypeName("longRunningProcessWorkerStatus")
    public static class LongRunningProcessWorkerStatus extends WorkerStatus {

    }
}
