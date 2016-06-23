package org.visallo.core.model.lock;

import org.visallo.core.exception.VisalloException;

import java.util.WeakHashMap;
import java.util.concurrent.Callable;

public class SingleJvmLockRepository extends LockRepository {
    private WeakHashMap<Long, Thread> threads = new WeakHashMap<>();

    @Override
    public Lock createLock(String lockName) {
        final Object synchronizationObject = getSynchronizationObject(lockName);
        return new Lock(lockName) {
            @Override
            public <T> T run(Callable<T> callable) {
                try {
                    synchronized (synchronizationObject) {
                        return callable.call();
                    }
                } catch (Exception ex) {
                    throw new VisalloException("Failed to run in lock", ex);
                }
            }
        };
    }

    @Override
    public void leaderElection(String lockName, final LeaderListener listener) {
        final Object synchronizationObject = getSynchronizationObject(lockName);
        Thread t = new Thread(() -> {
            synchronized (synchronizationObject) {
                try {
                    listener.isLeader();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        t.setName(SingleJvmLockRepository.class.getSimpleName() + "-LeaderElection-" + lockName);
        t.setDaemon(true);
        t.start();
        threads.put(t.getId(), t);
    }

    @Override
    public void shutdown() {
        for (Thread thread : threads.values()) {
            thread.interrupt();
        }
    }
}
