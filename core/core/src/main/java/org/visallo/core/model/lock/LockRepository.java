package org.visallo.core.model.lock;

import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.apache.curator.framework.recipes.leader.LeaderLatchListener;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

public abstract class LockRepository {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(LockRepository.class);
    private final Map<String, Object> synchronizationObjects = new HashMap<>();

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
        LOGGER.debug("starting lock: %s", lockName);
        try {
            Object synchronizationObject = getSynchronizationObject(lockName);
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (synchronizationObject) {
                Lock lock = createLock(lockName);
                return lock.run(callable);
            }
        } finally {
            LOGGER.debug("ending lock: %s", lockName);
        }
    }

    private Object getSynchronizationObject(String lockName) {
        synchronized (synchronizationObjects) {
            Object localLock = synchronizationObjects.get(lockName);
            if (localLock == null) {
                localLock = new Object();
                synchronizationObjects.put(lockName, localLock);
            }
            return localLock;
        }
    }

    public abstract Lock createLock(String lockName);

    public abstract void leaderElection(String lockName, LeaderLatchListener listener);

    public abstract void shutdown();
}
