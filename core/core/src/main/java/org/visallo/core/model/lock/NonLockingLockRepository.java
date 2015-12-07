package org.visallo.core.model.lock;

import java.util.concurrent.Callable;

public class NonLockingLockRepository extends LockRepository {
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
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                listener.isLeader();
            }
        });
        t.setName(NonLockingLockRepository.class.getSimpleName() + "-LeaderElection-" + lockName);
        t.setDaemon(true);
        t.start();
    }

    @Override
    public void shutdown() {
        // no implementation required
    }
}
