package org.visallo.vertexium.model.ontology;

import org.vertexium.Authorizations;
import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.ontology.*;

public class VertexiumOntologyRepositoryTest extends OntologyRepositoryTestBase {
    private VertexiumOntologyRepository ontologyRepository;

    @Override
    protected OntologyRepository getOntologyRepository() {
        if (ontologyRepository != null) {
            return ontologyRepository;
        }
        try {
            ontologyRepository = new VertexiumOntologyRepository(
                    getGraph(),
                    getGraphRepository(),
                    getConfiguration(),
                    getGraphAuthorizationRepository(),
                    getLockRepository()
            ) {
                @Override
                public void loadOntologies(Configuration config, Authorizations authorizations) throws Exception {
                    Concept rootConcept = getOrCreateConcept(null, ROOT_CONCEPT_IRI, "root", null);
                    getOrCreateConcept(rootConcept, ENTITY_CONCEPT_IRI, "thing", null);
                    clearCache();
                }
            };
        } catch (Exception ex) {
            throw new VisalloException("Could not create ontology repository", ex);
        }
        return ontologyRepository;
    }
}

