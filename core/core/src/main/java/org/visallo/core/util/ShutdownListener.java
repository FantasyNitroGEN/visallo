package org.visallo.core.util;

/**
 * Implementors of this class must call {@link ShutdownService#register(ShutdownListener)} to be notified of shutdown
 */
public interface ShutdownListener {
    void shutdown();
}
