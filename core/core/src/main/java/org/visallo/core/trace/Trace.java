package org.visallo.core.trace;

import org.visallo.core.bootstrap.InjectHelper;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

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
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(Trace.class);
    private static TraceRepository traceRepository;

    public static void on(String description) {
        getTraceRepository().on(description, new HashMap<>());
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
            try {
                traceRepository = InjectHelper.getInstance(TraceRepository.class);
            } catch (VisalloException e) {
                LOGGER.warn("TraceRepository not found through injection. Using no-op DefaultTraceRepository");
                traceRepository = new DefaultTraceRepository();
            }
        }
        return traceRepository;
    }
}
