package org.visallo.web.routes.ontology;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.visallo.core.model.ontology.Concept;
import org.visallo.core.model.ontology.OntologyPropertyDefinition;
import org.visallo.web.clientapi.model.ClientApiOntology;
import org.visallo.web.clientapi.model.Privilege;
import org.visallo.web.clientapi.model.PropertyType;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OntologyGetTest extends OntologyRouteTestBase {
    private OntologyGet route;

    @Before
    public void before() throws IOException {
        super.before();
        route = new OntologyGet(ontologyRepository);
    }

    @Test
    public void testGetPublicOntologyElements() throws Exception {
        doRouteTest(PUBLIC_CONCEPT_IRI, 1, PUBLIC_RELATIONSHIP_IRI, 1, PUBLIC_PROPERTY_IRI, 1);
    }

    @Test
    public void testGetSandboxedOntologyElements() throws Exception {
        when(privilegeRepository.hasPrivilege(user, Privilege.ONTOLOGY_ADD)).thenReturn(true);

        String sandboxConceptIri = "sandboxed-concept";
        String sandboxRelationshipIri = "sandboxed-relationship";
        String sandboxedPropertyIri = "sandboxed-property";

        Concept thingConcept = ontologyRepository.getEntityConcept(null);
        List<Concept> things = Collections.singletonList(thingConcept);
        ontologyRepository.getOrCreateConcept(thingConcept, sandboxConceptIri, "Sandboxed Concept", null, user, WORKSPACE_ID);
        ontologyRepository.getOrCreateRelationshipType(null, things, things, sandboxRelationshipIri, true, user, WORKSPACE_ID);

        OntologyPropertyDefinition ontologyPropertyDefinition = new OntologyPropertyDefinition(things, sandboxedPropertyIri, "Sandboxed Property", PropertyType.DATE);
        ontologyRepository.getOrCreateProperty(ontologyPropertyDefinition, user, WORKSPACE_ID);

        doRouteTest(sandboxConceptIri, 1, sandboxRelationshipIri, 1, sandboxedPropertyIri, 1);
    }

    @Test
    public void testGetPublicOntologyElementsWithJustConcepts() throws Exception {
        doRouteTest(PUBLIC_CONCEPT_IRI, 1, null, 0, null, 0);
    }

    @Test
    public void testGetPublicOntologyElementsWithJustRelationships() throws Exception {
        doRouteTest(null, 0, PUBLIC_RELATIONSHIP_IRI, 1, null, 0);
    }

    @Test
    public void testGetPublicOntologyElementsWithJustProperties() throws Exception {
        doRouteTest(null, 0, null, 0, PUBLIC_PROPERTY_IRI, 1);
    }

    @Test
    public void testGetPublicOntologyElementsWithUnknownConcept() throws Exception {
        doRouteTest("unknown-iri", 0, null, 0, null, 0);
    }

    @Test
    public void testGetPublicOntologyElementsWithUnknownRelationship() throws Exception {
        doRouteTest(null, 0, "unknown-iri", 0, null, 0);
    }

    @Test
    public void testGetPublicOntologyElementsWithUnknownProperty() throws Exception {
        doRouteTest(null, 0, null, 0, "unknown-iri", 0);
    }

    private void doRouteTest(String conceptIri, int expectedConcepts, String relationshipIri, int expectedRelationships, String propertyIri, int expectedProperties) throws Exception {
        ClientApiOntology response = route.handle(
                propertyIri == null ? null : new String[]{propertyIri},
                conceptIri == null ? null : new String[]{conceptIri},
                relationshipIri == null ? null : new String[]{relationshipIri},
                WORKSPACE_ID
        );


        assertEquals(expectedConcepts, response.getConcepts().size());
        if (expectedConcepts != 0) {
            assertEquals(conceptIri, response.getConcepts().get(0).getId());
        }

        assertEquals(expectedRelationships, response.getRelationships().size());
        if (expectedRelationships != 0) {
            assertEquals(relationshipIri, response.getRelationships().get(0).getTitle());
        }

        assertEquals(expectedProperties, response.getProperties().size());
        if (expectedProperties != 0) {
            assertEquals(propertyIri, response.getProperties().get(0).getTitle());
        }
    }
}
