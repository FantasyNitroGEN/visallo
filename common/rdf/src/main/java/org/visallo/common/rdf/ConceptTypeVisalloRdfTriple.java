package org.visallo.common.rdf;

import org.vertexium.Visibility;
import org.visallo.core.exception.VisalloException;

public class ConceptTypeVisalloRdfTriple extends VertexVisalloRdfTriple {
    private final String conceptType;

    public ConceptTypeVisalloRdfTriple(
            String vertexId,
            Visibility vertexVisibility,
            String vertexVisibilitySource,
            String conceptType
    ) {
        super(vertexId, vertexVisibility, vertexVisibilitySource);
        this.conceptType = conceptType;
    }

    public String getConceptType() {
        return conceptType;
    }
}
