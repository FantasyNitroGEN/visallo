package org.visallo.core.trace;

import com.google.common.base.Joiner;

import java.io.Closeable;

public abstract class TraceSpan implements Closeable {
    public abstract TraceSpan data(String key, String value);

    public TraceSpan data(String key, String[] values) {
        if (!Trace.isEnabled()) {
            return this;
        }
        return data(key, Joiner.on(", ").join(values));
    }

    @Override
    public abstract void close();
}
