package org.visallo.common.rdf;

import org.vertexium.ElementType;
import org.vertexium.Visibility;

public abstract class PropertyVisalloRdfTriple extends ElementVisalloRdfTriple {
    private final String propertyKey;
    private final String propertyName;
    private final Visibility propertyVisibility;
    private final String propertyVisibilitySource;
    private final Object value;

    public PropertyVisalloRdfTriple(
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
        super(elementType, elementId, elementVisibility, elementVisibilitySource);
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
