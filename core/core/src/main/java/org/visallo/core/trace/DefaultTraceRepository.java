package org.visallo.core.trace;

import java.util.Map;

public class DefaultTraceRepository extends TraceRepository {
    @Override
    public TraceSpan on(String description, Map<String, String> data) {
        return NullTraceSpan.INSTANCE;
    }

    @Override
    public void off() {
    }

    @Override
    public TraceSpan start(String description) {
        return NullTraceSpan.INSTANCE;
    }

    @Override
    public boolean isEnabled() {
        return false;
    }
}
