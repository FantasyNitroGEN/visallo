package org.visallo.core.model.lock;

import org.apache.curator.framework.recipes.locks.InterProcessLock;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class Lock {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(Lock.class);
    private final InterProcessLock interProcessLock;
    private final String lockName;

    public Lock(InterProcessLock interProcessLock, String lockName) {
        this.interProcessLock = interProcessLock;
        this.lockName = lockName;
    }

    public <T> T run(Callable<T> callable) {
        try {
            LOGGER.debug("acquire lock: %s", this.lockName);
            if (!this.interProcessLock.acquire(30, TimeUnit.SECONDS)) {
                throw new VisalloException("Could not acquire lock " + lockName);
            }
            LOGGER.debug("acquired lock: %s", this.lockName);
            try {
                return callable.call();
            } finally {
                this.interProcessLock.release();
                LOGGER.debug("released lock: %s", this.lockName);
            }
        } catch (Exception ex) {
            throw new VisalloException("Failed to run in lock", ex);
        }
    }
}
