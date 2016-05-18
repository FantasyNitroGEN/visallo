package org.visallo.common.rdf;

import org.vertexium.ElementType;
import org.vertexium.Visibility;

public class SetPropertyVisalloRdfTriple extends PropertyVisalloRdfTriple {
    public SetPropertyVisalloRdfTriple(
            ElementType elementType,
            String elementId,
            Visibility elementVisibility,
            String elementVisibilitySource,
            String propertyKey,
            String propertyName,
            Visibility propertyVisibility,
            String propertyVisibilitySource,
            Object value
    ) {
        super(
                elementType,
                elementId,
                elementVisibility,
                elementVisibilitySource,
                propertyKey,
                propertyName,
                propertyVisibility,
                propertyVisibilitySource,
                value
        );
    }
}
