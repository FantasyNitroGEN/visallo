package org.visallo.core.status.model;

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

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "type")
    public static abstract class WorkerStatus extends Status {
        private Map<String, Metric> metrics = new HashMap<>();

        public Map<String, Metric> getMetrics() {
            return metrics;
        }
    }
}
