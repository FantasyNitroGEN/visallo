package org.visallo.core.trace;

import java.util.Map;

public abstract class TraceRepository {
    public abstract TraceSpan on(String description, Map<String, String> data);

    public abstract void off();

    public abstract TraceSpan start(String description);

    public abstract boolean isEnabled();
}
