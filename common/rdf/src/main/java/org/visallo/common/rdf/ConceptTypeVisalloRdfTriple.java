package org.visallo.common.rdf;

import org.vertexium.ElementType;

public class ConceptTypeVisalloRdfTriple extends ElementVisalloRdfTriple {
    private final String conceptType;

    public ConceptTypeVisalloRdfTriple(
            String vertexId,
            String vertexVisibilitySource,
            String conceptType
    ) {
        super(ElementType.VERTEX, vertexId, vertexVisibilitySource);
        this.conceptType = conceptType;
    }

    public String getConceptType() {
        return conceptType;
    }

    @Override
    public String toString() {
        return String.format("<%s> <%s> <%s>", getElementRdfString(), VisalloRdfTriple.LABEL_CONCEPT_TYPE, getConceptType());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ConceptTypeVisalloRdfTriple that = (ConceptTypeVisalloRdfTriple) o;

        if (!conceptType.equals(that.conceptType)) {
            return false;
        }

        return super.equals(o);
    }
}
