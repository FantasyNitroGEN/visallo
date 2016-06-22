package org.visallo.core.model.lock;

import java.util.WeakHashMap;
import java.util.concurrent.Callable;

public class NonLockingLockRepository extends LockRepository {
    private WeakHashMap<Long, Thread> threads = new WeakHashMap<>();

    @Override
    public Lock createLock(String lockName) {
        return new Lock(lockName) {
            @Override
            public <T> T run(Callable<T> callable) {
                try {
                    return callable.call();
                } catch (Exception ex) {
                    throw new RuntimeException("Failed to run in lock", ex);
                }
            }
        };
    }

    @Override
    public void leaderElection(String lockName, final LeaderListener listener) {
        Thread t = new Thread(() -> {
            try {
                listener.isLeader();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        t.setName(NonLockingLockRepository.class.getSimpleName() + "-LeaderElection-" + lockName);
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
