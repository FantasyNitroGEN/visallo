package org.visallo.common.rdf;

import org.vertexium.Visibility;

public class SetMetadataVisalloRdfTriple extends PropertyVisalloRdfTriple {
    private final String metadataName;
    private final Visibility metadataVisibility;

    public SetMetadataVisalloRdfTriple(
            String vertexId,
            Visibility vertexVisibility,
            String vertexVisibilitySource,
            String propertyKey,
            String propertyName,
            Visibility propertyVisibility,
            String propertyVisibilitySource,
            String metadataName,
            Visibility metadataVisibility,
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
