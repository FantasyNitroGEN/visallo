package org.visallo.common.rdf;

import org.vertexium.Visibility;

public class SetPropertyVisalloRdfTriple extends PropertyVisalloRdfTriple {
    public SetPropertyVisalloRdfTriple(
            String vertexId,
            Visibility vertexVisibility,
            String vertexVisibilitySource,
            String propertyKey,
            String propertyName,
            Visibility propertyVisibility,
            String propertyVisibilitySource,
            Object value
    ) {
        super(
                vertexId,
                vertexVisibility,
                vertexVisibilitySource,
                propertyKey,
                propertyName,
                propertyVisibility,
                propertyVisibilitySource,
                value
        );
    }
}
