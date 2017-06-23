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
import org.visallo.core.model.ontology.OntologyProperty;
import org.visallo.core.model.ontology.OntologyRepositoryBase;
import org.visallo.core.model.ontology.Relationship;
import org.visallo.core.model.user.PrivilegeRepository;
import org.visallo.core.security.VisalloVisibility;
import org.visallo.core.user.SystemUser;
import org.visallo.core.user.User;
import org.visallo.vertexium.model.ontology.InMemoryOntologyRepository;
import org.visallo.web.clientapi.model.ClientApiOntology;
import org.visallo.web.clientapi.model.Privilege;
import org.visallo.web.clientapi.model.PropertyType;
import org.visallo.web.clientapi.model.SandboxStatus;
import org.visallo.web.routes.RouteTestBase;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OntologyPropertySaveTest extends RouteTestBase {
    private static final String WORKSPACE_ID = "junit-workspace";
    private static final String PUBLIC_CONCEPT_IRI = "public-concept";
    private static final String PUBLIC_RELATIONSHIP_IRI = "public-relationship";

    private OntologyPropertySave route;

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
                    return OntologyPropertySaveTest.this.privilegeRepository;
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

        ontologyRepository.getOrCreateConcept(thingConcept, PUBLIC_CONCEPT_IRI, "Public Concept", null, systemUser, null);
        ontologyRepository.getOrCreateRelationshipType(null, things, things, PUBLIC_RELATIONSHIP_IRI, true, systemUser, null);

        graph.createAuthorizations(WORKSPACE_ID);
        route = new OntologyPropertySave(ontologyRepository, workQueueRepository);
    }

    @Test
    public void testSaveNewProperty() throws Exception {
        when(privilegeRepository.hasPrivilege(user, Privilege.ONTOLOGY_ADD)).thenReturn(true);

        String propertyIRI = "junit-property";
        ClientApiOntology.Property response = route.handle(
                "New Property",
                "string",
                propertyIRI,
                new String[]{PUBLIC_CONCEPT_IRI},
                new String[]{PUBLIC_RELATIONSHIP_IRI},
                WORKSPACE_ID,
                user
        );

        // make sure the response looks ok
        assertEquals(propertyIRI, response.getTitle());
        assertEquals("New Property", response.getDisplayName());
        assertEquals(PropertyType.STRING, response.getDataType());
        assertEquals(Arrays.asList("FULL_TEXT", "EXACT_MATCH"), response.getTextIndexHints());
        assertEquals(SandboxStatus.PRIVATE, response.getSandboxStatus());

        // make sure it's sandboxed in the ontology now
        OntologyProperty property = ontologyRepository.getPropertyByIRI(propertyIRI, WORKSPACE_ID);
        assertNotNull(property);
        assertEquals("New Property", property.getDisplayName());
        assertEquals(SandboxStatus.PRIVATE, property.getSandboxStatus());

        Concept publicConcept = ontologyRepository.getConceptByIRI(PUBLIC_CONCEPT_IRI, WORKSPACE_ID);
        assertTrue(publicConcept.getProperties().stream().anyMatch(p -> p.getId().equals(propertyIRI)));

        // ensure it's not public
        assertNull(ontologyRepository.getPropertyByIRI(propertyIRI, null));

        // Make sure we let the front end know
        Mockito.verify(workQueueRepository, Mockito.times(1)).pushOntologyPropertiesChange(WORKSPACE_ID, property.getId());
    }

    @Test(expected = VisalloAccessDeniedException.class)
    public void testSaveNewPropertyWithNoPrivilege() throws Exception {
        route.handle(
            "New Property",
            "string",
            "junit-property",
            new String[]{PUBLIC_CONCEPT_IRI},
            new String[]{PUBLIC_RELATIONSHIP_IRI},
            WORKSPACE_ID,
            user
        );
    }

    @Test
    public void testSaveNewPropertyWithUnknownConcept() throws Exception {
        try {
            route.handle(
                "New Property",
                "string",
                "junit-property",
                new String[]{"unknown-concept"},
                new String[]{PUBLIC_RELATIONSHIP_IRI},
                WORKSPACE_ID,
                user
            );
            fail("Expected to raise a VisalloException for unknown concept iri.");
        } catch (VisalloException ve) {
            assertEquals("Unable to load concept with IRI: unknown-concept", ve.getMessage());
        }
    }

    @Test
    public void testSaveNewPropertyWithUnknownRelationship() throws Exception {
        try {
            route.handle(
                "New Property",
                "string",
                "junit-property",
                new String[]{PUBLIC_CONCEPT_IRI},
                new String[]{"unknown-relationship"},
                WORKSPACE_ID,
                user
            );
            fail("Expected to raise a VisalloException for unknown relationship iri.");
        } catch (VisalloException ve) {
            assertEquals("Unable to load relationship with IRI: unknown-relationship", ve.getMessage());
        }
    }

    @Test
    public void testSaveNewPropertyWithUnknownPropertyType() throws Exception {
        try {
            route.handle(
                "New Property",
                "unknown-type",
                "junit-property",
                new String[]{PUBLIC_CONCEPT_IRI},
                new String[]{PUBLIC_RELATIONSHIP_IRI},
                WORKSPACE_ID,
                user
            );
            fail("Expected to raise a VisalloException for unknown property type.");
        } catch (VisalloException ve) {
            assertEquals("Unknown property type: unknown-type", ve.getMessage());
        }
    }

    @Test
    public void testSaveNewPropertyWithGeneratedIri() throws Exception {
        when(privilegeRepository.hasPrivilege(user, Privilege.ONTOLOGY_ADD)).thenReturn(true);

        ClientApiOntology.Property response = route.handle(
            "New Property",
            "string",
            null,
            new String[]{PUBLIC_CONCEPT_IRI},
            new String[]{PUBLIC_RELATIONSHIP_IRI},
            WORKSPACE_ID,
            user
        );

        assertTrue(response.getTitle().matches(OntologyRepositoryBase.BASE_OWL_IRI + "/new_property#[a-z0-9]+"));
        assertNotNull(ontologyRepository.getPropertyByIRI(response.getTitle(), WORKSPACE_ID));
    }
}
