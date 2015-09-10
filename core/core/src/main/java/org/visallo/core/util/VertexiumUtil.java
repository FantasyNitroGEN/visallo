package org.visallo.core.util;

import org.vertexium.Direction;
import org.vertexium.Edge;
import org.vertexium.Vertex;

import java.util.*;

public class VertexiumUtil {
    public static Map<String, Vertex> verticesToMapById(Iterable<Vertex> vertices) {
        Map<String, Vertex> results = new HashMap<>();
        for (Vertex vertex : vertices) {
            results.put(vertex.getId(), vertex);
        }
        return results;
    }

    public static Set<String> getAllVertexIdsOnEdges(List<Edge> edges) {
        Set<String> results = new HashSet<>();
        for (Edge edge : edges) {
            results.add(edge.getVertexId(Direction.IN));
            results.add(edge.getVertexId(Direction.OUT));
        }
        return results;
    }
}
