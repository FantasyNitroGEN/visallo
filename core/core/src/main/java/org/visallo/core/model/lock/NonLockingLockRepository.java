package org.visallo.core.model.lock;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import org.visallo.core.util.ShutdownListener;
import org.visallo.core.util.ShutdownService;

import java.util.WeakHashMap;
import java.util.concurrent.Callable;

public class NonLockingLockRepository extends LockRepository implements ShutdownListener {
    private WeakHashMap<Long, Thread> threads = new WeakHashMap<>();

    // available for testing when you don't need perfect shutdown behavior
    @VisibleForTesting
    public NonLockingLockRepository() {

    }

    @Inject
    public NonLockingLockRepository(ShutdownService shutdownService) {
        shutdownService.register(this);
    }

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
