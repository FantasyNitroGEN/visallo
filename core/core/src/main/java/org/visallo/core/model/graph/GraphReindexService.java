package org.visallo.core.model.graph;

import com.google.inject.Inject;
import org.vertexium.*;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class GraphReindexService {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(GraphReindexService.class);
    private final GraphWithSearchIndex graph;

    @Inject
    public GraphReindexService(Graph graph) {
        if (!(graph instanceof GraphWithSearchIndex)) {
            throw new VisalloException("Reindex plugin cannot be enabled when the graph does not implement " + GraphWithSearchIndex.class.getName());
        }
        this.graph = (GraphWithSearchIndex) graph;
    }

    public void reindexVertices(int batchSize, Authorizations authorizations) {
        Iterable<Vertex> vertices = graph.getVertices(authorizations);
        reindexVertices(vertices, batchSize, authorizations);
    }

    public void reindexVertices(Iterable<Vertex> vertices, int batchSize, Authorizations authorizations) {
        long count = 0;
        List<Element> batch = new ArrayList<>(batchSize);
        for (Vertex vertex : vertices) {
            batch.add(vertex);
            if (batch.size() == batchSize) {
                submitBatchOfElementsToSearchIndex(ElementType.VERTEX, count, batch, authorizations);
            }
            count++;
        }
        if (batch.size() > 0) {
            submitBatchOfElementsToSearchIndex(ElementType.VERTEX, count, batch, authorizations);
        }
    }

    public void reindexEdges(int batchSize, Authorizations authorizations) {
        Iterable<Edge> edges = graph.getEdges(authorizations);
        reindexEdges(edges, batchSize, authorizations);
    }

    public void reindexEdges(Iterable<Edge> edges, int batchSize, Authorizations authorizations) {
        long count = 0;
        List<Element> batch = new ArrayList<>(batchSize);
        for (Edge edge : edges) {
            batch.add(edge);
            if (batch.size() == batchSize) {
                submitBatchOfElementsToSearchIndex(ElementType.EDGE, count, batch, authorizations);
            }
            count++;
        }
        if (batch.size() > 0) {
            submitBatchOfElementsToSearchIndex(ElementType.EDGE, count, batch, authorizations);
        }
    }

    private void submitBatchOfElementsToSearchIndex(ElementType elementType, long endIndex, List<Element> batch, Authorizations authorizations) {
        LOGGER.debug("indexing %s %d-%d", elementType, endIndex - batch.size(), endIndex);
        graph.getSearchIndex().addElements(graph, batch, authorizations);
        batch.clear();
    }
}
