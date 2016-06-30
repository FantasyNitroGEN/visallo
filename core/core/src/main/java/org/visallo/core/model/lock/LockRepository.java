package org.visallo.core.model.lock;

import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

public abstract class LockRepository {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(LockRepository.class);
    protected final Map<String, Object> synchronizationObjects = new HashMap<>();

    public void lock(String lockName, final Runnable runnable) {
        lock(lockName, new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                runnable.run();
                return null;
            }
        });
    }

    public <T> T lock(String lockName, Callable<T> callable) {
        LOGGER.debug("[thread: %s] acquiring lock: %s", Thread.currentThread().getName(), lockName);
        try {
            Object synchronizationObject = getSynchronizationObject(lockName);
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (synchronizationObject) {
                LOGGER.debug("[thread: %s] creating lock: %s", Thread.currentThread().getName(), lockName);
                Lock lock = createLock(lockName);
                LOGGER.debug("[thread: %s] running lock: %s", Thread.currentThread().getName(), lockName);
                return lock.run(callable);
            }
        } finally {
            LOGGER.debug("[thread: %s] released lock: %s", Thread.currentThread().getName(), lockName);
        }
    }

    protected Object getSynchronizationObject(String lockName) {
        synchronized (synchronizationObjects) {
            Object synchronizationObject = synchronizationObjects.get(lockName);
            if (synchronizationObject == null) {
                synchronizationObject = new Object();
                synchronizationObjects.put(lockName, synchronizationObject);
            }
            return synchronizationObject;
        }
    }

    public abstract Lock createLock(String lockName);

    public abstract void leaderElection(String lockName, LeaderListener listener);
}
