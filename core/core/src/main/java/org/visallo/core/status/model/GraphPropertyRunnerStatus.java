package org.visallo.core.status.model;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("graphPropertyRunner")
public class GraphPropertyRunnerStatus extends WorkerRunnerStatus {
    @JsonTypeName("graphPropertyWorkerStatus")
    public static class GraphPropertyWorkerStatus extends WorkerStatus {

    }
}
