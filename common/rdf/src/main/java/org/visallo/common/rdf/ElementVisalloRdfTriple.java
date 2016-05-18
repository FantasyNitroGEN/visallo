package org.visallo.common.rdf;

import org.vertexium.ElementType;
import org.vertexium.Visibility;

public class ElementVisalloRdfTriple extends VisalloRdfTriple {
    private final ElementType elementType;
    private final String elementId;
    private final Visibility elementVisibility;
    private final String elementVisibilitySource;

    protected ElementVisalloRdfTriple(
            ElementType elementType,
            String elementId,
            Visibility elementVisibility,
            String elementVisibilitySource
    ) {
        this.elementType = elementType;
        this.elementId = elementId;
        this.elementVisibility = elementVisibility;
        this.elementVisibilitySource = elementVisibilitySource;
    }

    public ElementType getElementType() {
        return elementType;
    }

    public String getElementId() {
        return elementId;
    }

    public Visibility getElementVisibility() {
        return elementVisibility;
    }

    public String getElementVisibilitySource() {
        return elementVisibilitySource;
    }
}
