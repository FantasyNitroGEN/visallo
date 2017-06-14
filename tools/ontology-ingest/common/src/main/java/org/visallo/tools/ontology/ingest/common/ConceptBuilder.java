package org.visallo.tools.ontology.ingest.common;

public abstract class ConceptBuilder extends EntityBuilder {

    public ConceptBuilder(final String id) {
        super(id);
    }

    public ConceptBuilder(final String id, final String visibility) {
        super(id, visibility);
    }
}
