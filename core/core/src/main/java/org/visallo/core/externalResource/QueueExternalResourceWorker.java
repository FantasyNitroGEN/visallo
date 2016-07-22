package org.visallo.core.externalResource;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.google.inject.Inject;
import org.json.JSONObject;
import org.vertexium.Authorizations;
import org.visallo.core.ingest.WorkerSpout;
import org.visallo.core.ingest.WorkerTuple;
import org.visallo.core.model.user.AuthorizationRepository;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.status.MetricEntry;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

import java.util.ArrayList;
import java.util.Collection;

public abstract class QueueExternalResourceWorker extends ExternalResourceWorker {
    public static final String QUEUE_NAME_PREFIX = "externalResource-";
    private final AuthorizationRepository authorizationRepository;
    private WorkQueueRepository workQueueRepository;
    private UserRepository userRepository;
    private volatile boolean shouldRun;
    private Timer processingTimeTimer;
    private Counter totalProcessedCounter;
    private Counter totalErrorCounter;
    private Collection<MetricEntry> metrics;

    protected QueueExternalResourceWorker(AuthorizationRepository authorizationRepository) {
        this.authorizationRepository = authorizationRepository;
    }

    @Override
    protected void prepare(@SuppressWarnings("UnusedParameters") User user) {
        super.prepare(user);
        String namePrefix = getMetricsManager().getNamePrefix(this);
        this.totalProcessedCounter = getMetricsManager().counter(namePrefix + "total-processed");
        this.totalErrorCounter = getMetricsManager().counter(namePrefix + "total-errors");
        this.processingTimeTimer = getMetricsManager().timer(namePrefix + "processing-time");

        metrics = new ArrayList<>();
        metrics.add(new MetricEntry("totalProcessed", this.totalProcessedCounter));
        metrics.add(new MetricEntry("totalErrors", this.totalErrorCounter));
        metrics.add(new MetricEntry("processingTime", this.processingTimeTimer));
    }

    @Override
    protected void run() throws Exception {
        VisalloLogger logger = VisalloLoggerFactory.getLogger(this.getClass());

        Authorizations authorizations = authorizationRepository.getGraphAuthorizations(getUserRepository().getSystemUser());

        WorkerSpout workerSpout = this.workQueueRepository.createWorkerSpout(getQueueName());
        workerSpout.open();

        shouldRun = true;
        while (shouldRun) {
            WorkerTuple tuple = workerSpout.nextTuple();
            if (tuple == null) {
                Thread.sleep(100);
                continue;
            }
            try (Timer.Context t = processingTimeTimer.time()) {
                long startTime = System.currentTimeMillis();
                process(tuple.getMessageId(), tuple.getJson(), authorizations);
                long endTime = System.currentTimeMillis();
                logger.debug("completed processing in (%dms)", endTime - startTime);
                workerSpout.ack(tuple.getMessageId());
                this.totalProcessedCounter.inc();
            } catch (Throwable ex) {
                logger.error("Could not process tuple: %s", tuple, ex);
                this.totalErrorCounter.inc();
                workerSpout.fail(tuple.getMessageId());
            }
        }
        logger.debug("end runner");
    }

    public void stop() {
        shouldRun = false;
    }

    protected abstract void process(Object messageId, JSONObject json, Authorizations authorizations) throws Exception;

    public abstract String getQueueName();

    @Inject
    public final void setWorkQueueRepository(WorkQueueRepository workQueueRepository) {
        this.workQueueRepository = workQueueRepository;
    }

    public WorkQueueRepository getWorkQueueRepository() {
        return workQueueRepository;
    }

    public UserRepository getUserRepository() {
        return userRepository;
    }

    @Inject
    public final void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Collection<MetricEntry> getMetrics() {
        return metrics;
    }
}
