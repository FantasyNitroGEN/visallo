package org.visallo.core.status.model;

import java.util.HashMap;
import java.util.Map;

public class QueueStatus extends Status {
    private Map<String, Metric> metrics = new HashMap<>();

    public Map<String, Metric> getMetrics() {
        return metrics;
    }

    public QueueStatus(int messages) {
        CounterMetric counterMetric = new CounterMetric();
        counterMetric.setCount(messages);
        metrics.put("messages", counterMetric);
    }
}
