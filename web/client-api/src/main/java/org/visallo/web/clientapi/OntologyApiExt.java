package org.visallo.web.clientapi;

import org.visallo.web.clientapi.codegen.ApiException;
import org.visallo.web.clientapi.model.ClientApiOntology;
import org.visallo.web.clientapi.codegen.OntologyApi;

public class OntologyApiExt extends OntologyApi {
    private ClientApiOntology ontology;

    public ClientApiOntology.Concept getConcept(String conceptIri) throws ApiException {
        if (ontology == null) {
            ontology = get();
        }
        for (ClientApiOntology.Concept concept : ontology.getConcepts()) {
            if (concept.getId().equals(conceptIri)) {
                return concept;
            }
        }
        return null;
    }
}
