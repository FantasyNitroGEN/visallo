package org.visallo.core.model.hazelcast;

import com.google.inject.Inject;
import com.hazelcast.core.HazelcastInstanceNotActiveException;
import com.hazelcast.core.ILock;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.lock.LeaderListener;
import org.visallo.core.model.lock.Lock;
import org.visallo.core.model.lock.LockRepository;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

import java.util.concurrent.Callable;

public class HazelcastLockRepository extends LockRepository {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(HazelcastLockRepository.class);
    private final HazelcastRepository hazelcastRepository;
    private boolean exit;

    @Inject
    public HazelcastLockRepository(HazelcastRepository hazelcastRepository) {
        this.hazelcastRepository = hazelcastRepository;
        this.exit = false;
    }

    @Override
    public Lock createLock(String lockName) {
        return new Lock(lockName) {
            @Override
            public <T> T run(Callable<T> callable) {
                ILock lock = hazelcastRepository.getHazelcastInstance().getLock(getLockName());
                lock.lock();
                try {
                    return callable.call();
                } catch (Exception ex) {
                    throw new VisalloException("Failed to run in lock", ex);
                } finally {
                    lock.unlock();
                }
            }
        };
    }

    @Override
    public void leaderElection(String lockName, final LeaderListener listener) {
        final ILock lock = hazelcastRepository.getHazelcastInstance().getLock(lockName);
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!exit) {
                    try {
                        if (lock.isLockedByCurrentThread()) {
                            Thread.sleep(1000);
                            continue;
                        }
                        lock.lock();
                        listener.isLeader();
                    } catch (HazelcastInstanceNotActiveException exInner) {
                        handleHazelcastInstanceNotActiveException(exInner);
                    } catch (Throwable ex) {
                        if (exit) {
                            return;
                        }
                        LOGGER.error("Could not elect leader", ex);
                        try {
                            lock.unlock();
                        } catch (HazelcastInstanceNotActiveException exInner) {
                            handleHazelcastInstanceNotActiveException(exInner);
                        }
                    }
                }
            }

            private void handleHazelcastInstanceNotActiveException(HazelcastInstanceNotActiveException exInner) {
                LOGGER.debug("Hazelcast is already shutdown and will release the lock for us");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    LOGGER.debug("nothing we can do here");
                }
            }
        });
        t.setName(HazelcastLockRepository.class.getSimpleName() + "-LeaderElection-" + lockName);
        t.setDaemon(true);
        t.start();
    }

    @Override
    public void shutdown() {
        exit = true;
    }
}
