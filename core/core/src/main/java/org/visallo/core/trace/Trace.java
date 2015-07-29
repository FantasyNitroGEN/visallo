package org.visallo.core.trace;

import org.visallo.core.bootstrap.InjectHelper;

import java.util.HashMap;
import java.util.Map;

/**
 * Trace.on("start some process");
 * ...
 * TraceSpan trace = Trace.start("subprocess");
 * trace.data("data", "some data");
 * try {
 * ...
 * } finally {
 * trace.close();
 * }
 * ...
 * Trace.off();
 */
public class Trace {
    private static TraceRepository traceRepository;

    public static void on(String description) {
        getTraceRepository().on(description, new HashMap<String, String>());
    }

    public static TraceSpan on(String description, Map<String, String> data) {
        return getTraceRepository().on(description, data);
    }

    public static void off() {
        getTraceRepository().off();
    }

    public static TraceSpan start(String description) {
        return getTraceRepository().start(description);
    }

    public static boolean isEnabled() {
        return getTraceRepository().isEnabled();
    }

    private static TraceRepository getTraceRepository() {
        if (traceRepository == null) {
            traceRepository = InjectHelper.getInstance(TraceRepository.class);
        }
        return traceRepository;
    }
}
