package org.visallo.web.product.graph;


import org.visallo.core.model.properties.types.JsonArraySingleValueVisalloProperty;
import org.visallo.core.model.properties.types.JsonSingleValueVisalloProperty;
import org.visallo.core.model.properties.types.StringSingleValueVisalloProperty;

public class GraphProductOntology {
    public static final String IRI = "http://visallo.org/workspace/product/graph";
    public static final String CONCEPT_TYPE_COMPOUND_NODE = "http://visallo.org/workspace/product/graph#compoundNode";

    public static final JsonSingleValueVisalloProperty ENTITY_POSITION = new JsonSingleValueVisalloProperty("http://visallo.org/workspace/product/graph#entityPosition");
    public static final StringSingleValueVisalloProperty PARENT_NODE = new StringSingleValueVisalloProperty("http://visallo.org/workspace/product/graph#parentNode");
    public static final JsonArraySingleValueVisalloProperty NODE_CHILDREN = new JsonArraySingleValueVisalloProperty("http://visallo.org/workspace/product/graph#nodeChildren");
}
