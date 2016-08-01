package org.visallo.common.rdf;

import org.vertexium.Authorizations;
import org.vertexium.Element;
import org.vertexium.mutation.ElementMutation;

class ImportContext {
    private final ElementMutation m;
    private final String elementId;

    ImportContext(String elementId, ElementMutation m) {
        this.m = m;
        this.elementId = elementId;
    }

    public Element save(Authorizations authorizations) {
        return getElementMutation().save(authorizations);
    }

    public boolean isNewElement(VisalloRdfTriple triple) {
        return !this.elementId.equals(triple.getElementId());
    }

    public ElementMutation getElementMutation() {
        return m;
    }
}
