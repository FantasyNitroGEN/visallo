package org.visallo.common.rdf;

import org.vertexium.Visibility;

public abstract class PropertyVisalloRdfTriple extends VertexVisalloRdfTriple {
    private final String propertyKey;
    private final String propertyName;
    private final Visibility propertyVisibility;
    private final String propertyVisibilitySource;
    private final Object value;

    public PropertyVisalloRdfTriple(
            String vertexId,
            Visibility vertexVisibility,
            String vertexVisibilitySource,
            String propertyKey,
            String propertyName,
            Visibility propertyVisibility,
            String propertyVisibilitySource,
            Object value
    ) {
        super(vertexId, vertexVisibility, vertexVisibilitySource);
        this.propertyKey = propertyKey;
        this.propertyName = propertyName;
        this.propertyVisibility = propertyVisibility;
        this.propertyVisibilitySource = propertyVisibilitySource;
        this.value = value;
    }

    public String getPropertyKey() {
        return propertyKey;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public Visibility getPropertyVisibility() {
        return propertyVisibility;
    }

    public String getPropertyVisibilitySource() {
        return propertyVisibilitySource;
    }

    public Object getValue() {
        return value;
    }
}
