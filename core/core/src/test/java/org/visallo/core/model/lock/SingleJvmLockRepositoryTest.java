package org.visallo.core.model.lock;

import org.apache.curator.framework.recipes.leader.LeaderLatchListener;
import org.junit.Test;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class SingleJvmLockRepositoryTest {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(SingleJvmLockRepositoryTest.class);
    private LockRepository lockRepository = new SingleJvmLockRepository();

    @Test
    public void testCreateLock() throws Exception {
        List<String> messages = new ArrayList<>();
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            threads.add(createLockExercisingThread("lockOne", i, messages));
        }
        for (int i = 5; i < 10; i++) {
            threads.add(createLockExercisingThread("lockTwo", i, messages));
        }
        for (Thread t : threads) {
            t.start();
            t.join();
        }
        assertEquals(threads.size(), messages.size());
    }

    private Thread createLockExercisingThread(final String lockName, int threadIndex, final List<String> messages) {
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

    @Test
    public void testLeaderElection() throws Exception {
        List<String> messages = new ArrayList<>();
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            threads.add(createLeaderElectingThread("leaderOne", i, messages));
        }
        for (int i = 2; i < 5; i++) {
            threads.add(createLeaderElectingThread("leaderTwo", i, messages));
        }
        for (Thread t : threads) {
            t.start();
        }
        Thread.sleep(1000);
        assertEquals(2, messages.size());
    }

    private Thread createLeaderElectingThread(final String lockName, int threadIndex, final List<String> messages) {
        Thread t = new Thread() {
            @Override
            public void run() {
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