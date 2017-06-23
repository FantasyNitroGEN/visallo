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
public class OntologyConceptSaveTest extends RouteTestBase {
    private static final String WORKSPACE_ID = "junit-workspace";
    private static final String PUBLIC_CONCEPT_IRI = "public-concept-a";

    private OntologyConceptSave route;

    private Authorizations authorizations;

    Concept thingConcept;

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
                    return OntologyConceptSaveTest.this.privilegeRepository;
                }
            };
        } catch (Exception e) {
            throw new VisalloException("Unable to create in memory ontology repository", e);
        }

        User systemUser = new SystemUser();
        Authorizations systemAuthorizations = graph.createAuthorizations(VisalloVisibility.SUPER_USER_VISIBILITY_STRING);
        thingConcept = ontologyRepository.getEntityConcept(null);

        List<Concept> things = Collections.singletonList(thingConcept);
        Relationship hasEntityRel = ontologyRepository.getOrCreateRelationshipType(null, things, things, "has-entity-iri", true, systemUser, null);
        hasEntityRel.addIntent("entityHasImage", systemAuthorizations);

        ontologyRepository.getOrCreateConcept(thingConcept, PUBLIC_CONCEPT_IRI, "Public Concept", null, systemUser, null);

        authorizations = graph.createAuthorizations(WORKSPACE_ID);
        route = new OntologyConceptSave(ontologyRepository, workQueueRepository);
    }

    @Test
    public void testSaveNewConcept() throws Exception {
        when(privilegeRepository.hasPrivilege(user, Privilege.ONTOLOGY_ADD)).thenReturn(true);

        String conceptIri = "new-concept-iri";
        String displayName = "New Concept";
        ClientApiOntology.Concept response = route.handle(
                displayName,
                conceptIri,
                thingConcept.getIRI(),
                "glyph.png",
                "red",
                WORKSPACE_ID,
                user
        );

        assertEquals(conceptIri, response.getId());
        assertEquals(thingConcept.getIRI(), response.getParentConcept());
        assertEquals(displayName, response.getDisplayName());
        assertEquals("resource?id=new-concept-iri", response.getGlyphIconHref());
        assertEquals("red", response.getColor());
        assertEquals(SandboxStatus.PRIVATE, response.getSandboxStatus());

        // make sure it's sandboxed in the ontology now
        Concept concept = ontologyRepository.getConceptByIRI(conceptIri, WORKSPACE_ID);
        assertNotNull(concept);
        assertEquals("New Concept", concept.getDisplayName());
        assertEquals(SandboxStatus.PRIVATE, concept.getSandboxStatus());
        assertEquals(thingConcept.getIRI(), ontologyRepository.getParentConcept(concept, WORKSPACE_ID).getIRI());

        // ensure it's not public
        assertNull(ontologyRepository.getConceptByIRI(conceptIri, null));

        // Make sure we let the front end know
        Mockito.verify(workQueueRepository, Mockito.times(1)).pushOntologyConceptsChange(WORKSPACE_ID, concept.getId());
    }

    @Test(expected = VisalloAccessDeniedException.class)
    public void testSaveNewConceptNoPermission() throws Exception {
        route.handle(
                "New Concept",
                "new-concept-iri",
                thingConcept.getIRI(),
                "glyph.png",
                "red",
                WORKSPACE_ID,
                user
        );
    }

    @Test
    public void testSaveNewConceptWithUnknownParentConcept() throws Exception {
        when(privilegeRepository.hasPrivilege(user, Privilege.ONTOLOGY_ADD)).thenReturn(true);

        try {
            route.handle(
                    "New Concept",
                    "new-concept-iri",
                    "unknown-parent-iri",
                    "glyph.png",
                    "red",
                    WORKSPACE_ID,
                    user
            );
            fail("Expected to get an exception for unknown parent concept");
        } catch (VisalloException ve) {
            assertEquals("Unable to find parent concept with IRI: unknown-parent-iri", ve.getMessage());
        }
    }

    @Test
    public void testSaveNewConceptWithGeneratedIri() throws Exception {
        when(privilegeRepository.hasPrivilege(user, Privilege.ONTOLOGY_ADD)).thenReturn(true);

        ClientApiOntology.Concept response = route.handle(
                "New Concept",
                null,
                thingConcept.getIRI(),
                "glyph.png",
                "red",
                WORKSPACE_ID,
                user
        );

        assertTrue(response.getTitle().matches(OntologyRepositoryBase.BASE_OWL_IRI + "/new_concept#[a-z0-9]+"));
        assertNotNull(ontologyRepository.getConceptByIRI(response.getId(), WORKSPACE_ID));
    }
}