package org.visallo.core.externalResource;

import com.google.inject.Inject;
import org.visallo.core.status.MetricEntry;
import org.visallo.core.status.MetricsManager;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

import java.util.Collection;

public abstract class ExternalResourceWorker {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(ExternalResourceWorker.class);
    private MetricsManager metricsManager;

    protected void prepare(
            @SuppressWarnings("UnusedParameters") User user
    ) {

    }

    protected abstract void run() throws Exception;

    @Inject
    public final void setMetricsManager(MetricsManager metricsManager) {
        this.metricsManager = metricsManager;
    }

    public MetricsManager getMetricsManager() {
        return metricsManager;
    }

    public abstract Collection<MetricEntry> getMetrics();
}
