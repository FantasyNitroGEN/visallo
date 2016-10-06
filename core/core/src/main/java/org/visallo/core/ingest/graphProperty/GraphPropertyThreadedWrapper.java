package org.visallo.core.ingest.graphProperty;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.google.inject.Inject;
import org.vertexium.Element;
import org.visallo.core.status.MetricsManager;
import org.visallo.core.status.PausableTimerContext;
import org.visallo.core.status.PausableTimerContextAware;
import org.visallo.core.status.StatusServer;
import org.visallo.core.status.model.GraphPropertyRunnerStatus;
import org.visallo.core.status.model.Status;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;

public class GraphPropertyThreadedWrapper implements Runnable {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(GraphPropertyThreadedWrapper.class);
    private final GraphPropertyWorker worker;

    public GraphPropertyThreadedWrapper(GraphPropertyWorker worker) {
        this.worker = worker;
    }

    private Counter totalProcessedCounter = null;
    private Counter processingCounter;
    private Counter totalErrorCounter;
    private Timer processingTimeTimer;
    private boolean stopped;
    private final Queue<Work> workItems = new LinkedList<>();
    private final Queue<WorkResult> workResults = new LinkedList<>();
    private MetricsManager metricsManager;

    @Override
    public final void run() {
        ensureMetricsInitialized();

        stopped = false;
        try {
            while (!stopped) {
                Work work;
                synchronized (workItems) {
                    if (workItems.size() == 0) {
                        workItems.wait(1000);
                        continue;
                    }
                    work = workItems.remove();
                }
                InputStream in = work.getIn();
                String workerClassName = this.worker.getClass().getName();
                Element element = work.getData() == null ? null : work.getData().getElement();
                String elementId = element == null ? null : element.getId();
                try {
                    LOGGER.debug("BEGIN doWork (%s): %s", workerClassName, elementId);
                    PausableTimerContext timerContext = new PausableTimerContext(processingTimeTimer);
                    if (in instanceof PausableTimerContextAware) {
                        ((PausableTimerContextAware) in).setPausableTimerContext(timerContext);
                    }
                    processingCounter.inc();
                    long startTime = System.currentTimeMillis();
                    try {
                        this.worker.execute(in, work.getData());
                    } finally {
                        long endTime = System.currentTimeMillis();
                        long time = endTime - startTime;
                        LOGGER.debug("END doWork (%s): %s (%dms)", workerClassName, elementId, time);
                        processingCounter.dec();
                        totalProcessedCounter.inc();
                        timerContext.stop();
                    }
                    synchronized (workResults) {
                        workResults.add(new WorkResult(null));
                        workResults.notifyAll();
                    }
                } catch (Throwable ex) {
                    LOGGER.error("failed to complete work (%s): %s", workerClassName, elementId, ex);
                    totalErrorCounter.inc();
                    synchronized (workResults) {
                        workResults.add(new WorkResult(ex));
                        workResults.notifyAll();
                    }
                } finally {
                    try {
                        if (in != null) {
                            in.close();
                        }
                    } catch (IOException ex) {
                        synchronized (workResults) {
                            workResults.add(new WorkResult(ex));
                            workResults.notifyAll();
                        }
                    }
                }
            }
        } catch (InterruptedException ex) {
            LOGGER.error("thread was interrupted", ex);
        }
    }

    private void ensureMetricsInitialized() {
        if (totalProcessedCounter == null) {
            String namePrefix = metricsManager.getNamePrefix(this.worker);
            totalProcessedCounter = metricsManager.counter(namePrefix + "total-processed");
            processingCounter = metricsManager.counter(namePrefix + "processing");
            totalErrorCounter = metricsManager.counter(namePrefix + "total-errors");
            processingTimeTimer = metricsManager.timer(namePrefix + "processing-time");
        }
    }

    public void enqueueWork(InputStream in, GraphPropertyWorkData data) {
        synchronized (workItems) {
            workItems.add(new Work(in, data));
            workItems.notifyAll();
        }
    }

    public WorkResult dequeueResult(boolean waitForever) {
        synchronized (workResults) {
            if (workResults.size() == 0) {
                long startTime = new Date().getTime();
                while (workResults.size() == 0 && (waitForever || (new Date().getTime() - startTime < 10 * 1000))) {
                    try {
                        if (new Date().getTime() - startTime > 60 * 1000) {
                            LOGGER.warn("worker has zero results. sleeping waiting for results.");
                        } else {
                            LOGGER.debug("worker has zero results. sleeping waiting for results.");
                        }
                        workResults.wait(waitForever ? 30 * 1000 : 1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            return workResults.remove();
        }
    }

    public void stop() {
        stopped = true;
    }

    public GraphPropertyWorker getWorker() {
        return worker;
    }

    public GraphPropertyRunnerStatus.GraphPropertyWorkerStatus getStatus() {
        GraphPropertyRunnerStatus.GraphPropertyWorkerStatus status = new GraphPropertyRunnerStatus.GraphPropertyWorkerStatus();
        StatusServer.getGeneralInfo(status, this.worker.getClass());
        status.getMetrics().put("totalProcessed", Status.Metric.create(totalProcessedCounter));
        status.getMetrics().put("processing", Status.Metric.create(processingCounter));
        status.getMetrics().put("totalErrors", Status.Metric.create(totalErrorCounter));
        status.getMetrics().put("processingTime", Status.Metric.create(processingTimeTimer));
        return status;
    }

    private class Work {
        private final InputStream in;
        private final GraphPropertyWorkData data;

        public Work(InputStream in, GraphPropertyWorkData data) {
            this.in = in;
            this.data = data;
        }

        private InputStream getIn() {
            return in;
        }

        private GraphPropertyWorkData getData() {
            return data;
        }
    }

    public static class WorkResult {
        private final Throwable error;

        public WorkResult(Throwable error) {
            this.error = error;
        }

        public Throwable getError() {
            return error;
        }
    }

    @Inject
    public void setMetricsManager(MetricsManager metricsManager) {
        this.metricsManager = metricsManager;
    }

    @Override
    public String toString() {
        return "GraphPropertyThreadedWrapper{" +
                "worker=" + worker +
                '}';
    }
}
