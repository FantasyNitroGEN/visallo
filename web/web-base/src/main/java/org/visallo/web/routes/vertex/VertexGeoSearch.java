package org.visallo.web.routes.vertex;

import com.google.inject.Inject;
import org.visallo.core.config.Configuration;
import org.visallo.core.model.ontology.OntologyProperty;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.user.User;
import org.visallo.core.util.ClientApiConverter;
import com.v5analytics.webster.HandlerChain;
import org.visallo.web.BaseRequestHandler;
import org.visallo.web.clientapi.model.ClientApiVertexSearchResponse;
import org.visallo.web.clientapi.model.PropertyType;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.Vertex;
import org.vertexium.query.GeoCompare;
import org.vertexium.type.GeoCircle;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class VertexGeoSearch extends BaseRequestHandler {
    private final Graph graph;
    private final OntologyRepository ontologyRepository;

    @Inject
    public VertexGeoSearch(
            final Graph graph,
            final UserRepository userRepository,
            final WorkspaceRepository workspaceRepository,
            final Configuration configuration,
            final OntologyRepository ontologyRepository) {
        super(userRepository, workspaceRepository, configuration);
        this.graph = graph;
        this.ontologyRepository = ontologyRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final double latitude = getRequiredParameterAsDouble(request, "lat");
        final double longitude = getRequiredParameterAsDouble(request, "lon");
        final double radius = getRequiredParameterAsDouble(request, "radius");

        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);
        String workspaceId = getActiveWorkspaceId(request);

        ClientApiVertexSearchResponse results = new ClientApiVertexSearchResponse();

        for (OntologyProperty property : this.ontologyRepository.getProperties()) {
            if (property.getDataType() != PropertyType.GEO_LOCATION) {
                continue;
            }

            Iterable<Vertex> vertices = graph.query(authorizations).
                    has(property.getTitle(), GeoCompare.WITHIN, new GeoCircle(latitude, longitude, radius)).
                    vertices();
            for (Vertex vertex : vertices) {
                results.getVertices().add(ClientApiConverter.toClientApiVertex(vertex, workspaceId, authorizations));
            }
        }

        respondWithClientApiObject(response, results);
    }
}
