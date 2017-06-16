package org.visallo.web.routes.ontology;

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Optional;
import com.v5analytics.webster.annotations.Required;
import org.apache.commons.lang.StringUtils;
import org.vertexium.Authorizations;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.ontology.Concept;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.ontology.OntologyRepositoryBase;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.user.User;
import org.visallo.web.VisalloResponse;
import org.visallo.web.clientapi.model.ClientApiOntology;
import org.visallo.web.clientapi.model.ClientApiSuccess;
import org.visallo.web.clientapi.util.ClientApiConverter;
import org.visallo.web.clientapi.util.ObjectMapperFactory;
import org.visallo.web.parameterProviders.ActiveWorkspaceId;

public class OntologyConceptSave implements ParameterizedHandler {
    private final OntologyRepository ontologyRepository;
    private final WorkQueueRepository workQueueRepository;

    @Inject
    public OntologyConceptSave(
            final OntologyRepository ontologyRepository,
            final WorkQueueRepository workQueueRepository
    ) {
        this.ontologyRepository = ontologyRepository;
        this.workQueueRepository = workQueueRepository;
    }

    @Handle
    public ClientApiOntology.Concept handle(
            @Required(name = "displayName", allowEmpty = false) String displayName,
            @Optional(name = "iri", allowEmpty = false) String iri,
            @Optional(name = "parentConcept", allowEmpty = false) String parentConcept,
            @Optional(name = "glyphIconHref", allowEmpty = false) String glyphIconHref,
            @Optional(name = "color", allowEmpty = false) String color,
            @ActiveWorkspaceId String workspaceId,
            User user) throws Exception {

        Concept parent;
        if (parentConcept == null) {
            parent = ontologyRepository.getConceptByIRI(OntologyRepository.ENTITY_CONCEPT_IRI, user, workspaceId);
        } else {
            parent = ontologyRepository.getConceptByIRI(parentConcept, user, workspaceId);
        }
        if (iri == null) {
            iri = ontologyRepository.generateDynamicIri(Concept.class, displayName, workspaceId);
        }
        Concept concept = ontologyRepository.getOrCreateConcept(parent, iri, displayName, glyphIconHref, color, null, user, workspaceId);

        ontologyRepository.clearCache(workspaceId);
        workQueueRepository.pushOntologyConceptsChange(workspaceId, concept.getId());

        return concept.toClientApi();
    }

}
