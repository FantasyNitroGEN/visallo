package org.visallo.core.model.lock;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class NonLockingLockRepositoryTest extends LockRepositoryTestBase {
    @Test
    public void testCreateLock() throws Exception {
        super.testCreateLock(lockRepository);
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
        startThreadsWaitForMessagesThenStopThreads(
                threads,
                messages,
                5
        ); // this isn't what we really want but it is expected for this implementation
    }

    @Override
    protected LockRepository createLockRepository() {
        return new NonLockingLockRepository();
    }
}
