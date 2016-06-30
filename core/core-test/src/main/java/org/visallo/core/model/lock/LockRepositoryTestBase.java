package org.visallo.core.model.lock;

import org.junit.After;
import org.junit.Before;
import org.visallo.core.util.ShutdownListener;
import org.visallo.core.util.ShutdownService;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public abstract class LockRepositoryTestBase {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(LockRepositoryTestBase.class);
    protected LockRepository lockRepository;
    protected ShutdownService shutdownService = new ShutdownService();

    protected abstract LockRepository createLockRepository();

    @Before
    public void before() throws Exception {
        lockRepository = createLockRepository();
    }

    @After
    public void after() throws Exception {
        shutdownService.shutdown();
    }

    protected Thread createLockExercisingThread(
            final LockRepository lockRepository,
            final String lockName,
            int threadIndex,
            final List<String> messages
    ) {
        Thread t = new Thread() {
            @Override
            public void run() {
                lockRepository.lock(lockName, new Runnable() {
                    @Override
                    public void run() {
                        String message = String.format(
                                "[thread: %s] run: %s",
                                Thread.currentThread().getName(),
                                lockName
                        );
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

    protected Thread createLeaderElectingThread(
            final LockRepository lockRepository,
            final String lockName,
            int threadIndex,
            final List<String> messages
    ) {
        Thread t = new Thread() {
            @Override
            public void run() {
                LOGGER.debug("thread %s started", Thread.currentThread().getName());
                lockRepository.leaderElection(lockName, new LeaderListener() {
                    @Override
                    public void isLeader() throws InterruptedException {
                        String message = String.format(
                                "[thread: %s, threadIndex: %d] isLeader: %s",
                                Thread.currentThread().getName(),
                                threadIndex,
                                lockName
                        );
                        LOGGER.debug(message);
                        messages.add(message);
                        while (true) {
                            // spin like we are happily running as the leader
                            Thread.sleep(1000);
                        }
                    }

                    @Override
                    public void notLeader() {
                        String message = String.format(
                                "[thread: %s, threadIndex: %d] notLeader: %s",
                                Thread.currentThread().getName(),
                                threadIndex,
                                lockName
                        );
                        LOGGER.debug(message);
                    }
                });
            }
        };
        t.setName("LeaderElectingThread-" + threadIndex);
        t.setDaemon(true);
        return t;
    }

    protected void startThreadsWaitForMessagesThenStopThreads(
            List<Thread> threads,
            List<String> messages,
            int expectedMessageCount
    ) throws InterruptedException {
        for (Thread t : threads) {
            t.start();
        }
        for (int i = 0; i < 300 && messages.size() < expectedMessageCount; i++) {
            Thread.sleep(100);
        }
        assertEquals(expectedMessageCount, messages.size());
        for (Thread t : threads) {
            t.interrupt();
        }
    }

    protected void testCreateLock(LockRepository lockRepository) throws InterruptedException {
        List<String> messages = new ArrayList<>();
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            threads.add(createLockExercisingThread(lockRepository, "lockOne", i, messages));
        }
        for (int i = 5; i < 10; i++) {
            threads.add(createLockExercisingThread(lockRepository, "lockTwo", i, messages));
        }
        for (Thread t : threads) {
            t.start();
            t.join();
        }
        if (threads.size() != messages.size()) {
            throw new RuntimeException("Expected " + threads.size() + " found " + messages.size());
        }
    }
}
