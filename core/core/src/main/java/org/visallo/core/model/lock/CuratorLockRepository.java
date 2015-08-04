package org.visallo.core.model.lock;

import com.google.inject.Inject;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.curator.framework.recipes.leader.LeaderLatchListener;
import org.apache.curator.framework.recipes.locks.InterProcessLock;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloException;

public class CuratorLockRepository extends LockRepository {
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
    public void shutdown() {
        this.curatorFramework.close();
    }

    private String getPath(String lockName) {
        return this.pathPrefix + "/" + lockName;
    }
}
