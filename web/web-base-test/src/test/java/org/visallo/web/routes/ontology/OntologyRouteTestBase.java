package org.visallo.web.routes.ontology;

import org.junit.Before;
import org.mockito.Mock;
import org.vertexium.Authorizations;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.lock.NonLockingLockRepository;
import org.visallo.core.model.ontology.Concept;
import org.visallo.core.model.ontology.OntologyPropertyDefinition;
import org.visallo.core.model.ontology.Relationship;
import org.visallo.core.model.user.PrivilegeRepository;
import org.visallo.core.security.VisalloVisibility;
import org.visallo.core.user.SystemUser;
import org.visallo.core.user.User;
import org.visallo.vertexium.model.ontology.InMemoryOntologyRepository;
import org.visallo.web.clientapi.model.PropertyType;
import org.visallo.web.routes.RouteTestBase;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class OntologyRouteTestBase extends RouteTestBase {
    static final String WORKSPACE_ID = "junit-workspace";
    static final String PUBLIC_CONCEPT_IRI = "public-concept-a";
    static final String PUBLIC_RELATIONSHIP_IRI = "public-relationship";
    static final String PUBLIC_PROPERTY_IRI = "public-property";

    @Mock
    PrivilegeRepository privilegeRepository;

    Authorizations workspaceAuthorizations;

    @Before
    public void before() throws IOException {
        super.before();

        NonLockingLockRepository nonLockingLockRepository = new NonLockingLockRepository();
        try {
            ontologyRepository = new InMemoryOntologyRepository(graph, configuration, nonLockingLockRepository) {
                @Override
                protected PrivilegeRepository getPrivilegeRepository() {
                    return OntologyRouteTestBase.this.privilegeRepository;
                }
            };
        } catch (Exception e) {
            throw new VisalloException("Unable to create in memory ontology repository", e);
        }

        User systemUser = new SystemUser();
        Authorizations systemAuthorizations = graph.createAuthorizations(VisalloVisibility.SUPER_USER_VISIBILITY_STRING);
        Concept thingConcept = ontologyRepository.getEntityConcept(null);

        List<Concept> things = Collections.singletonList(thingConcept);
        Relationship hasEntityRel = ontologyRepository.getOrCreateRelationshipType(null, things, things, "has-entity-iri", true, systemUser, null);
        hasEntityRel.addIntent("entityHasImage", systemAuthorizations);

        ontologyRepository.getOrCreateConcept(thingConcept, PUBLIC_CONCEPT_IRI, "Public A", null, systemUser, null);
        ontologyRepository.getOrCreateRelationshipType(null, things, things, PUBLIC_RELATIONSHIP_IRI, true, systemUser, null);

        OntologyPropertyDefinition ontologyPropertyDefinition = new OntologyPropertyDefinition(things, PUBLIC_PROPERTY_IRI, "Public Property", PropertyType.DATE);
        ontologyRepository.getOrCreateProperty(ontologyPropertyDefinition, systemUser, null);

        workspaceAuthorizations = graph.createAuthorizations(WORKSPACE_ID);
    }
}
