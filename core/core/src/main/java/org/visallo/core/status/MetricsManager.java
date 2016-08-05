package org.visallo.core.status;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;

public interface MetricsManager {
    String getNamePrefix(final Object obj);

    Counter counter(final String name);

    Timer timer(final String name);

    void removeMetric(String metricName);
}
