package org.visallo.core.model.longRunningProcess;

import org.visallo.core.model.properties.types.JsonSingleValueVisalloProperty;

public class LongRunningProcessProperties {
    public static final String LONG_RUNNING_PROCESS_CONCEPT_IRI = "http://visallo.org/longRunningProcess#longRunningProcess";
    public static final String LONG_RUNNING_PROCESS_TO_USER_EDGE_IRI = "http://visallo.org/longRunningProcess#hasLongRunningProcess";
    public static final String LONG_RUNNING_PROCESS_ID_PREFIX = "LONG_RUNNING_PROCESS_";
    public static final String OWL_IRI = "http://visallo.org/longRunningProcess";

    public static JsonSingleValueVisalloProperty QUEUE_ITEM_JSON_PROPERTY = new JsonSingleValueVisalloProperty("http://visallo.org/longRunningProcess#queueItemJson");
}
