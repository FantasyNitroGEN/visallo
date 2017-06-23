package org.visallo.web.routes.ontology;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.visallo.core.exception.VisalloAccessDeniedException;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.ontology.OntologyRepositoryBase;
import org.visallo.core.model.ontology.Relationship;
import org.visallo.web.clientapi.model.ClientApiOntology;
import org.visallo.web.clientapi.model.Privilege;
import org.visallo.web.clientapi.model.SandboxStatus;

import java.io.IOException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OntologyRelationshipSaveTest extends OntologyRouteTestBase {
    private OntologyRelationshipSave route;

    @Before
    public void before() throws IOException {
        super.before();
        route = new OntologyRelationshipSave(ontologyRepository, workQueueRepository);
    }

    @Test
    public void testSaveNewRelationship() throws Exception {
        when(privilegeRepository.hasPrivilege(user, Privilege.ONTOLOGY_ADD)).thenReturn(true);

        String relationshipIRI = "junit-relationship";
        ClientApiOntology.Relationship response = route.handle(
                "New Relationship",
                new String[]{PUBLIC_CONCEPT_IRI},
                new String[]{ontologyRepository.getEntityConcept(null).getIRI()},
                PUBLIC_RELATIONSHIP_IRI,
                relationshipIRI,
                WORKSPACE_ID,
                workspaceAuthorizations,
                user
        );

        // make sure the response looks ok
        assertEquals(relationshipIRI, response.getTitle());
        assertEquals(PUBLIC_RELATIONSHIP_IRI, response.getParentIri());
        assertEquals("New Relationship", response.getDisplayName());
        assertEquals(SandboxStatus.PRIVATE, response.getSandboxStatus());
        assertEquals(1, response.getDomainConceptIris().size());
        assertEquals(PUBLIC_CONCEPT_IRI, response.getDomainConceptIris().get(0));
        assertEquals(1, response.getRangeConceptIris().size());
        assertEquals(ontologyRepository.getEntityConcept(null).getIRI(), response.getRangeConceptIris().get(0));

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
                new String[]{PUBLIC_CONCEPT_IRI},
                new String[]{ontologyRepository.getEntityConcept(null).getIRI()},
                PUBLIC_RELATIONSHIP_IRI,
                "junit-relationship",
                WORKSPACE_ID,
                workspaceAuthorizations,
                user
        );
    }

    @Test
    public void testSaveNewRelationshipWithUnknownDomainConcept() throws Exception {
        try {
            route.handle(
                    "New Relationship",
                    new String[]{"unknown-concept"},
                    new String[]{PUBLIC_CONCEPT_IRI},
                    null,
                    "junit-relationship",
                    WORKSPACE_ID,
                    workspaceAuthorizations,
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
                    new String[]{PUBLIC_CONCEPT_IRI},
                    new String[]{"unknown-concept"},
                    null,
                    "junit-relationship",
                    WORKSPACE_ID,
                    workspaceAuthorizations,
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
                    new String[]{PUBLIC_CONCEPT_IRI},
                    new String[]{ontologyRepository.getEntityConcept(null).getIRI()},
                    "unknown-parent-relationship",
                    "junit-relationship",
                    WORKSPACE_ID,
                    workspaceAuthorizations,
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
                new String[]{PUBLIC_CONCEPT_IRI},
                new String[]{ontologyRepository.getEntityConcept(null).getIRI()},
                null,
                null,
                WORKSPACE_ID,
                workspaceAuthorizations,
                user
        );

        assertTrue(response.getTitle().matches(OntologyRepositoryBase.BASE_OWL_IRI + "/new_relationship#[a-z0-9]+"));
        assertNotNull(ontologyRepository.getRelationshipByIRI(response.getTitle(), WORKSPACE_ID));
    }
}
