package org.visallo.core.model.lock;

import org.apache.curator.framework.recipes.locks.InterProcessLock;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class Lock {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(Lock.class);
    private final InterProcessLock lock;
    private final String lockName;

    public Lock(InterProcessLock lock, String lockName) {
        this.lock = lock;
        this.lockName = lockName;
    }

    public <T> T run(Callable<T> runnable) {
        try {
            LOGGER.debug("acquire lock: %s", this.lockName);
            if (!this.lock.acquire(30, TimeUnit.SECONDS)) {
                throw new VisalloException("Could not acquire lock " + lockName);
            }
            LOGGER.debug("acquired lock: %s", this.lockName);
            try {
                return runnable.call();
            } finally {
                this.lock.release();
                LOGGER.debug("released lock: %s", this.lockName);
            }
        } catch (Exception ex) {
            throw new VisalloException("Failed to run in lock", ex);
        }
    }
}
