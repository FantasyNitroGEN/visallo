package org.visallo.core.model.lock;

import org.apache.curator.framework.recipes.leader.LeaderLatchListener;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

public class LocalLockRepository extends LockRepository {
    private Map<String, AtomicInteger> atomicIntegers = new HashMap<>();

    @Override
    public Lock createLock(String lockName) {
        return new Lock(null, lockName) {
            @Override
            public <T> T run(Callable<T> runnable) {
                try {
                    return runnable.call();
                } catch (Exception ex) {
                    throw new RuntimeException("Failed to run in lock", ex);
                }
            }
        };
    }

    @Override
    public void leaderElection(String lockName, final LeaderLatchListener listener) {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                listener.isLeader();
            }
        });
        t.setName("leaderElection-" + lockName);
        t.setDaemon(true);
        t.start();
    }

    @Override
    public DistributedAtomicInteger getDistributedAtomicInteger(String path, int initialValue) {
        AtomicInteger ai = atomicIntegers.get(path);
        if (ai == null) {
            ai = new AtomicInteger(initialValue);
            atomicIntegers.put(path, ai);
        }
        final AtomicInteger finalAtomicInteger = ai;
        return new DistributedAtomicInteger() {
            @Override
            public int increment() {
                return finalAtomicInteger.incrementAndGet();
            }

            @Override
            public int decrement() {
                return finalAtomicInteger.decrementAndGet();
            }
        };
    }

    @Override
    public void deleteDistributedAtomicInteger(String path) {
        atomicIntegers.remove(path);
    }

    @Override
    public void shutdown() {

    }
}
