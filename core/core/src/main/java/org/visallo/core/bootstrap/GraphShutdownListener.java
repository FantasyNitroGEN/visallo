package org.visallo.core.bootstrap;

import org.vertexium.Graph;
import org.visallo.core.util.ShutdownListener;

public class GraphShutdownListener implements ShutdownListener {
    private final Graph graph;

    public GraphShutdownListener(Graph graph) {
        this.graph = graph;
    }

    @Override
    public void shutdown() {
        this.graph.shutdown();
    }
}
