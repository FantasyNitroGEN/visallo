package org.visallo.core.model.lock;

import com.google.inject.Inject;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.curator.framework.recipes.leader.LeaderLatchListener;
import org.apache.curator.framework.recipes.locks.InterProcessLock;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class CuratorLockRepository extends LockRepository {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(CuratorLockRepository.class);
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
        final InterProcessLock interProcessLock = new InterProcessMutex(this.curatorFramework, getPath(lockName));
        return new Lock(lockName) {
            @Override
            public <T> T run(Callable<T> callable) {
                try {
                    LOGGER.debug("acquire lock: %s", getLockName());
                    if (!interProcessLock.acquire(30, TimeUnit.SECONDS)) {
                        throw new VisalloException("Could not acquire lock " + getLockName());
                    }
                    LOGGER.debug("acquired lock: %s", getLockName());
                    try {
                        return callable.call();
                    } finally {
                        interProcessLock.release();
                        LOGGER.debug("released lock: %s", getLockName());
                    }
                } catch (Exception ex) {
                    throw new VisalloException("Failed to run in lock", ex);
                }
            }
        };
    }

    public void leaderElection(String lockName, final LeaderListener listener) {
        String latchPath = getPath(lockName);
        try {
            LeaderLatch leaderLatch = new LeaderLatch(this.curatorFramework, latchPath);
            leaderLatch.addListener(new LeaderLatchListenerAdapter(listener));
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
