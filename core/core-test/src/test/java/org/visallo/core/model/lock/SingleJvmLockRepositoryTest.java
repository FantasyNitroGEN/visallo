package org.visallo.core.model.lock;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SingleJvmLockRepositoryTest extends LockRepositoryTestBase {
    @Test
    public void testCreateLock() throws Exception {
        super.testCreateLock(lockRepository);
    }

    @Test
    public void testLeaderElection() throws Exception {
        List<String> messages = Collections.synchronizedList(new ArrayList<>());
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            threads.add(createLeaderElectingThread(lockRepository, "leaderOne", i, messages));
        }
        for (int i = 2; i < 5; i++) {
            threads.add(createLeaderElectingThread(lockRepository, "leaderTwo", i, messages));
        }
        startThreadsWaitForMessagesThenStopThreads(threads, messages, 2);
    }

    @Override
    protected LockRepository createLockRepository() {
        return new SingleJvmLockRepository();
    }
}