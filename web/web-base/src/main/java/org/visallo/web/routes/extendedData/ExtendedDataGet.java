package org.visallo.web.routes.extendedData;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Optional;
import com.v5analytics.webster.annotations.Required;
import org.vertexium.Authorizations;
import org.vertexium.ElementType;
import org.vertexium.ExtendedDataRow;
import org.vertexium.Graph;
import org.visallo.core.util.ClientApiConverter;
import org.visallo.web.clientapi.model.ClientApiExtendedDataGetResponse;

public class ExtendedDataGet implements ParameterizedHandler {
    private final Graph graph;

    @Inject
    public ExtendedDataGet(Graph graph) {
        this.graph = graph;
    }

    @Handle
    public ClientApiExtendedDataGetResponse handle(
            @Required(name = "elementType") ElementType elementType,
            @Required(name = "elementId") String elementId,
            @Required(name = "tableName") String tableName,
            Authorizations authorizations
    ) throws Exception {
        Iterable<ExtendedDataRow> rows = graph.getExtendedData(elementType, elementId, tableName, authorizations);
        return new ClientApiExtendedDataGetResponse(ClientApiConverter.toClientApiExtendedDataRows(rows));
    }
}
