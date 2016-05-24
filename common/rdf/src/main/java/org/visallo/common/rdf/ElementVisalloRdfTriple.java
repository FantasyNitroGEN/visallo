package org.visallo.common.rdf;

import com.google.common.base.Strings;
import org.vertexium.ElementType;

public abstract class ElementVisalloRdfTriple extends VisalloRdfTriple {
    private final ElementType elementType;
    private final String elementId;
    private final String elementVisibilitySource;

    protected ElementVisalloRdfTriple(
            ElementType elementType,
            String elementId,
            String elementVisibilitySource
    ) {
        this.elementType = elementType;
        this.elementId = elementId;
        this.elementVisibilitySource = elementVisibilitySource;
    }

    public ElementType getElementType() {
        return elementType;
    }

    public String getElementId() {
        return elementId;
    }

    public String getElementVisibilitySource() {
        return elementVisibilitySource;
    }

    protected String getElementRdfString() {
        String result = String.format(
                "%s%s",
                getElementType() == ElementType.VERTEX ? "" : "EDGE:",
                getElementId()
        );
        if (!Strings.isNullOrEmpty(getElementVisibilitySource())) {
            result += String.format("[%s]", getElementVisibilitySource());
        }
        return result;
    }

    @Override
    public int hashCode() {
        return getElementId().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ElementVisalloRdfTriple that = (ElementVisalloRdfTriple) o;

        if (elementType != that.elementType) {
            return false;
        }
        if (elementId != null ? !elementId.equals(that.elementId) : that.elementId != null) {
            return false;
        }
        if (elementVisibilitySource != null ? !elementVisibilitySource.equals(that.elementVisibilitySource) : that.elementVisibilitySource != null) {
            return false;
        }

        return true;
    }
}
