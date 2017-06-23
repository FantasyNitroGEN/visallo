package org.visallo.web.routes.ontology;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.vertexium.Authorizations;
import org.visallo.core.exception.VisalloAccessDeniedException;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.lock.NonLockingLockRepository;
import org.visallo.core.model.ontology.Concept;
import org.visallo.core.model.ontology.OntologyRepositoryBase;
import org.visallo.core.model.ontology.Relationship;
import org.visallo.core.model.user.PrivilegeRepository;
import org.visallo.core.security.VisalloVisibility;
import org.visallo.core.user.SystemUser;
import org.visallo.core.user.User;
import org.visallo.vertexium.model.ontology.InMemoryOntologyRepository;
import org.visallo.web.clientapi.model.ClientApiOntology;
import org.visallo.web.clientapi.model.Privilege;
import org.visallo.web.clientapi.model.SandboxStatus;
import org.visallo.web.routes.RouteTestBase;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OntologyRelationshipSaveTest extends RouteTestBase {
    private static final String WORKSPACE_ID = "junit-workspace";
    private static final String PUBLIC_CONCEPT_A_IRI = "public-concept-a";
    private static final String PUBLIC_CONCEPT_B_IRI = "public-concept-b";
    private static final String PUBLIC_RELATIONSHIP_IRI = "public-relationship";

    private OntologyRelationshipSave route;

    private Authorizations authorizations;

    @Mock
    private PrivilegeRepository privilegeRepository;

    @Before
    public void before() throws IOException {
        super.before();

        NonLockingLockRepository nonLockingLockRepository = new NonLockingLockRepository();
        try {
            ontologyRepository = new InMemoryOntologyRepository(graph, configuration, nonLockingLockRepository) {
                @Override
                protected PrivilegeRepository getPrivilegeRepository() {
                    return OntologyRelationshipSaveTest.this.privilegeRepository;
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

        ontologyRepository.getOrCreateConcept(thingConcept, PUBLIC_CONCEPT_A_IRI, "Public A", null, systemUser, null);
        ontologyRepository.getOrCreateConcept(thingConcept, PUBLIC_CONCEPT_B_IRI, "Public B", null, systemUser, null);
        ontologyRepository.getOrCreateRelationshipType(null, things, things, PUBLIC_RELATIONSHIP_IRI, true, systemUser, null);

        authorizations = graph.createAuthorizations(WORKSPACE_ID);
        route = new OntologyRelationshipSave(ontologyRepository, workQueueRepository);
    }

    @Test
    public void testSaveNewRelationship() throws Exception {
        when(privilegeRepository.hasPrivilege(user, Privilege.ONTOLOGY_ADD)).thenReturn(true);

        String relationshipIRI = "junit-relationship";
        ClientApiOntology.Relationship response = route.handle(
                "New Relationship",
                new String[]{PUBLIC_CONCEPT_A_IRI},
                new String[]{PUBLIC_CONCEPT_B_IRI},
                PUBLIC_RELATIONSHIP_IRI,
                relationshipIRI,
                WORKSPACE_ID,
                authorizations,
                user
        );

        // make sure the response looks ok
        assertEquals(relationshipIRI, response.getTitle());
        assertEquals(PUBLIC_RELATIONSHIP_IRI, response.getParentIri());
        assertEquals("New Relationship", response.getDisplayName());
        assertEquals(SandboxStatus.PRIVATE, response.getSandboxStatus());
        assertEquals(1, response.getDomainConceptIris().size());
        assertEquals(PUBLIC_CONCEPT_A_IRI, response.getDomainConceptIris().get(0));
        assertEquals(1, response.getRangeConceptIris().size());
        assertEquals(PUBLIC_CONCEPT_B_IRI, response.getRangeConceptIris().get(0));

        // make sure it's sandboxed in the ontology now
        Relationship relationship = ontologyRepository.getRelationshipByIRI(relationshipIRI, WORKSPACE_ID);
        assertNotNull(relationship);
        assertEquals("New Relationship", relationship.getDisplayName());
        assertEquals(SandboxStatus.PRIVATE, relationship.getSandboxStatus());
        assertEquals(PUBLIC_RELATIONSHIP_IRI, ontologyRepository.getParentRelationship(relationship, WORKSPACE_ID).getIRI());

        // ensure it's not public
        assertNull(ontologyRepository.getRelationshipByIRI(relationshipIRI, null));

        // Make sure we let the front end know
        Mockito.verify(workQueueRepository, Mockito.times(1)).pushOntologyRelationshipsChange(WORKSPACE_ID, relationship.getId());
    }
    @Test(expected = VisalloAccessDeniedException.class)
    public void testSaveNewRelationshipNoPrivilege() throws Exception {
        route.handle(
                "New Relationship",
                new String[]{PUBLIC_CONCEPT_A_IRI},
                new String[]{PUBLIC_CONCEPT_B_IRI},
                PUBLIC_RELATIONSHIP_IRI,
                "junit-relationship",
                WORKSPACE_ID,
                authorizations,
                user
        );
    }

    @Test
    public void testSaveNewRelationshipWithUnknownDomainConcept() throws Exception {
        try {
            route.handle(
                    "New Relationship",
                    new String[]{"unknown-concept"},
                    new String[]{PUBLIC_CONCEPT_B_IRI},
                    null,
                    "junit-relationship",
                    WORKSPACE_ID,
                    authorizations,
                    user
            );
            fail("Expected to raise a VisalloException for unknown concept iri.");
        } catch (VisalloException ve) {
            assertEquals("Unable to load concept with IRI: unknown-concept", ve.getMessage());
        }
    }

    @Test
    public void testSaveNewRelationshipWithUnknownRangeConcept() throws Exception {
        try {
            route.handle(
                    "New Relationship",
                    new String[]{PUBLIC_CONCEPT_A_IRI},
                    new String[]{"unknown-concept"},
                    null,
                    "junit-relationship",
                    WORKSPACE_ID,
                    authorizations,
                    user
            );
            fail("Expected to raise a VisalloException for unknown concept iri.");
        } catch (VisalloException ve) {
            assertEquals("Unable to load concept with IRI: unknown-concept", ve.getMessage());
        }
    }

    @Test
    public void testSaveNewRelationshipWithUnknownParentIri() throws Exception {
        try {
            route.handle(
                    "New Relationship",
                    new String[]{PUBLIC_CONCEPT_A_IRI},
                    new String[]{PUBLIC_CONCEPT_B_IRI},
                    "unknown-parent-relationship",
                    "junit-relationship",
                    WORKSPACE_ID,
                    authorizations,
                    user
            );
            fail("Expected to raise a VisalloException for unknown relationship iri.");
        } catch (VisalloException ve) {
            assertEquals("Unable to load parent relationship with IRI: unknown-parent-relationship", ve.getMessage());
        }
    }

    @Test
    public void testSaveNewRelationshipWithGeneratedIri() throws Exception {
        when(privilegeRepository.hasPrivilege(user, Privilege.ONTOLOGY_ADD)).thenReturn(true);

        ClientApiOntology.Relationship response = route.handle(
                "New Relationship",
                new String[]{PUBLIC_CONCEPT_A_IRI},
                new String[]{PUBLIC_CONCEPT_B_IRI},
                null,
                null,
                WORKSPACE_ID,
                authorizations,
                user
        );

        assertTrue(response.getTitle().matches(OntologyRepositoryBase.BASE_OWL_IRI + "/new_relationship#[a-z0-9]+"));
        assertNotNull(ontologyRepository.getRelationshipByIRI(response.getTitle(), WORKSPACE_ID));
    }
}
