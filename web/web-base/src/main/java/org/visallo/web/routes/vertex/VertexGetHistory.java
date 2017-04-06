package org.visallo.web.routes.vertex;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Optional;
import com.v5analytics.webster.annotations.Required;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.HistoricalPropertyValue;
import org.vertexium.Vertex;
import org.visallo.core.exception.VisalloResourceNotFoundException;
import org.visallo.core.util.ClientApiConverter;
import org.visallo.web.clientapi.model.ClientApiHistoricalPropertyResults;

import java.util.Locale;
import java.util.ResourceBundle;

public class VertexGetHistory implements ParameterizedHandler {
    private Graph graph;

    @Inject
    public VertexGetHistory(Graph graph) {
        this.graph = graph;
    }

    @Handle
    public ClientApiHistoricalPropertyResults handle(
            @Required(name = "graphVertexId") String graphVertexId,
            @Optional(name = "startTime") Long startTime,
            @Optional(name = "endTime") Long endTime,
            @Optional(name = "withVisibility") Boolean withVisibility,
            Locale locale,
            ResourceBundle resourceBundle,
            Authorizations authorizations
    ) throws Exception {
        Vertex vertex = graph.getVertex(graphVertexId, authorizations);
        if (vertex == null) {
            throw new VisalloResourceNotFoundException(String.format("vertex %s not found", graphVertexId));
        }

        Iterable<HistoricalPropertyValue> historicalPropertyValues = vertex.getHistoricalPropertyValues(
                startTime,
                endTime,
                authorizations
        );
        return ClientApiConverter.toClientApi(historicalPropertyValues, locale, resourceBundle, withVisibility);
    }
}
