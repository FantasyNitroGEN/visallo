package org.visallo.vertexium.model.ontology;

import org.vertexium.Authorizations;
import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.ontology.Concept;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.ontology.OntologyRepositoryTestBase;

public class InMemoryOntologyRepositoryTest extends OntologyRepositoryTestBase {
    private InMemoryOntologyRepository ontologyRepository;

    @Override
    protected OntologyRepository getOntologyRepository() {
        if (ontologyRepository != null) {
            return ontologyRepository;
        }
        try {
            ontologyRepository = new InMemoryOntologyRepository(
                    getGraph(),
                    getConfiguration(),
                    getLockRepository()
            ) {
                @Override
                public void loadOntologies(Configuration config, Authorizations authorizations) throws Exception {
                    Concept rootConcept = getOrCreateConcept(null, ROOT_CONCEPT_IRI, "root", null);
                    getOrCreateConcept(rootConcept, ENTITY_CONCEPT_IRI, "thing", null);
                }
            };
        } catch (Exception ex) {
            throw new VisalloException("Could not create ontology repository", ex);
        }
        return ontologyRepository;
    }
}
