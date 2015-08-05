package org.visallo.core.model.lock;

import org.apache.curator.framework.recipes.leader.LeaderLatchListener;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

import java.util.List;

public class LockRepositoryTestBase {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(LockRepositoryTestBase.class);

    protected Thread createLockExercisingThread(final LockRepository lockRepository, final String lockName, int threadIndex, final List<String> messages) {
        Thread t = new Thread() {
            @Override
            public void run() {
                lockRepository.lock(lockName, new Runnable() {
                    @Override
                    public void run() {
                        String message = String.format("[thread: %s] run: %s", Thread.currentThread().getName(), lockName);
                        LOGGER.debug(message);
                        messages.add(message);
                    }
                });
            }
        };
        t.setName("LockExercisingThread-" + threadIndex);
        t.setDaemon(true);
        return t;
    }

    protected Thread createLeaderElectingThread(final LockRepository lockRepository, final String lockName, int threadIndex, final List<String> messages) {
        Thread t = new Thread() {
            @Override
            public void run() {
                LOGGER.debug("thread %s started", Thread.currentThread().getName());
                lockRepository.leaderElection(lockName, new LeaderLatchListener() {
                    @Override
                    public void isLeader() {
                        String message = String.format("[thread: %s] isLeader: %s", Thread.currentThread().getName(), lockName);
                        LOGGER.debug(message);
                        messages.add(message);
                        while (true) {
                            // spin like we are happily running as the leader
                        }
                    }

                    @Override
                    public void notLeader() {
                        String message = String.format("[thread: %s] notLeader: %s", Thread.currentThread().getName(), lockName);
                        LOGGER.debug(message);
                    }
                });
            }
        };
        t.setName("LeaderElectingThread-" + threadIndex);
        t.setDaemon(true);
        return t;
    }
}
