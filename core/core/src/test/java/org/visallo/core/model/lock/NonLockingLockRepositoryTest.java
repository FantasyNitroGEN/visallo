package org.visallo.core.model.lock;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class NonLockingLockRepositoryTest extends LockRepositoryTestBase {
    private LockRepository lockRepository = new NonLockingLockRepository();

    @Test
    public void testCreateLock() throws Exception {
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
        assertEquals(threads.size(), messages.size());
    }

    @Test
    public void testLeaderElection() throws Exception {
        List<String> messages = new ArrayList<>();
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            threads.add(createLeaderElectingThread(lockRepository, "leaderOne", i, messages));
        }
        for (int i = 2; i < 5; i++) {
            threads.add(createLeaderElectingThread(lockRepository, "leaderTwo", i, messages));
        }
        for (Thread t : threads) {
            t.start();
        }
        Thread.sleep(1000);
        assertEquals(5, messages.size()); // this isn't what we really want but it is expected for this implementation
    }
}
