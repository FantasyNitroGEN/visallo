package org.visallo.vertexium.trace;

import com.google.inject.Inject;
import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.trace.DistributedTrace;
import org.apache.accumulo.fate.zookeeper.ZooReader;
import org.apache.accumulo.trace.instrument.Span;
import org.apache.accumulo.trace.instrument.Trace;
import org.vertexium.Graph;
import org.vertexium.VertexiumException;
import org.vertexium.accumulo.AccumuloGraph;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.trace.TraceRepository;
import org.visallo.core.trace.TraceSpan;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

import java.util.Map;

public class AccumuloTraceRepository extends TraceRepository {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(AccumuloTraceRepository.class);
    private static boolean distributedTraceEnabled;
    private final Connector connector;

    @Inject
    public AccumuloTraceRepository(Graph graph) {
        if (graph instanceof AccumuloGraph) {
            connector = ((AccumuloGraph) graph).getConnector();
        } else {
            throw new VisalloException("You cannot use the " + AccumuloTraceRepository.class.getName() + " unless you are using " + AccumuloGraph.class.getName());
        }
    }

    @Override
    public TraceSpan on(String description, Map<String, String> data) {
        if (!distributedTraceEnabled) {
            try {
                DistributedTrace.enable(connector.getInstance(), new ZooReader(connector.getInstance().getZooKeepers(), 10000), "visallo", null);
                distributedTraceEnabled = true;
            } catch (Exception e) {
                throw new VertexiumException("Could not enable DistributedTrace", e);
            }
        }
        if (Trace.isTracing()) {
            throw new VertexiumException("Trace already running");
        }
        Span span = Trace.on(description);
        for (Map.Entry<String, String> dataEntry : data.entrySet()) {
            span.data(dataEntry.getKey(), dataEntry.getValue());
        }

        LOGGER.info("Started trace '%s'", description);
        return wrapSpan(span);
    }

    @Override
    public void off() {
        Trace.off();
    }

    @Override
    public TraceSpan start(String description) {
        final Span span = Trace.start(description);
        return wrapSpan(span);
    }

    private TraceSpan wrapSpan(final Span span) {
        return new TraceSpan() {
            @Override
            public TraceSpan data(String key, String value) {
                span.data(key, value);
                return this;
            }

            @Override
            public void close() {
                span.stop();
            }
        };
    }

    @Override
    public boolean isEnabled() {
        return Trace.isTracing();
    }
}
