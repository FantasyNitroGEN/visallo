package org.visallo.core.util;

import org.apache.commons.lang.time.DateUtils;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.lock.LeaderListener;
import org.visallo.core.model.lock.LockRepository;

import java.util.Date;

public abstract class PeriodicBackgroundService {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(PeriodicBackgroundService.class);
    private final LockRepository lockRepository;
    private volatile boolean enabled;

    protected PeriodicBackgroundService(LockRepository lockRepository) {
        this.lockRepository = lockRepository;
    }

    public void start() {
        if (getCheckIntervalSeconds() > 0) {
            startBackgroundThread();
        }
    }

    @SuppressWarnings("InfiniteLoopStatement")
    private void startBackgroundThread() {
        Thread t = new Thread(() -> {
            enabled = false;
            lockRepository.leaderElection(getLockName(), new LeaderListener() {
                @Override
                public void isLeader() {
                    LOGGER.debug("using successfully acquired lock (%s)", Thread.currentThread().getName());
                    enabled = true;
                }

                @Override
                public void notLeader() {
                    LOGGER.debug("lost leadership (%s)", Thread.currentThread().getName());
                    disable();
                }
            });

            while (true) {
                try {
                    Thread.sleep(10 * 1000); // wait for enabled to change
                } catch (InterruptedException e) {
                    LOGGER.error("Failed to sleep", e);
                    throw new VisalloException("Failed to sleep", e);
                }
                runPeriodically();
            }
        });
        t.setDaemon(true);
        t.setName(getThreadName());
        t.start();
    }

    private void runPeriodically() {
        try {
            while (enabled) {
                LOGGER.debug("running periodically");
                Date now = new Date();
                Date nowPlusOneMinute = DateUtils.addSeconds(now, getCheckIntervalSeconds());
                run();
                try {
                    long remainingMilliseconds = nowPlusOneMinute.getTime() - System.currentTimeMillis();
                    if (remainingMilliseconds > 0) {
                        Thread.sleep(remainingMilliseconds);
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }
        } catch (Throwable ex) {
            LOGGER.error("runPeriodically error", ex);
            throw ex;
        }
    }

    protected String getThreadName() {
        return "visallo-periodic-" + this.getClass().getSimpleName();
    }

    protected String getLockName() {
        return this.getClass().getName();
    }

    protected abstract void run();

    protected abstract int getCheckIntervalSeconds();

    public void disable() {
        enabled = false;
    }
}
