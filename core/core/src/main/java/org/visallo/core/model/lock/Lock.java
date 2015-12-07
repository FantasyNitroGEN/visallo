package org.visallo.core.model.lock;

import java.util.concurrent.Callable;

public abstract class Lock {
    private final String lockName;

    public Lock(String lockName) {
        this.lockName = lockName;
    }

    public abstract <T> T run(Callable<T> callable);

    public String getLockName() {
        return lockName;
    }
}
