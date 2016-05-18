package org.visallo.common.rdf;

import org.vertexium.ElementType;
import org.vertexium.Visibility;

public class SetMetadataVisalloRdfTriple extends PropertyVisalloRdfTriple {
    private final String metadataName;
    private final Visibility metadataVisibility;

    public SetMetadataVisalloRdfTriple(
            ElementType elementType,
            String elementId,
            Visibility elementVisibility,
            String elementVisibilitySource,
            String propertyKey,
            String propertyName,
            Visibility propertyVisibility,
            String propertyVisibilitySource,
            String metadataName,
            Visibility metadataVisibility,
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
        this.metadataName = metadataName;
        this.metadataVisibility = metadataVisibility;
    }

    public String getMetadataName() {
        return metadataName;
    }

    public Visibility getMetadataVisibility() {
        return metadataVisibility;
    }
}
