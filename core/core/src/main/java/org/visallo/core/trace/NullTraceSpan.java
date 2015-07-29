package org.visallo.core.trace;

public class NullTraceSpan extends TraceSpan {
    public static final TraceSpan INSTANCE = new NullTraceSpan();

    @Override
    public TraceSpan data(String key, String value) {
        return this;
    }

    @Override
    public void close() {

    }
}
