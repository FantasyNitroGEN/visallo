package org.visallo.core.model.longRunningProcess;

import com.google.inject.Inject;
import org.json.JSONObject;
import org.visallo.core.bootstrap.InjectHelper;
import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.WorkQueueNames;
import org.visallo.core.model.WorkerBase;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.status.MetricsManager;
import org.visallo.core.status.StatusRepository;
import org.visallo.core.status.StatusServer;
import org.visallo.core.status.model.LongRunningProcessRunnerStatus;
import org.visallo.core.status.model.ProcessStatus;
import org.visallo.core.user.User;
import org.visallo.core.util.StoppableRunnable;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LongRunningProcessRunner extends WorkerBase<LongRunningProcessWorkerItem> {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(LongRunningProcessRunner.class);
    private UserRepository userRepository;
    private LongRunningProcessRepository longRunningProcessRepository;
    private User user;
    private WorkQueueNames workQueueNames;
    private Configuration configuration;
    private List<LongRunningProcessWorker> workers = new ArrayList<>();
    private final StatusRepository statusRepository;

    @Inject
    public LongRunningProcessRunner(
            WorkQueueRepository workQueueRepository,
            StatusRepository statusRepository,
            Configuration configuration,
            MetricsManager metricsManager
    ) {
        super(workQueueRepository, configuration, metricsManager);
        this.statusRepository = statusRepository;
    }

    public void prepare(Map map) {
        prepareUser(map);
        prepareWorkers(map);
    }

    private void prepareUser(Map map) {
        this.user = (User) map.get("user");
        if (this.user == null) {
            this.user = this.userRepository.getSystemUser();
        }
    }

    private void prepareWorkers(Map map) {
        LongRunningWorkerPrepareData workerPrepareData = new LongRunningWorkerPrepareData(
                map,
                this.user,
                InjectHelper.getInjector()
        );
        for (LongRunningProcessWorker worker : InjectHelper.getInjectedServices(LongRunningProcessWorker.class, configuration)) {
            try {
                LOGGER.info("preparing: %s", worker.getClass().getName());
                worker.prepare(workerPrepareData);
            } catch (Exception ex) {
                throw new VisalloException("Could not prepare graph property worker " + worker.getClass().getName(), ex);
            }
            workers.add(worker);
        }
    }

    @Override
    protected StatusServer createStatusServer() throws Exception {
        return new StatusServer(configuration, statusRepository, "longRunningProcess", LongRunningProcessRunner.class) {
            @Override
            protected ProcessStatus createStatus() {
                LongRunningProcessRunnerStatus status = new LongRunningProcessRunnerStatus();
                for (LongRunningProcessWorker worker : workers) {
                    status.getRunningWorkers().add(worker.getStatus());
                }
                return status;
            }
        };
    }

    @Override
    protected LongRunningProcessWorkerItem tupleDataToWorkerItem(byte[] data) {
        return new LongRunningProcessWorkerItem(data);
    }

    @Override
    public void process(LongRunningProcessWorkerItem workerItem) {
        JSONObject longRunningProcessQueueItem = workerItem.getJson();
        LOGGER.info("process long running queue item %s", longRunningProcessQueueItem.toString());

        for (LongRunningProcessWorker worker : workers) {
            if (worker.isHandled(longRunningProcessQueueItem)) {
                try {
                    longRunningProcessQueueItem.put("startTime", System.currentTimeMillis());
                    longRunningProcessQueueItem.put("progress", 0.0);
                    longRunningProcessRepository.beginWork(longRunningProcessQueueItem);
                    getWorkQueueRepository().broadcastLongRunningProcessChange(longRunningProcessQueueItem);

                    worker.process(longRunningProcessQueueItem);

                    longRunningProcessQueueItem.put("endTime", System.currentTimeMillis());
                    longRunningProcessQueueItem.put("progress", 1.0);
                    longRunningProcessRepository.ack(longRunningProcessQueueItem);
                    getWorkQueueRepository().broadcastLongRunningProcessChange(longRunningProcessQueueItem);
                } catch (Throwable ex) {
                    LOGGER.error("Failed to process long running process queue item", ex);
                    longRunningProcessQueueItem.put("error", ex.getMessage());
                    longRunningProcessQueueItem.put("endTime", System.currentTimeMillis());
                    longRunningProcessRepository.nak(longRunningProcessQueueItem, ex);
                    getWorkQueueRepository().broadcastLongRunningProcessChange(longRunningProcessQueueItem);
                }
                return;
            }
        }
    }

    @Override
    protected String getQueueName() {
        return workQueueNames.getLongRunningProcessQueueName();
    }

    @Inject
    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Inject
    public void setLongRunningProcessRepository(LongRunningProcessRepository longRunningProcessRepository) {
        this.longRunningProcessRepository = longRunningProcessRepository;
    }

    @Inject
    public void setWorkQueueNames(WorkQueueNames workQueueNames) {
        this.workQueueNames = workQueueNames;
    }

    @Inject
    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    public static List<StoppableRunnable> startThreaded(int threadCount, Configuration config) {
        List<StoppableRunnable> stoppables = new ArrayList<>();

        LOGGER.info("Starting LongRunningProcessRunners on %d threads", threadCount);
        for (int i = 0; i < threadCount; i++) {
            StoppableRunnable stoppable = new StoppableRunnable() {
                private LongRunningProcessRunner longRunningProcessRunner = null;

                @Override
                public void run() {
                    try {
                        longRunningProcessRunner = InjectHelper.getInstance(LongRunningProcessRunner.class);
                        longRunningProcessRunner.prepare(config.toMap());
                        longRunningProcessRunner.run();
                    } catch (Exception ex) {
                        LOGGER.error("Failed running LongRunningProcessRunner", ex);
                    }
                }

                @Override
                public void stop() {
                    try {
                        if (longRunningProcessRunner != null) {
                            LOGGER.debug("Stopping LongRunningProcessRunner");
                            longRunningProcessRunner.stop();
                        }
                    } catch (Exception ex) {
                        LOGGER.error("Failed stopping LongRunningProcessRunner", ex);
                    }
                }
            };
            stoppables.add(stoppable);
            Thread t = new Thread(stoppable);
            t.setName("long-running-process-runner-" + t.getId());
            t.setDaemon(true);
            LOGGER.debug("Starting LongRunningProcessRunner thread: %s", t.getName());
            t.start();
        }

        return stoppables;
    }
}
