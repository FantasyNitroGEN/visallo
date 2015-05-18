package org.visallo.core.model.lock;

public interface DistributedAtomicInteger {
    int increment();

    int decrement();
}
