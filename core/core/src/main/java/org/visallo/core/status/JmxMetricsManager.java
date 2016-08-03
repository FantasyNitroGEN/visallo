package org.visallo.core.status;

import com.codahale.metrics.Counter;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

import java.util.concurrent.atomic.AtomicInteger;

public class JmxMetricsManager implements MetricsManager {
    private static final MetricRegistry REGISTRY;
    private static final JmxReporter JMX_REPORTER;
    private static final AtomicInteger ID = new AtomicInteger(0);

    static {
        REGISTRY = new MetricRegistry();
        JMX_REPORTER = JmxReporter.forRegistry(REGISTRY).build();
        JMX_REPORTER.start();
    }

    private static int nextId() {
        return ID.getAndIncrement();
    }

    @Override
    public String getNamePrefix(Object obj) {
        return String.format("%s.%d.", obj.getClass().getName(), JmxMetricsManager.nextId());
    }

    @Override
    public Counter counter(String name) {
        return REGISTRY.counter(name);
    }

    @Override
    public Timer timer(String name) {
        return REGISTRY.timer(name);
    }

    @Override
    public void removeMetric(String metricName) {
        REGISTRY.remove(metricName);
    }
}
