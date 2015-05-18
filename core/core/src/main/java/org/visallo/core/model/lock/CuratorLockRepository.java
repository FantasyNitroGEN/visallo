package org.visallo.core.model.lock;

import com.google.inject.Inject;
import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloException;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.atomic.AtomicValue;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.curator.framework.recipes.leader.LeaderLatchListener;
import org.apache.curator.framework.recipes.locks.InterProcessLock;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.retry.BoundedExponentialBackoffRetry;

public class CuratorLockRepository extends LockRepository {
    private static final int BASE_SLEEP_TIME_MS = 10;
    private static final int MAX_SLEEP_TIME_MS = 2000;
    private static final int MAX_RETRIES = 5;
    private final CuratorFramework curatorFramework;
    private final String pathPrefix;

    @Inject
    public CuratorLockRepository(
            final CuratorFramework curatorFramework,
            final Configuration configuration
    ) {
        this.curatorFramework = curatorFramework;
        this.pathPrefix = configuration.get(Configuration.LOCK_REPOSITORY_PATH_PREFIX, Configuration.DEFAULT_LOCK_REPOSITORY_PATH_PREFIX);
    }

    private String getPath(String lockName) {
        return this.pathPrefix + "/" + lockName;
    }

    public Lock createLock(String lockName) {
        InterProcessLock l = new InterProcessMutex(this.curatorFramework, getPath(lockName));
        return new Lock(l, lockName);
    }

    public void leaderElection(String lockName, LeaderLatchListener listener) {
        String latchPath = getPath(lockName);
        try {
            LeaderLatch leaderLatch = new LeaderLatch(this.curatorFramework, latchPath);
            leaderLatch.addListener(listener);
            leaderLatch.start();
        } catch (Exception ex) {
            throw new VisalloException("Could not perform leader election: " + latchPath, ex);
        }
    }

    @Override
    public DistributedAtomicInteger getDistributedAtomicInteger(final String path, int initialValue) {
        RetryPolicy retryPolicy = new BoundedExponentialBackoffRetry(BASE_SLEEP_TIME_MS, MAX_SLEEP_TIME_MS, MAX_RETRIES);
        final org.apache.curator.framework.recipes.atomic.DistributedAtomicInteger distributedAtomicInteger
                = new org.apache.curator.framework.recipes.atomic.DistributedAtomicInteger(curatorFramework, path, retryPolicy);
        try {
            distributedAtomicInteger.initialize(initialValue); // this will respect an existing value but set uninitialized values
        } catch (Exception e) {
            throw new VisalloException("failed to initialize counter for " + path);
        }

        return new DistributedAtomicInteger() {
            @Override
            public int increment() {
                AtomicValue<Integer> count = null;
                try {
                    count = distributedAtomicInteger.increment();
                    if (count.succeeded()) {
                        return count.postValue();
                    } else {
                        throw new VisalloException("failed to increment counter for " + path);
                    }
                } catch (Exception e) {
                    throw new VisalloException("Could not increment counter for " + path, e);
                }
            }

            @Override
            public int decrement() {
                AtomicValue<Integer> count = null;
                try {
                    count = distributedAtomicInteger.decrement();
                    if (count.succeeded()) {
                        return count.postValue();
                    } else {
                        throw new VisalloException("failed to decrement counter for " + path);
                    }
                } catch (Exception e) {
                    throw new VisalloException("Could not decrement counter for " + path, e);
                }
            }
        };
    }

    @Override
    public void deleteDistributedAtomicInteger(String path) {
        try {
            curatorFramework.delete().inBackground().forPath(path);
        } catch (Exception e) {
            throw new VisalloException("failed to remove counter for " + path, e);
        }
    }

    @Override
    public void shutdown() {
        this.curatorFramework.close();
    }
}
