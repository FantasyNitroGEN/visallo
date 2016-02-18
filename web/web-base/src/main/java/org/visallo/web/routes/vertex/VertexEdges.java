package org.visallo.web.routes.vertex;

import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Optional;
import com.v5analytics.webster.annotations.Required;
import org.vertexium.*;
import org.visallo.core.exception.VisalloResourceNotFoundException;
import org.visallo.core.trace.Trace;
import org.visallo.core.trace.TraceSpan;
import org.visallo.core.util.ClientApiConverter;
import org.visallo.web.clientapi.model.ClientApiVertex;
import org.visallo.web.clientapi.model.ClientApiVertexEdges;
import org.visallo.web.parameterProviders.ActiveWorkspaceId;

import java.util.List;
import java.util.Map;

public class VertexEdges implements ParameterizedHandler {
    private final Graph graph;

    @Inject
    public VertexEdges(final Graph graph) {
        this.graph = graph;
    }

    @Handle
    public ClientApiVertexEdges handle(
            @Required(name = "graphVertexId") String graphVertexId,
            @Optional(name = "offset", defaultValue = "0") int offset,
            @Optional(name = "size", defaultValue = "25") int size,
            @Optional(name = "edgeLabel") String edgeLabel,
            @Optional(name = "relatedVertexId") String relatedVertexId,
            @Optional(name = "direction", defaultValue = "BOTH") String directionStr,
            @ActiveWorkspaceId String workspaceId,
            Authorizations authorizations
    ) throws Exception {
        Vertex vertex;
        Vertex relatedVertex = null;
        try (TraceSpan trace = Trace.start("getOriginalVertex").data("graphVertexId", graphVertexId)) {
            vertex = graph.getVertex(graphVertexId, authorizations);
            if (vertex == null) {
                throw new VisalloResourceNotFoundException("Could not find vertex: " + graphVertexId);
            }
        }

        if (relatedVertexId != null) {
            relatedVertex = graph.getVertex(relatedVertexId, authorizations);
            if (relatedVertex == null) {
                throw new VisalloResourceNotFoundException("Could not find related vertex: " + relatedVertexId);
            }
        }

        Direction direction = Direction.valueOf(directionStr.toUpperCase());

        List<String> edgeIds = loadEdgeIds(edgeLabel, vertex, relatedVertex, direction, authorizations);
        int totalEdgeCount = edgeIds.size();

        ClientApiVertexEdges result = new ClientApiVertexEdges();

        edgeIds = edgeIds.subList(Math.min(edgeIds.size(), offset), Math.min(edgeIds.size(), offset + size));
        List<Edge> edges = loadEdges(edgeIds, authorizations);
        Map<String, Vertex> vertices;
        try (TraceSpan trace = Trace.start("getConnectedVertices").data("graphVertexId", vertex.getId())) {
            vertices = getVertices(vertex.getId(), edges, authorizations);
        }

        for (Edge edge : edges) {
            String otherVertexId = relatedVertexId == null ? edge.getOtherVertexId(vertex.getId()) : relatedVertexId;
            Vertex otherVertex = relatedVertex == null ? vertices.get(otherVertexId) : relatedVertex;

            result.getRelationships().add(convertEdgeToClientApi(edge, otherVertexId, otherVertex, workspaceId, authorizations));
        }
        result.setTotalReferences(totalEdgeCount);

        return result;
    }

    /**
     * This is overridable so web plugins can modify the resulting set of edges.
     */
    protected List<String> loadEdgeIds(String edgeLabel, Vertex vertex, Vertex relatedVertex, Direction direction,
                                       Authorizations authorizations) {
        if (edgeLabel == null && relatedVertex == null) {
            return Lists.newArrayList(vertex.getEdgeIds(direction, authorizations));
        } else if (relatedVertex == null) {
            return Lists.newArrayList(vertex.getEdgeIds(direction, edgeLabel, authorizations));
        } else if (edgeLabel == null) {
            return Lists.newArrayList(vertex.getEdgeIds(relatedVertex, direction, authorizations));
        } else {
            return Lists.newArrayList(vertex.getEdgeIds(relatedVertex, direction, edgeLabel, authorizations));
        }
    }

    /**
     * This is overridable so web plugins can modify the resulting set of edges.
     */
    protected List<Edge> loadEdges(List<String> edgeIds, Authorizations authorizations) {
        return Lists.newArrayList(graph.getEdges(edgeIds, authorizations));
    }

    /**
     * This is overridable so web plugins can modify the resulting set of edges.
     */
    protected ClientApiVertexEdges.Edge convertEdgeToClientApi(Edge edge, String otherVertexId, Vertex otherVertex, String workspaceId, Authorizations authorizations) {
        ClientApiVertexEdges.Edge clientApiEdge = new ClientApiVertexEdges.Edge();
        clientApiEdge.setRelationship(ClientApiConverter.toClientApiEdge(edge, workspaceId));
        ClientApiVertex clientApiVertex;
        if (otherVertex == null) {
            clientApiVertex = new ClientApiVertex();
            clientApiVertex.setId(otherVertexId);
        } else {
            clientApiVertex = ClientApiConverter.toClientApiVertex(otherVertex, workspaceId, authorizations);
        }
        clientApiEdge.setVertex(clientApiVertex);

        return clientApiEdge;
    }

    private Map<String, Vertex> getVertices(final String myVertexId, List<Edge> edges, Authorizations authorizations) {
        Iterable<String> vertexIds = getOtherVertexIds(myVertexId, edges);
        Iterable<Vertex> vertices = graph.getVertices(vertexIds, ClientApiConverter.SEARCH_FETCH_HINTS, authorizations);
        vertices = Iterables.filter(vertices, Predicates.notNull());
        return Maps.uniqueIndex(vertices, new Function<Vertex, String>() {
            @Override
            public String apply(Vertex vertex) {
                return vertex.getId();
            }
        });
    }

    private Iterable<String> getOtherVertexIds(final String myVertexId, List<Edge> edges) {
        return Iterables.transform(
                edges,
                new Function<Edge, String>() {
                    @Override
                    public String apply(Edge edge) {
                        return edge.getOtherVertexId(myVertexId);
                    }
                });
    }
}
