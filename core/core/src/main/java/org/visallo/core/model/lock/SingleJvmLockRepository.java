package org.visallo.core.model.lock;

import org.apache.curator.framework.recipes.leader.LeaderLatchListener;

import java.util.concurrent.Callable;

public class SingleJvmLockRepository extends LockRepository {
    @Override
    public Lock createLock(String lockName) {
        final Object synchronizationObject = getSynchronizationObject(lockName);
        return new Lock(null, lockName) {
            @Override
            public <T> T run(Callable<T> callable) {
                try {
                    synchronized (synchronizationObject) {
                        return callable.call();
                    }
                } catch (Exception ex) {
                    throw new RuntimeException("Failed to run in lock", ex);
                }
            }
        };
    }

    @Override
    public void leaderElection(String lockName, final LeaderLatchListener listener) {
        final Object synchronizationObject = getSynchronizationObject(lockName);
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (synchronizationObject) {
                    listener.isLeader();
                }
            }
        });
        t.setName(SingleJvmLockRepository.class.getSimpleName() + "-LeaderElection-" + lockName);
        t.setDaemon(true);
        t.start();
    }

    @Override
    public void shutdown() {
        // no implementation required
    }
}
