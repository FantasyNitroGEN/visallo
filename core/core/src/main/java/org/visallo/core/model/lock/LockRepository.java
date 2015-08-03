package org.visallo.core.model.lock;

import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.apache.curator.framework.recipes.leader.LeaderLatchListener;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

public abstract class LockRepository {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(LockRepository.class);
    private final Map<String, Object> localLocks = new HashMap<>();

    public abstract Lock createLock(String lockName);

    public void lock(String lockName, final Runnable runnable) {
        lock(lockName, new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                runnable.run();
                return null;
            }
        });
    }

    public abstract void leaderElection(String lockName, LeaderLatchListener listener);

    public <T> T lock(String lockName, Callable<T> callable) {
        LOGGER.debug("starting lock: %s", lockName);
        try {
            Object localLock = getLocalLock(lockName);
            // localLock comes from a field so we can synchronized
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (localLock) {
                Lock lock = createLock(lockName);
                return lock.run(callable);
            }
        } finally {
            LOGGER.debug("ending lock: %s", lockName);
        }
    }

    private Object getLocalLock(String lockName) {
        synchronized (localLocks) {
            Object localLock = localLocks.get(lockName);
            if (localLock == null) {
                localLock = new Object();
                localLocks.put(lockName, localLock);
            }
            return localLock;
        }
    }

    public abstract void shutdown();
}
