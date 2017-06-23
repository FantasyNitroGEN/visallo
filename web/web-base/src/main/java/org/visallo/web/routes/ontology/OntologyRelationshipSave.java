package org.visallo.web.routes.ontology;

import com.google.inject.Inject;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Optional;
import com.v5analytics.webster.annotations.Required;
import org.vertexium.Authorizations;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.ontology.*;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.user.User;
import org.visallo.web.clientapi.model.ClientApiOntology;
import org.visallo.web.parameterProviders.ActiveWorkspaceId;

import java.util.List;

public class OntologyRelationshipSave extends OntologyBase {
    private final OntologyRepository ontologyRepository;
    private final WorkQueueRepository workQueueRepository;

    @Inject
    public OntologyRelationshipSave(
            final OntologyRepository ontologyRepository,
            final WorkQueueRepository workQueueRepository
    ) {
        super(ontologyRepository);
        this.ontologyRepository = ontologyRepository;
        this.workQueueRepository = workQueueRepository;
    }

    @Handle
    public ClientApiOntology.Relationship handle(
            @Required(name = "displayName", allowEmpty = false) String displayName,
            @Required(name = "sourceIris[]", allowEmpty = false) String[] sourceIris,
            @Required(name = "targetIris[]", allowEmpty = false) String[] targetIris,
            @Optional(name = "parentIri", allowEmpty = false) String parentIri,
            @Optional(name = "iri", allowEmpty = false) String relationshipIri,
            @ActiveWorkspaceId String workspaceId,
            Authorizations authorizations,
            User user) throws Exception {


        List<Concept> domainConcepts = ontologyIrisToConcepts(sourceIris, workspaceId);
        List<Concept> rangeConcepts = ontologyIrisToConcepts(targetIris, workspaceId);

        if (relationshipIri == null) {
            relationshipIri = ontologyRepository.generateDynamicIri(Relationship.class, displayName, workspaceId);
        }

        Relationship parent = null;
        if (parentIri != null) {
            parent = ontologyRepository.getRelationshipByIRI(parentIri, workspaceId);
            if (parent == null) {
                throw new VisalloException("Unable to load parent relationship with IRI: " + parentIri);
            }
        }

        Relationship relationship = ontologyRepository.getOrCreateRelationshipType(parent, domainConcepts, rangeConcepts, relationshipIri, displayName, false, user, workspaceId);
        relationship.setProperty(OntologyProperties.DISPLAY_NAME.getPropertyName(), displayName, authorizations);

        ontologyRepository.clearCache(workspaceId);
        workQueueRepository.pushOntologyRelationshipsChange(workspaceId, relationship.getId());

        return relationship.toClientApi();
    }
}
