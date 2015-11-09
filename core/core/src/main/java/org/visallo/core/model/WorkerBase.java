package org.visallo.core.model;

import com.google.inject.Inject;
import org.apache.curator.framework.CuratorFramework;
import org.json.JSONObject;
import org.visallo.core.ingest.WorkerSpout;
import org.visallo.core.ingest.WorkerTuple;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.status.StatusServer;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

public abstract class WorkerBase {
    private WorkQueueRepository workQueueRepository;
    private CuratorFramework curatorFramework;
    private boolean shouldRun;

    public void run() throws Exception {
        VisalloLogger logger = VisalloLoggerFactory.getLogger(this.getClass());

        logger.debug("begin runner");
        WorkerSpout workerSpout = prepareWorkerSpout();
        shouldRun = true;
        StatusServer statusServer = null;
        try {
            statusServer = createStatusServer();
            while (shouldRun) {
                WorkerTuple tuple = workerSpout.nextTuple();
                if (tuple == null) {
                    Thread.sleep(100);
                    continue;
                }
                try {
                    logger.debug("start processing");
                    long startTime = System.currentTimeMillis();
                    process(tuple.getMessageId(), tuple.getJson());
                    long endTime = System.currentTimeMillis();
                    logger.debug("completed processing in (%dms)", endTime - startTime);
                    workerSpout.ack(tuple.getMessageId());
                } catch (Throwable ex) {
                    logger.error("Could not process tuple: %s", tuple, ex);
                    workerSpout.fail(tuple.getMessageId());
                }
            }
        } finally {
            if (statusServer != null) {
                statusServer.shutdown();
            }
            logger.debug("end runner");
        }
    }

    protected abstract StatusServer createStatusServer() throws Exception;

    protected abstract void process(Object messageId, JSONObject json) throws Exception;

    public void stop() {
        shouldRun = false;
    }

    protected WorkerSpout prepareWorkerSpout() {
        WorkerSpout spout = workQueueRepository.createWorkerSpout(getQueueName());
        spout.open();
        return spout;
    }

    protected abstract String getQueueName();

    @Inject
    public final void setWorkQueueRepository(WorkQueueRepository workQueueRepository) {
        this.workQueueRepository = workQueueRepository;
    }

    public WorkQueueRepository getWorkQueueRepository() {
        return workQueueRepository;
    }

    @Inject
    public final void setCuratorFramework(CuratorFramework curatorFramework) {
        this.curatorFramework = curatorFramework;
    }

    public CuratorFramework getCuratorFramework() {
        return curatorFramework;
    }


}
