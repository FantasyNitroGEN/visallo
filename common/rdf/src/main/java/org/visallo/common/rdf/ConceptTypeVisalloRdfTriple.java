package org.visallo.common.rdf;

import org.vertexium.ElementType;
import org.vertexium.Visibility;

public class ConceptTypeVisalloRdfTriple extends ElementVisalloRdfTriple {
    private final String conceptType;

    public ConceptTypeVisalloRdfTriple(
            String vertexId,
            Visibility vertexVisibility,
            String vertexVisibilitySource,
            String conceptType
    ) {
        super(ElementType.VERTEX, vertexId, vertexVisibility, vertexVisibilitySource);
        this.conceptType = conceptType;
    }

    public String getConceptType() {
        return conceptType;
    }
}
