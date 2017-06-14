package org.visallo.vertexium.model.ontology;

import com.beust.jcommander.internal.Lists;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.semanticweb.owlapi.model.IRI;
import org.vertexium.Authorizations;
import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloAccessDeniedException;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.ontology.Concept;
import org.visallo.core.model.ontology.OntologyProperty;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.ontology.Relationship;
import org.visallo.core.model.user.AuthorizationRepository;
import org.visallo.core.model.user.PrivilegeRepository;
import org.visallo.core.user.SystemUser;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloInMemoryTestBase;
import org.visallo.web.clientapi.model.Privilege;
import org.visallo.web.clientapi.model.PropertyType;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.junit.Assert.*;
import static org.visallo.core.model.user.UserRepository.USER_CONCEPT_IRI;

public class VertexiumOntologyRepositoryTest extends VisalloInMemoryTestBase {
    private static final String TEST_OWL = "test.owl";
    private static final String TEST_IRI = "http://visallo.org/test";

    private static final String TEST01_OWL = "test01.owl";
    private static final String TEST01_IRI = "http://visallo.org/test01";

    private static final String TEST_HIERARCHY_OWL = "test_hierarchy.owl";
    private static final String TEST_HIERARCHY_IRI = "http://visallo.org/testhierarchy";

    private static final String TEST_CHANGED_OWL = "test_changed.owl";

    private static final String SANDBOX_IRI = "sandbox-iri";
    private static final String SANDBOX_DISPLAY_NAME = "Sandbox Display";
    private static final String PUBLIC_IRI = "public-iri";
    private static final String PUBLIC_DISPLAY_NAME = "Public Display";

    private VertexiumOntologyRepository ontologyRepository;
    private Authorizations authorizations;

    private String workspaceId = "junit-workspace";
    private User systemUser = new SystemUser();
    private User user;

    @Before
    public void before() {
        super.before();
        authorizations = getGraph().createAuthorizations();
        user = getUserRepository().findOrAddUser("junit", "Junit", "junit@visallo.com", "password");
    }

    @Test
    public void changingDisplayAnnotationsShouldSucceed() throws Exception {
        loadTestOwlFile();
        File changedOwl = new File(VertexiumOntologyRepositoryTest.class.getResource(TEST_CHANGED_OWL).toURI());

        getOntologyRepository().importFile(changedOwl, IRI.create(TEST_IRI), authorizations);

        validateChangedOwlRelationships();
        validateChangedOwlConcepts();
        validateChangedOwlProperties();
    }

    @Test
    public void testGettingParentConceptReturnsParentProperties() throws Exception {
        loadHierarchyOwlFile();
        Concept concept = getOntologyRepository().getConceptByIRI(TEST_HIERARCHY_IRI + "#person");
        Concept parentConcept = getOntologyRepository().getParentConcept(concept);
        assertEquals(1, parentConcept.getProperties().size());
    }


    @Test
    public void dependenciesBetweenOntologyFilesShouldNotChangeParentProperties() throws Exception {
        loadTestOwlFile();
        File changedOwl = new File(VertexiumOntologyRepositoryTest.class.getResource(TEST01_OWL).toURI());

        getOntologyRepository().importFile(changedOwl, IRI.create(TEST01_IRI), authorizations);
        validateTestOwlRelationship();
        validateTestOwlConcepts(1, 3);
        validateTestOwlProperties();

        OntologyProperty aliasProperty = getOntologyRepository().getPropertyByIRI(TEST01_IRI + "#alias");
        Concept person = getOntologyRepository().getConceptByIRI(TEST_IRI + "#person");
        assertTrue(person.getProperties()
                .stream()
                .anyMatch(p -> p.getIri().equals(aliasProperty.getIri()))
        );
    }

    @Test
    public void testGetConceptsWithProperties() throws Exception {
        loadHierarchyOwlFile();
        ontologyRepository.clearCache();

        Iterable<Concept> conceptsWithProperties = ontologyRepository.getConceptsWithProperties(user, workspaceId);
        Map<String, Concept> conceptsByIri = StreamSupport.stream(conceptsWithProperties.spliterator(), false)
                .collect(Collectors.toMap(Concept::getIRI, Function.identity()));

        Concept personConcept = conceptsByIri.get("http://visallo.org/testhierarchy#person");

        // Check parent iris
        assertNull(conceptsByIri.get("http://visallo.org#root").getParentConceptIRI());
        assertEquals("http://visallo.org#root", conceptsByIri.get("http://www.w3.org/2002/07/owl#Thing").getParentConceptIRI());
        assertEquals("http://www.w3.org/2002/07/owl#Thing", conceptsByIri.get("http://visallo.org/testhierarchy#contact").getParentConceptIRI());
        assertEquals("http://visallo.org/testhierarchy#contact", personConcept.getParentConceptIRI());

        // Check properties
        List<OntologyProperty> personProperties = personConcept.getProperties().stream().collect(Collectors.toList());
        assertEquals(1, personProperties.size());
        assertEquals("http://visallo.org/testhierarchy#name", personProperties.get(0).getIri());

        // Check intents
        List<String> intents = Arrays.asList(personConcept.getIntents());
        assertEquals(2, intents.size());
        assertTrue(intents.contains("face"));
        assertTrue(intents.contains("person"));

        // Check metadata
        Map<String, String> metadata = personConcept.getMetadata();
        assertEquals("{\"source\":\"(ontology)|visallo\"}", metadata.get("http://visallo.org#visibilityJson"));

        // Spot check other concept values
        assertEquals("Person", personConcept.getDisplayName());
        assertEquals("prop('http://visallo.org/testhierarchy#name') || ''", personConcept.getTitleFormula());
    }

    @Test
    public void testGetRelationships() throws Exception {
        loadHierarchyOwlFile();
        ontologyRepository.clearCache();

        Iterable<Relationship> relationships = ontologyRepository.getRelationships(user, workspaceId);
        Map<String, Relationship> relationshipsByIri = StreamSupport.stream(relationships.spliterator(), false)
                .collect(Collectors.toMap(Relationship::getIRI, Function.identity()));

        assertNull(relationshipsByIri.get("http://www.w3.org/2002/07/owl#topObjectProperty").getParentIRI());
        assertEquals("http://www.w3.org/2002/07/owl#topObjectProperty", relationshipsByIri.get("http://visallo.org/testhierarchy#personKnowsPerson").getParentIRI());
    }

    @Test(expected = VisalloAccessDeniedException.class)
    public void creatingOntologyConceptsWithNoUserOrWorkspace() {
        Concept thing = ontologyRepository.getEntityConcept(user, workspaceId);
        ontologyRepository.getOrCreateConcept(thing, PUBLIC_IRI, PUBLIC_DISPLAY_NAME, null, null, null);
    }

    @Test(expected = VisalloAccessDeniedException.class)
    public void creatingPublicOntologyConceptsWithoutPublishPrivilege() {
        Concept thing = ontologyRepository.getEntityConcept(user, workspaceId);
        ontologyRepository.getOrCreateConcept(thing, PUBLIC_IRI, PUBLIC_DISPLAY_NAME, null, user, null);
    }

    @Test
    public void creatingPublicOntologyConcepts() {
        Concept thing = ontologyRepository.getEntityConcept(systemUser, workspaceId);
        ontologyRepository.getOrCreateConcept(thing, PUBLIC_IRI, PUBLIC_DISPLAY_NAME, null, systemUser, null);
        ontologyRepository.clearCache();

        Concept noWorkspace = ontologyRepository.getConceptByIRI(PUBLIC_IRI, user, null);
        assertEquals(PUBLIC_DISPLAY_NAME, noWorkspace.getDisplayName());

        Concept withWorkspace = ontologyRepository.getConceptByIRI(PUBLIC_IRI, user, workspaceId);
        assertEquals(PUBLIC_DISPLAY_NAME, withWorkspace.getDisplayName());
    }

    @Test(expected = VisalloAccessDeniedException.class)
    public void creatingSandboxedOntologyConceptsWithoutAddPermissionPrivilege() {
        Concept thing = ontologyRepository.getEntityConcept(user, workspaceId);
        ontologyRepository.getOrCreateConcept(thing, SANDBOX_IRI, SANDBOX_DISPLAY_NAME, null, user, workspaceId);
    }

    @Test
    public void creatingSandboxedOntologyConcepts() {
        setPrivileges(user, Collections.singleton(Privilege.ONTOLOGY_ADD));

        Concept thing = ontologyRepository.getEntityConcept(user, workspaceId);
        ontologyRepository.getOrCreateConcept(thing, SANDBOX_IRI, SANDBOX_DISPLAY_NAME, null, user, workspaceId);
        ontologyRepository.clearCache();

        Concept noWorkspace = ontologyRepository.getConceptByIRI(SANDBOX_IRI, user, null);
        assertNull(noWorkspace);

        Concept withWorkspace = ontologyRepository.getConceptByIRI(SANDBOX_IRI, user, workspaceId);
        assertEquals(SANDBOX_DISPLAY_NAME, withWorkspace.getDisplayName());
    }

    @Test(expected = VisalloAccessDeniedException.class)
    public void publishingConceptsWithoutPublishPrivilege() {
        setPrivileges(user, Collections.singleton(Privilege.ONTOLOGY_ADD));

        Concept thing = ontologyRepository.getEntityConcept(user, workspaceId);
        Concept sandboxedConcept = ontologyRepository.getOrCreateConcept(thing, SANDBOX_IRI, SANDBOX_DISPLAY_NAME, null, user, workspaceId);
        ontologyRepository.clearCache();

        ontologyRepository.publishConcept(sandboxedConcept, user, workspaceId);
    }

    @Test
    public void publishingConcepts() {
        setPrivileges(user, Sets.newHashSet(Privilege.ONTOLOGY_ADD, Privilege.ONTOLOGY_PUBLISH));

        Concept thing = ontologyRepository.getEntityConcept(user, workspaceId);
        Concept sandboxedConcept = ontologyRepository.getOrCreateConcept(thing, SANDBOX_IRI, SANDBOX_DISPLAY_NAME, null, user, workspaceId);
        ontologyRepository.publishConcept(sandboxedConcept, user, workspaceId);
        ontologyRepository.clearCache();

        Concept publicConcept = ontologyRepository.getOrCreateConcept(thing, SANDBOX_IRI, SANDBOX_DISPLAY_NAME, null, user, null);
        assertEquals(SANDBOX_DISPLAY_NAME, publicConcept.getDisplayName());
    }

    @Test(expected = VisalloAccessDeniedException.class)
    public void creatingOntologyRelationshipsWithNoUserOrWorkspace() {
        List<Concept> thing = Collections.singletonList(ontologyRepository.getEntityConcept(user, workspaceId));
        ontologyRepository.getOrCreateRelationshipType(null, thing, thing, PUBLIC_IRI, null, true, null, null);
    }

    @Test(expected = VisalloAccessDeniedException.class)
    public void creatingPublicOntologyRelationshipsWithoutPublishPrivilege() {
        List<Concept> thing = Collections.singletonList(ontologyRepository.getEntityConcept(user, workspaceId));
        ontologyRepository.getOrCreateRelationshipType(null, thing, thing, PUBLIC_IRI, null, true, user, null);
    }

    @Test
    public void creatingPublicOntologyRelationships() {
        List<Concept> thing = Collections.singletonList(ontologyRepository.getEntityConcept(user, workspaceId));
        ontologyRepository.getOrCreateRelationshipType(null, thing, thing, PUBLIC_IRI, null, true, systemUser, null);
        ontologyRepository.clearCache();

        Relationship noWorkspace = ontologyRepository.getRelationshipByIRI(PUBLIC_IRI, user, null);
        assertEquals(PUBLIC_IRI, noWorkspace.getIRI());

        Relationship withWorkspace = ontologyRepository.getRelationshipByIRI(PUBLIC_IRI, user, workspaceId);
        assertEquals(PUBLIC_IRI, withWorkspace.getIRI());
    }

    @Test(expected = VisalloAccessDeniedException.class)
    public void creatingSandboxedOntologyRelationshipsWithoutAddPermissionPrivilege() {
        List<Concept> thing = Collections.singletonList(ontologyRepository.getEntityConcept(user, workspaceId));
        ontologyRepository.getOrCreateRelationshipType(null, thing, thing, PUBLIC_IRI, null, true, user, workspaceId);
    }

    @Test
    public void creatingSandboxedOntologyRelationships() {
        setPrivileges(user, Collections.singleton(Privilege.ONTOLOGY_ADD));

        List<Concept> thing = Collections.singletonList(ontologyRepository.getEntityConcept(user, workspaceId));
        ontologyRepository.getOrCreateRelationshipType(null, thing, thing, SANDBOX_IRI, null, true, user, workspaceId);
        ontologyRepository.clearCache();

        Relationship noWorkspace = ontologyRepository.getRelationshipByIRI(SANDBOX_IRI, user, null);
        assertNull(noWorkspace);

        Relationship withWorkspace = ontologyRepository.getRelationshipByIRI(SANDBOX_IRI, user, workspaceId);
        assertEquals(SANDBOX_IRI, withWorkspace.getIRI());
    }

    private void validateTestOwlRelationship() {
        Relationship relationship = getOntologyRepository().getRelationshipByIRI(TEST_IRI + "#personKnowsPerson");
        assertEquals("Knows", relationship.getDisplayName());
        assertEquals("prop('http://visallo.org/test#firstMet') || ''", relationship.getTimeFormula());
        assertTrue(relationship.getRangeConceptIRIs().contains("http://visallo.org/test#person"));
        assertTrue(relationship.getDomainConceptIRIs().contains("http://visallo.org/test#person"));

        relationship = getOntologyRepository().getRelationshipByIRI(TEST_IRI + "#personIsRelatedToPerson");
        assertEquals("Is Related To", relationship.getDisplayName());
        String[] intents = relationship.getIntents();
        assertTrue(intents.length == 1);
        assertEquals("test", intents[0]);
        assertTrue(relationship.getRangeConceptIRIs().contains("http://visallo.org/test#person"));
        assertTrue(relationship.getDomainConceptIRIs().contains("http://visallo.org/test#person"));
    }

    private void validateTestOwlProperties() {
        OntologyProperty nameProperty = getOntologyRepository().getPropertyByIRI(TEST_IRI + "#name");
        assertEquals("Name", nameProperty.getDisplayName());
        assertEquals(PropertyType.STRING, nameProperty.getDataType());
        assertEquals("_.compact([\n" +
                "            dependentProp('http://visallo.org/test#firstName'),\n" +
                "            dependentProp('http://visallo.org/test#middleName'),\n" +
                "            dependentProp('http://visallo.org/test#lastName')\n" +
                "            ]).join(', ')", nameProperty.getDisplayFormula().trim());
        ImmutableList<String> dependentPropertyIris = nameProperty.getDependentPropertyIris();
        assertEquals(3, dependentPropertyIris.size());
        assertTrue(dependentPropertyIris.contains("http://visallo.org/test#firstName"));
        assertTrue(dependentPropertyIris.contains("http://visallo.org/test#middleName"));
        assertTrue(dependentPropertyIris.contains("http://visallo.org/test#lastName"));
        List<String> intents = Arrays.asList(nameProperty.getIntents());
        assertEquals(1, intents.size());
        assertTrue(intents.contains("test3"));
        assertEquals(
                "dependentProp('http://visallo.org/test#lastName') && dependentProp('http://visallo.org/test#firstName')",
                nameProperty.getValidationFormula()
        );
        assertEquals("Personal Information", nameProperty.getPropertyGroup());
        assertEquals("test", nameProperty.getDisplayType());
        assertFalse(nameProperty.getAddable());
        assertFalse(nameProperty.getUpdateable());
        assertFalse(nameProperty.getDeleteable());
        Map<String, String> possibleValues = nameProperty.getPossibleValues();
        assertEquals(2, possibleValues.size());
        assertEquals("test 1", possibleValues.get("T1"));
        assertEquals("test 2", possibleValues.get("T2"));

        Concept person = getOntologyRepository().getConceptByIRI(TEST_IRI + "#person");
        assertTrue(person.getProperties()
                .stream()
                .anyMatch(p -> p.getIri().equals(nameProperty.getIri()))
        );

        OntologyProperty firstMetProperty = getOntologyRepository().getPropertyByIRI(TEST_IRI + "#firstMet");
        assertEquals("First Met", firstMetProperty.getDisplayName());
        assertEquals(PropertyType.DATE, firstMetProperty.getDataType());

        Relationship relationship = getOntologyRepository().getRelationshipByIRI(TEST_IRI + "#personKnowsPerson");
        assertTrue(relationship.getProperties()
                .stream()
                .anyMatch(p -> p.getIri().equals(firstMetProperty.getIri()))
        );
    }

    private void validateTestOwlConcepts(int expectedIntentSize, int expectedIriSize) throws IOException {
        Concept contact = getOntologyRepository().getConceptByIRI(TEST_IRI + "#contact");
        assertEquals("Contact", contact.getDisplayName());
        assertEquals("rgb(149, 138, 218)", contact.getColor());
        assertEquals("test", contact.getDisplayType());
        List<String> intents = Arrays.asList(contact.getIntents());
        assertEquals(expectedIntentSize, intents.size());
        assertTrue(intents.contains("face"));

        Concept person = getOntologyRepository().getConceptByIRI(TEST_IRI + "#person");
        assertEquals("Person", person.getDisplayName());
        intents = Arrays.asList(person.getIntents());
        assertEquals(expectedIntentSize, intents.size());
        assertTrue(intents.contains("person"));
        assertEquals("prop('http://visallo.org/test#birthDate') || ''", person.getTimeFormula());
        assertEquals("prop('http://visallo.org/test#name') || ''", person.getTitleFormula());

        byte[] bytes = IOUtils.toByteArray(VertexiumOntologyRepositoryTest.class.getResourceAsStream("glyphicons_003_user@2x.png"));
        assertArrayEquals(bytes, person.getGlyphIcon());
        assertEquals("rgb(28, 137, 28)", person.getColor());

        Set<Concept> conceptAndAllChildren = getOntologyRepository().getConceptAndAllChildren(contact);
        List<String> iris = Lists.newArrayList();
        conceptAndAllChildren.stream()
                .forEach(c -> iris.add(c.getIRI()));
        assertEquals(expectedIriSize, iris.size());
        assertTrue(iris.contains(contact.getIRI()));
        assertTrue(iris.contains(person.getIRI()));
    }

    private void validateChangedOwlRelationships() throws IOException {
        Relationship relationship = getOntologyRepository().getRelationshipByIRI(TEST_IRI + "#personKnowsPerson");
        assertEquals("Person Knows Person", relationship.getDisplayName());
        assertNull(relationship.getTimeFormula());
        assertFalse(relationship.getRangeConceptIRIs().contains("http://visallo.org/test#person2"));
        assertTrue(relationship.getRangeConceptIRIs().contains("http://visallo.org/test#person"));
        assertTrue(relationship.getDomainConceptIRIs().contains("http://visallo.org/test#person"));
    }

    private void validateChangedOwlConcepts() throws IOException {
        Concept contact = getOntologyRepository().getConceptByIRI(TEST_IRI + "#contact");
        Concept person = getOntologyRepository().getConceptByIRI(TEST_IRI + "#person");
        assertEquals("Person", person.getDisplayName());
        List<String> intents = Arrays.asList(person.getIntents());
        assertEquals(1, intents.size());
        assertFalse(intents.contains("person"));
        assertFalse(intents.contains("face"));
        assertTrue(intents.contains("test"));
        assertNull(person.getTimeFormula());
        assertEquals("prop('http://visallo.org/test#name') || ''", person.getTitleFormula());

        assertNull(person.getGlyphIcon());
        assertEquals("rgb(28, 137, 28)", person.getColor());

        Set<Concept> conceptAndAllChildren = getOntologyRepository().getConceptAndAllChildren(contact);
        List<String> iris = Lists.newArrayList();
        conceptAndAllChildren.stream()
                .forEach(c -> iris.add(c.getIRI()));
        assertEquals(2, iris.size());
        assertTrue(iris.contains(contact.getIRI()));
        assertTrue(iris.contains(person.getIRI()));
    }

    private void validateChangedOwlProperties() throws IOException {
        OntologyProperty nameProperty = getOntologyRepository().getPropertyByIRI(TEST_IRI + "#name");
        assertEquals("http://visallo.org/test#name", nameProperty.getDisplayName());
        assertEquals(PropertyType.STRING, nameProperty.getDataType());
        assertEquals("_.compact([\n" +
                "            dependentProp('http://visallo.org/test#firstName'),\n" +
                "            dependentProp('http://visallo.org/test#lastName')\n" +
                "            ]).join(', ')", nameProperty.getDisplayFormula().trim());
        ImmutableList<String> dependentPropertyIris = nameProperty.getDependentPropertyIris();
        assertEquals(3, dependentPropertyIris.size());
        assertTrue(dependentPropertyIris.contains("http://visallo.org/test#firstName"));
        assertTrue(dependentPropertyIris.contains("http://visallo.org/test#middleName"));
        assertTrue(dependentPropertyIris.contains("http://visallo.org/test#lastName"));
        List<String> intents = Arrays.asList(nameProperty.getIntents());
        assertEquals(1, intents.size());
        assertTrue(intents.contains("test3"));
        assertEquals(
                "dependentProp('http://visallo.org/test#lastName') && dependentProp('http://visallo.org/test#firstName')",
                nameProperty.getValidationFormula()
        );
        assertEquals("Personal Information", nameProperty.getPropertyGroup());
        assertEquals("test 2", nameProperty.getDisplayType());
        assertTrue(nameProperty.getAddable());
        assertTrue(nameProperty.getUpdateable());
        assertTrue(nameProperty.getDeleteable());
        Map<String, String> possibleValues = nameProperty.getPossibleValues();
        assertNull(possibleValues);

        OntologyProperty firstMetProperty = getOntologyRepository().getPropertyByIRI(TEST_IRI + "#firstMet");
        assertEquals("First Met", firstMetProperty.getDisplayName());
        assertEquals(PropertyType.DATE, firstMetProperty.getDataType());

        Relationship relationship = getOntologyRepository().getRelationshipByIRI(TEST_IRI + "#personKnowsPerson");
        assertTrue(relationship.getProperties()
                .stream()
                .anyMatch(p -> p.getIri().equals(firstMetProperty.getIri()))
        );
    }

    private void loadTestOwlFile() throws Exception {
        createTestOntologyRepository(TEST_OWL, TEST_IRI);
        validateTestOwlRelationship();
        validateTestOwlConcepts(1, 2);
        validateTestOwlProperties();
    }


    private void loadHierarchyOwlFile() throws Exception {
        createTestOntologyRepository(TEST_HIERARCHY_OWL, TEST_HIERARCHY_IRI);
    }

    private VertexiumOntologyRepository createTestOntologyRepository(String owlFileResourcePath, String iri) throws Exception {
        File testOwl = new File(VertexiumOntologyRepositoryTest.class.getResource(owlFileResourcePath).toURI());
        getOntologyRepository().importFile(testOwl, IRI.create(iri), authorizations);

        return ontologyRepository;
    }

    @Override
    protected OntologyRepository getOntologyRepository() {
        if (ontologyRepository != null) {
            return ontologyRepository;
        }
        try {
            ontologyRepository = new VertexiumOntologyRepository(
                    getGraph(),
                    getGraphRepository(),
                    getVisibilityTranslator(),
                    getConfiguration(),
                    getGraphAuthorizationRepository(),
                    getLockRepository()
            ) {
                @Override
                public void loadOntologies(Configuration config, Authorizations authorizations) throws Exception {
                    Concept rootConcept = getOrCreateConcept(null, ROOT_CONCEPT_IRI, "root", null, systemUser, null);
                    getOrCreateConcept(rootConcept, ENTITY_CONCEPT_IRI, "thing", null, systemUser, null);
                    getOrCreateConcept(null, USER_CONCEPT_IRI, "visalloUser", null, false, systemUser, null);
                    clearCache();
                }

                @Override
                protected AuthorizationRepository getAuthorizationRepository() {
                    return VertexiumOntologyRepositoryTest.this.getAuthorizationRepository();
                }

                @Override
                protected PrivilegeRepository getPrivilegeRepository() {
                    return VertexiumOntologyRepositoryTest.this.getPrivilegeRepository();
                }
            };
        } catch (Exception ex) {
            throw new VisalloException("Could not create ontology repository", ex);
        }
        return ontologyRepository;
    }
}

