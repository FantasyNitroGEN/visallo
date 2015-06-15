package org.visallo.core.model.longRunningProcess;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.google.inject.Inject;
import org.json.JSONObject;
import org.visallo.core.status.MetricsManager;
import org.visallo.core.status.StatusServer;
import org.visallo.core.status.model.LongRunningProcessRunnerStatus;
import org.visallo.core.status.model.Status;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

public abstract class LongRunningProcessWorker {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(LongRunningProcessWorker.class);
    private MetricsManager metricsManager;
    private Counter totalProcessedCounter;
    private Counter totalErrorCounter;
    private Counter processingCounter;
    private Timer processingTimeTimer;

    public void prepare(LongRunningWorkerPrepareData workerPrepareData) {
        String namePrefix = getMetricsManager().getNamePrefix(this);
        totalProcessedCounter = getMetricsManager().counter(namePrefix + "total-processed");
        processingCounter = getMetricsManager().counter(namePrefix + "processing");
        totalErrorCounter = getMetricsManager().counter(namePrefix + "total-errors");
        processingTimeTimer = getMetricsManager().timer(namePrefix + "processing-time");
    }

    public abstract boolean isHandled(JSONObject longRunningProcessQueueItem);

    public final void process(JSONObject longRunningProcessQueueItem) {
        try (Timer.Context t = processingTimeTimer.time()) {
            processingCounter.inc();
            try {
                processInternal(longRunningProcessQueueItem);
            } finally {
                processingCounter.dec();
            }
            totalProcessedCounter.inc();
        } catch (Throwable ex) {
            LOGGER.error("Failed to complete long running process: " + longRunningProcessQueueItem, ex);
            this.totalErrorCounter.inc();
            throw ex;
        }
    }

    protected abstract void processInternal(JSONObject longRunningProcessQueueItem);

    public LongRunningProcessRunnerStatus.LongRunningProcessWorkerStatus getStatus() {
        LongRunningProcessRunnerStatus.LongRunningProcessWorkerStatus status = new LongRunningProcessRunnerStatus.LongRunningProcessWorkerStatus();
        StatusServer.getGeneralInfo(status, getClass());
        status.getMetrics().put("totalProcessed", Status.Metric.create(totalProcessedCounter));
        status.getMetrics().put("processing", Status.Metric.create(processingCounter));
        status.getMetrics().put("totalErrors", Status.Metric.create(totalErrorCounter));
        status.getMetrics().put("processingTime", Status.Metric.create(processingTimeTimer));
        return status;
    }

    @Inject
    public final void setMetricsManager(MetricsManager metricsManager) {
        this.metricsManager = metricsManager;
    }

    public MetricsManager getMetricsManager() {
        return metricsManager;
    }
}
