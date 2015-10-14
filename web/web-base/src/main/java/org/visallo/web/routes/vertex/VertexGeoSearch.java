package org.visallo.web.routes.vertex;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Required;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.Vertex;
import org.vertexium.query.GeoCompare;
import org.vertexium.type.GeoCircle;
import org.visallo.core.model.ontology.OntologyProperty;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.util.ClientApiConverter;
import org.visallo.web.clientapi.model.ClientApiElementSearchResponse;
import org.visallo.web.clientapi.model.PropertyType;
import org.visallo.web.parameterProviders.ActiveWorkspaceId;

public class VertexGeoSearch implements ParameterizedHandler {
    private final Graph graph;
    private final OntologyRepository ontologyRepository;

    @Inject
    public VertexGeoSearch(
            final Graph graph,
            final OntologyRepository ontologyRepository
    ) {
        this.graph = graph;
        this.ontologyRepository = ontologyRepository;
    }

    @Handle
    public ClientApiElementSearchResponse handle(
            @Required(name = "lat") double latitude,
            @Required(name = "lon") double longitude,
            @Required(name = "radius") double radius,
            @ActiveWorkspaceId String workspaceId,
            Authorizations authorizations
    ) throws Exception {
        ClientApiElementSearchResponse results = new ClientApiElementSearchResponse();

        for (OntologyProperty property : this.ontologyRepository.getProperties()) {
            if (property.getDataType() != PropertyType.GEO_LOCATION) {
                continue;
            }

            Iterable<Vertex> vertices = graph.query(authorizations).
                    has(property.getTitle(), GeoCompare.WITHIN, new GeoCircle(latitude, longitude, radius)).
                    vertices();
            for (Vertex vertex : vertices) {
                results.getElements().add(ClientApiConverter.toClientApiVertex(vertex, workspaceId, authorizations));
            }
        }

        return results;
    }
}
