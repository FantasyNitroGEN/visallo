package org.visallo.common.rdf;

import org.vertexium.Authorizations;
import org.vertexium.ElementType;
import org.vertexium.VertexBuilder;
import org.vertexium.Visibility;

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

    @Override
    public ImportContext createImportContext(
            ImportContext ctx,
            RdfTripleImportHelper rdfTripleImportHelper,
            Authorizations authorizations
    ) {
        Visibility elementVisibility = rdfTripleImportHelper.getVisibility(getElementVisibilitySource());
        VertexBuilder m = rdfTripleImportHelper.getGraph().prepareVertex(getElementId(), elementVisibility);
        return new ImportContext(getElementId(), m);
    }
}
