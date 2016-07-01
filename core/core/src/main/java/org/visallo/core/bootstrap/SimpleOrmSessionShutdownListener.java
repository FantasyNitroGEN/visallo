package org.visallo.core.bootstrap;

import com.v5analytics.simpleorm.SimpleOrmSession;
import org.visallo.core.util.ShutdownListener;

public class SimpleOrmSessionShutdownListener implements ShutdownListener {
    private final SimpleOrmSession simpleOrmSession;

    public SimpleOrmSessionShutdownListener(SimpleOrmSession simpleOrmSession) {
        this.simpleOrmSession = simpleOrmSession;
    }

    @Override
    public void shutdown() {
        simpleOrmSession.close();
    }
}
