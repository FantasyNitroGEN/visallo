package org.visallo.core.externalResource;

import org.visallo.core.bootstrap.InjectHelper;
import org.visallo.core.config.Configuration;
import org.visallo.core.status.MetricEntry;
import org.visallo.core.status.StatusServer;
import org.visallo.core.status.model.ExternalResourceRunnerStatus;
import org.visallo.core.status.model.Status;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ExternalResourceRunner {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(ExternalResourceRunner.class);
    private final Configuration config;
    private final User user;

    public ExternalResourceRunner(Configuration config, final User user) {
        this.config = config;
        this.user = user;
    }

    public void startAllAndWait() {
        Collection<RunningWorker> runningWorkers = startAll();
        while (runningWorkers.size() > 0) {
            for (RunningWorker runningWorker : runningWorkers) {
                if (!runningWorker.getThread().isAlive()) {
                    LOGGER.error("found a dead thread: " + runningWorker.getThread().getName());
                    return;
                }

                try {
                    runningWorker.getThread().join(1000);
                } catch (InterruptedException e) {
                    LOGGER.error("join interrupted", e);
                    return;
                }
            }
        }
    }

    public Collection<RunningWorker> startAll() {
        final List<RunningWorker> runningWorkers = new ArrayList<>();
        if (config.getBoolean(Configuration.STATUS_ENABLED, Configuration.STATUS_ENABLED_DEFAULT)) {
            startStatusServer(runningWorkers);
        }

        Collection<ExternalResourceWorker> workers = InjectHelper.getInjectedServices(ExternalResourceWorker.class, config);
        for (final ExternalResourceWorker worker : workers) {
            runningWorkers.add(start(worker, user));
        }
        return runningWorkers;
    }

    private void startStatusServer(final List<RunningWorker> runningWorkers) {
        new StatusServer(config, "externalResource", ExternalResourceRunner.class) {
            @Override
            protected ExternalResourceRunnerStatus createStatus() {
                ExternalResourceRunnerStatus status = new ExternalResourceRunnerStatus();
                for (RunningWorker runningWorker : runningWorkers) {
                    status.getRunningWorkers().add(runningWorker.getStatus());
                }
                return status;
            }
        };
    }

    private RunningWorker start(final ExternalResourceWorker worker, final User user) {
        worker.prepare(user);
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    worker.run();
                } catch (Throwable ex) {
                    LOGGER.error("Failed running external resource worker: " + worker.getClass().getName(), ex);
                }
            }
        });
        t.setName("external-resource-worker-" + worker.getClass().getSimpleName() + "-" + t.getId());
        t.setDaemon(true);
        LOGGER.debug("starting external resource worker thread: %s", t.getName());
        t.start();
        return new RunningWorker(worker, t);
    }

    public static class RunningWorker {
        private final ExternalResourceWorker worker;
        private final Thread thread;

        public RunningWorker(ExternalResourceWorker worker, Thread thread) {
            this.worker = worker;
            this.thread = thread;
        }

        public ExternalResourceWorker getWorker() {
            return worker;
        }

        public Thread getThread() {
            return thread;
        }

        public ExternalResourceRunnerStatus.ExternalResourceWorkerStatus getStatus() {
            ExternalResourceRunnerStatus.ExternalResourceWorkerStatus status = new ExternalResourceRunnerStatus.ExternalResourceWorkerStatus();
            StatusServer.getGeneralInfo(status, getWorker().getClass());
            status.setThreadName(getThread().getName());
            for (MetricEntry metric : getWorker().getMetrics()) {
                status.getMetrics().put(metric.getName(), Status.Metric.create(metric.getMetric()));
            }
            return status;
        }
    }
}
