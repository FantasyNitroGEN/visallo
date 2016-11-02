package org.visallo.core.model;

import com.codahale.metrics.Counter;
import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.ingest.WorkerSpout;
import org.visallo.core.ingest.WorkerTuple;
import org.visallo.core.ingest.graphProperty.WorkerItem;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.status.MetricsManager;
import org.visallo.core.status.StatusServer;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

import java.util.LinkedList;
import java.util.Queue;

public abstract class WorkerBase<TWorkerItem extends WorkerItem> {
    private final boolean statusEnabled;
    private final boolean exitOnNextTupleFailure;
    private final Counter queueSizeMetric;
    private final MetricsManager metricsManager;
    private final String queueSizeMetricName;
    private WorkQueueRepository workQueueRepository;
    private volatile boolean shouldRun;
    private StatusServer statusServer = null;
    private final Queue<WorkerItemWrapper> tupleQueue = new LinkedList<>();
    private final int tupleQueueSize;
    private Thread processThread;

    protected WorkerBase(
            WorkQueueRepository workQueueRepository,
            Configuration configuration,
            MetricsManager metricsManager
    ) {
        this.workQueueRepository = workQueueRepository;
        this.metricsManager = metricsManager;
        this.exitOnNextTupleFailure = configuration.getBoolean(getClass().getName() + ".exitOnNextTupleFailure", true);
        this.tupleQueueSize = configuration.getInt(getClass().getName() + ".tupleQueueSize", 10);
        this.statusEnabled = configuration.getBoolean(Configuration.STATUS_ENABLED, Configuration.STATUS_ENABLED_DEFAULT);
        this.queueSizeMetricName = metricsManager.getNamePrefix(this) + "queue-size-" + Thread.currentThread().getId();
        this.queueSizeMetric = metricsManager.counter(queueSizeMetricName);
    }

    @Override
    protected void finalize() throws Throwable {
        metricsManager.removeMetric(queueSizeMetricName);
        super.finalize();
    }

    public void run() throws Exception {
        VisalloLogger logger = VisalloLoggerFactory.getLogger(this.getClass());

        logger.debug("begin runner");
        WorkerSpout workerSpout = prepareWorkerSpout();
        shouldRun = true;
        if (statusEnabled) {
            statusServer = createStatusServer();
        }
        startProcessThread(logger, workerSpout);
        pollWorkerSpout(logger, workerSpout);
    }

    private void startProcessThread(VisalloLogger logger, WorkerSpout workerSpout) {
        processThread = new Thread(() -> {
            while (shouldRun) {
                WorkerItemWrapper workerItemWrapper = null;
                try {
                    synchronized (tupleQueue) {
                        do {
                            while (shouldRun && tupleQueue.size() == 0) {
                                tupleQueue.wait();
                            }
                            if (!shouldRun) {
                                return;
                            }
                            if (tupleQueue.size() > 0) {
                                workerItemWrapper = tupleQueue.remove();
                                queueSizeMetric.dec();
                                tupleQueue.notifyAll();
                            }
                        } while (shouldRun && workerItemWrapper == null);
                    }
                } catch (Exception ex) {
                    throw new VisalloException("Could not get next workerItem", ex);
                }
                if (!shouldRun) {
                    return;
                }
                try {
                    logger.debug("start processing");
                    long startTime = System.currentTimeMillis();
                    process(workerItemWrapper.getWorkerItem());
                    long endTime = System.currentTimeMillis();
                    logger.debug("completed processing in (%dms)", endTime - startTime);
                    workerSpout.ack(workerItemWrapper.getMessageId());
                } catch (Throwable ex) {
                    logger.error("Could not process tuple: %s", workerItemWrapper, ex);
                    workerSpout.fail(workerItemWrapper.getMessageId());
                }
            }
        });
        processThread.setName(Thread.currentThread().getName() + "-process");
        processThread.start();
    }

    private void pollWorkerSpout(VisalloLogger logger, WorkerSpout workerSpout) throws InterruptedException {
        while (shouldRun) {
            WorkerItemWrapper workerItemWrapper;
            try {
                WorkerTuple tuple = workerSpout.nextTuple();
                if (tuple == null) {
                    workerItemWrapper = null;
                } else {
                    TWorkerItem workerItem = tupleDataToWorkerItem(tuple.getData());
                    workerItemWrapper = new WorkerItemWrapper(tuple.getMessageId(), workerItem);
                }
            } catch (InterruptedException ex) {
                throw ex;
            } catch (Exception ex) {
                handleNextTupleException(logger, ex);
                continue;
            }
            if (workerItemWrapper == null) {
                Thread.sleep(100);
                continue;
            }
            synchronized (tupleQueue) {
                tupleQueue.add(workerItemWrapper);
                queueSizeMetric.inc();
                tupleQueue.notifyAll();
                while (shouldRun && tupleQueue.size() >= tupleQueueSize) {
                    tupleQueue.wait();
                }
            }
        }
    }

    protected void handleNextTupleException(VisalloLogger logger, Exception ex) throws InterruptedException {
        if (exitOnNextTupleFailure) {
            throw new VisalloException("Failed to get next tuple", ex);
        } else {
            logger.error("Failed to get next tuple", ex);
            Thread.sleep(10 * 1000);
        }
    }

    protected abstract StatusServer createStatusServer() throws Exception;

    protected abstract void process(TWorkerItem workerItem) throws Exception;

    /**
     * This method gets called in a different thread than {@link #process(WorkerItem)} this
     * allows an implementing class to prefetch data needed for processing.
     */
    protected abstract TWorkerItem tupleDataToWorkerItem(byte[] data);

    public void stop() {
        shouldRun = false;
        if (statusServer != null) {
            statusServer.shutdown();
        }
        synchronized (tupleQueue) {
            tupleQueue.notifyAll();
        }
        try {
            processThread.join(10000);
        } catch (InterruptedException e) {
            throw new VisalloException("Could not stop process thread: " + processThread.getName());
        }
    }

    protected WorkerSpout prepareWorkerSpout() {
        WorkerSpout spout = workQueueRepository.createWorkerSpout(getQueueName());
        spout.open();
        return spout;
    }

    protected abstract String getQueueName();

    protected WorkQueueRepository getWorkQueueRepository() {
        return workQueueRepository;
    }

    public boolean shouldRun() {
        return shouldRun;
    }

    private class WorkerItemWrapper {
        private final Object messageId;
        private final TWorkerItem workerItem;

        public WorkerItemWrapper(Object messageId, TWorkerItem workerItem) {
            this.messageId = messageId;
            this.workerItem = workerItem;
        }

        public Object getMessageId() {
            return messageId;
        }

        public TWorkerItem getWorkerItem() {
            return workerItem;
        }
    }
}
