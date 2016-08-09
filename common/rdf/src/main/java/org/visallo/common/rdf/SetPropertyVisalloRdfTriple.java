package org.visallo.common.rdf;

import org.vertexium.ElementType;

public class SetPropertyVisalloRdfTriple extends PropertyVisalloRdfTriple {
    public SetPropertyVisalloRdfTriple(
            ElementType elementType,
            String elementId,
            String elementVisibilitySource,
            String propertyKey,
            String propertyName,
            String propertyVisibilitySource,
            Object value
    ) {
        super(
                elementType,
                elementId,
                elementVisibilitySource,
                propertyKey,
                propertyName,
                propertyVisibilitySource,
                value
        );
    }

    @Override
    public String toString() {
        String warning = "\"Unhandled value type " + getValue().getClass().getName() + " to convert to RDF string\"";
        String value = getValueRdfString();
        return String.format("%s<%s> <%s> %s",
                value == null ? "# " : "",
                getElementRdfString(),
                getPropertyRdfString(),
                value == null ? warning : value);
    }
}
