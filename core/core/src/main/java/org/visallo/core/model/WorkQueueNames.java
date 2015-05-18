package org.visallo.core.model;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.visallo.core.config.Configuration;

@Singleton
public class WorkQueueNames {
    private static final String GRAPH_PROPERTY_QUEUE_NAME = "graphProperty";
    private static final String LONG_RUNNING_PROCESS_QUEUE_NAME = "longRunningProcess";

    private final String graphPropertyQueueName;
    private final String longRunningProcessQueueName;

    @Inject
    public WorkQueueNames(Configuration configuration) {
        String prefix = configuration.get(Configuration.QUEUE_PREFIX, null);
        prefix = prefix == null ? "" : prefix + "-";
        graphPropertyQueueName = prefix + GRAPH_PROPERTY_QUEUE_NAME;
        longRunningProcessQueueName = prefix + LONG_RUNNING_PROCESS_QUEUE_NAME;
    }

    public String getGraphPropertyQueueName() {
        return graphPropertyQueueName;
    }

    public String getLongRunningProcessQueueName() {
        return longRunningProcessQueueName;
    }
}
