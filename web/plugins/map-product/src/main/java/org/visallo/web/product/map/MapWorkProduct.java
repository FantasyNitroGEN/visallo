package org.visallo.web.product.map;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import org.json.JSONArray;
import org.json.JSONObject;
import org.vertexium.*;
import org.visallo.core.model.graph.ElementUpdateContext;
import org.visallo.core.model.graph.GraphUpdateContext;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.user.AuthorizationRepository;
import org.visallo.core.model.workspace.WorkspaceProperties;
import org.visallo.core.model.workspace.product.WorkProductElements;
import org.visallo.core.util.JSONUtil;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

import java.util.List;

public class MapWorkProduct extends WorkProductElements {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(MapWorkProduct.class);

    @Inject
    public MapWorkProduct(OntologyRepository ontologyRepository, AuthorizationRepository authorizationRepository) {
        super(ontologyRepository, authorizationRepository);
    }

    @Override
    protected void updateProductEdge(ElementUpdateContext<Edge> elemCtx, JSONObject update, Visibility visibility) {
    }

    protected void setEdgeJson(Edge propertyVertexEdge, JSONObject vertex) {
    }

    public void updateVertices(
            GraphUpdateContext ctx,
            Vertex productVertex,
            JSONObject updateVertices,
            Visibility visibility
    ) {
        if (updateVertices != null) {
            @SuppressWarnings("unchecked")
            List<String> vertexIds = Lists.newArrayList(updateVertices.keys());
            for (String id : vertexIds) {
                JSONObject updateData = updateVertices.getJSONObject(id);
                String edgeId = getEdgeId(productVertex.getId(), id);
                EdgeBuilderByVertexId edgeBuilder = ctx.getGraph().prepareEdge(
                        edgeId,
                        productVertex.getId(),
                        id,
                        WorkspaceProperties.PRODUCT_TO_ENTITY_RELATIONSHIP_IRI,
                        visibility
                );
                ctx.update(edgeBuilder, elemCtx -> updateProductEdge(elemCtx, updateData, visibility));
            }
        }
    }
    public void removeVertices(
            GraphUpdateContext ctx,
            Vertex productVertex,
            JSONArray removeVertices,
            Authorizations authorizations
    ) {
        if (removeVertices != null) {
            JSONUtil.toList(removeVertices)
                    .forEach(id -> ctx.getGraph().softDeleteEdge(getEdgeId(productVertex.getId(), (String) id), authorizations));
        }
    }
}
