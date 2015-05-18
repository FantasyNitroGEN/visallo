package org.visallo.core.status.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class WorkerRunnerStatus extends ProcessStatus {
    private List<WorkerStatus> runningWorkers = new ArrayList<>();

    public List<WorkerStatus> getRunningWorkers() {
        return runningWorkers;
    }

    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.PROPERTY,
            property = "type")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = ExternalResourceRunnerStatus.ExternalResourceWorkerStatus.class, name = "externalResourceWorkerStatus"),
            @JsonSubTypes.Type(value = GraphPropertyRunnerStatus.GraphPropertyWorkerStatus.class, name = "graphPropertyWorkerStatus"),
            @JsonSubTypes.Type(value = LongRunningProcessRunnerStatus.LongRunningProcessWorkerStatus.class, name = "longRunningProcessWorkerStatus")
    })
    public static abstract class WorkerStatus extends Status {
        private Map<String, Metric> metrics = new HashMap<>();

        public Map<String, Metric> getMetrics() {
            return metrics;
        }
    }
}
