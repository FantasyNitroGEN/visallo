package org.visallo.web.product.graph;

import com.google.inject.Inject;
import org.json.JSONObject;
import org.vertexium.Edge;
import org.vertexium.TextIndexHint;
import org.vertexium.Visibility;
import org.visallo.core.model.graph.ElementUpdateContext;
import org.visallo.core.model.ontology.OntologyPropertyDefinition;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.ontology.Relationship;
import org.visallo.core.model.properties.types.JsonSingleValueVisalloProperty;
import org.visallo.core.model.user.AuthorizationRepository;
import org.visallo.core.model.workspace.product.WorkProductElements;
import org.visallo.web.clientapi.model.PropertyType;

import java.util.ArrayList;

import static com.google.common.base.Preconditions.checkNotNull;

public class GraphWorkProduct extends WorkProductElements {
    public static final JsonSingleValueVisalloProperty ENTITY_POSITION = new JsonSingleValueVisalloProperty("http://visallo.org/workspace/product/graph#entityPosition");

    @Inject
    public GraphWorkProduct(OntologyRepository ontologyRepository, AuthorizationRepository authorizationRepository) {
        super(ontologyRepository, authorizationRepository);
        addEdgePositionToOntology(ontologyRepository);
    }

    private void addEdgePositionToOntology(OntologyRepository ontologyRepository) {
        Relationship productToEntityRelationship = ontologyRepository.getRelationshipByIRI(WORKSPACE_PRODUCT_TO_ENTITY_RELATIONSHIP_IRI);
        checkNotNull(productToEntityRelationship, "Cannot find relationship: " + WORKSPACE_PRODUCT_TO_ENTITY_RELATIONSHIP_IRI);
        OntologyPropertyDefinition propertyDefinition = new OntologyPropertyDefinition(
                new ArrayList<>(),
                ENTITY_POSITION.getPropertyName(),
                "Entity Position",
                PropertyType.STRING
        );
        propertyDefinition.setTextIndexHints(TextIndexHint.NONE);
        propertyDefinition.getRelationships().add(productToEntityRelationship);
        ontologyRepository.getOrCreateProperty(propertyDefinition);
    }

    @Override
    protected void updateProductEdge(ElementUpdateContext<Edge> elemCtx, JSONObject update, Visibility visibility) {
        ENTITY_POSITION.updateProperty(elemCtx, update, visibility);
    }

    protected void setEdgeJson(Edge propertyVertexEdge, JSONObject vertex) {
        JSONObject position = ENTITY_POSITION.getPropertyValue(propertyVertexEdge);
        if (position == null) {
            position = new JSONObject();
            position.put("x", 0);
            position.put("y", 0);
        }
        vertex.put("pos", position);
    }

}
