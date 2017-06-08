package org.visallo.vertexium.model.ontology;

import com.beust.jcommander.internal.Lists;
import com.google.common.collect.ImmutableList;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.semanticweb.owlapi.model.IRI;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.inmemory.InMemoryAuthorizations;
import org.vertexium.inmemory.InMemoryGraph;
import org.visallo.core.config.Configuration;
import org.visallo.core.config.HashMapConfigurationLoader;
import org.visallo.core.exception.VisalloAccessDeniedException;
import org.visallo.core.model.graph.GraphRepository;
import org.visallo.core.model.lock.NonLockingLockRepository;
import org.visallo.core.model.ontology.Concept;
import org.visallo.core.model.ontology.OntologyProperties;
import org.visallo.core.model.ontology.OntologyProperty;
import org.visallo.core.model.ontology.Relationship;
import org.visallo.core.model.termMention.TermMentionRepository;
import org.visallo.core.model.user.*;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.security.DirectVisibilityTranslator;
import org.visallo.core.security.VisibilityTranslator;
import org.visallo.core.user.SystemUser;
import org.visallo.core.user.User;
import org.visallo.web.clientapi.model.Privilege;
import org.visallo.web.clientapi.model.PropertyType;
import org.visallo.web.clientapi.model.UserType;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class VertexiumOntologyRepositoryTest {
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

    private User systemUser = new SystemUser();
    private User user = new SystemUser() {
        @Override
        public String getUserId() { return "junit-user"; }

        @Override
        public UserType getUserType() { return UserType.USER; }
    };
    private Set<String> userPrivileges = new HashSet<>();

    private String workspaceId = "junit-workspace";

    @Mock
    private WorkQueueRepository workQueueRepository;

    @Mock
    private UserRepository userRepository;

    @Before
    public void setup() throws Exception {
        Map<String, String> configMap = Collections.singletonMap("org.visallo.core.model.user.UserPropertyAuthorizationRepository.defaultAuthorizations", "");
        Configuration configuration = new HashMapConfigurationLoader(configMap).createConfiguration();

        when(userRepository.getSystemUser()).thenReturn(systemUser);

        Graph graph = InMemoryGraph.create();
        VisibilityTranslator visibilityTranslator = new DirectVisibilityTranslator();
        GraphAuthorizationRepository graphAuthorizationRepository = new InMemoryGraphAuthorizationRepository();
        TermMentionRepository termMentionRepository = new TermMentionRepository(graph, graphAuthorizationRepository);
        GraphRepository graphRepository = new GraphRepository(graph, visibilityTranslator, termMentionRepository, workQueueRepository);
        PrivilegeRepository privilegeRepository = new PrivilegeRepositoryBase(configuration) {
            @Override
            public void updateUser(User user, AuthorizationContext authorizationContext) { /* Nothing to do here */ }

            @Override
            protected Iterable<PrivilegesProvider> getPrivilegesProviders(Configuration configuration) { return Collections.emptyList(); }

            @Override
            public Set<String> getPrivileges(User privUser) {
                if (privUser == user) {
                    return userPrivileges;
                }
                throw new UnsupportedOperationException("This test method is only intended for the non-system user");
            }
        };
        AuthorizationRepository authorizationRepository = new AuthorizationRepositoryBase(graph) {
            @Override
            public void updateUser(User user, AuthorizationContext authorizationContext) { /* Nothing to do here */ }

            @Override
            public Set<String> getAuthorizations(User user) { return new HashSet<>(); }
        };

        ontologyRepository = new VertexiumOntologyRepository(
                graph,
                graphRepository,
                visibilityTranslator,
                configuration,
                graphAuthorizationRepository,
                new NonLockingLockRepository()
        ) {
            @Override
            public void loadOntologies(Configuration config, Authorizations authorizations) throws Exception {
                Concept rootConcept = getOrCreateConcept(null, ROOT_CONCEPT_IRI, "root", null, systemUser, null);
                getOrCreateConcept(rootConcept, ENTITY_CONCEPT_IRI, "thing", null, systemUser, null);
                clearCache();
            }

            @Override
            protected PrivilegeRepository getPrivilegeRepository() {
                return privilegeRepository;
            }

            @Override
            protected AuthorizationRepository getAuthorizationRepository() {
                return authorizationRepository;
            }
        };

        authorizations = new InMemoryAuthorizations();
    }

    @Test
    public void changingDisplayAnnotationsShouldSucceed() throws Exception {
        loadTestOwlFile();
        File changedOwl = new File(VertexiumOntologyRepositoryTest.class.getResource(TEST_CHANGED_OWL).toURI());

        ontologyRepository.importFile(changedOwl, IRI.create(TEST_IRI), authorizations);

        validateChangedOwlRelationships();
        validateChangedOwlConcepts();
        validateChangedOwlProperties();
    }

    @Test
    public void testGettingParentConceptReturnsParentProperties() throws Exception {
        loadHierarchyOwlFile();
        Concept concept = ontologyRepository.getConceptByIRI(TEST_HIERARCHY_IRI + "#person", user, workspaceId);
        Concept parentConcept = ontologyRepository.getParentConcept(concept, null, null);
        assertEquals(1, parentConcept.getProperties().size());
    }

    @Test
    public void dependenciesBetweenOntologyFilesShouldNotChangeParentProperties() throws Exception {
        loadTestOwlFile();

        File changedOwl = new File(VertexiumOntologyRepositoryTest.class.getResource(TEST01_OWL).toURI());

        ontologyRepository.importFile(changedOwl, IRI.create(TEST01_IRI), authorizations);
        validateTestOwlRelationship();
        validateTestOwlConcepts(1, 3);
        validateTestOwlProperties();

        OntologyProperty aliasProperty = ontologyRepository.getPropertyByIRI(TEST01_IRI + "#alias", user, workspaceId);
        Concept person = ontologyRepository.getConceptByIRI(TEST_IRI + "#person", user, workspaceId);
        assertTrue(person.getProperties().stream().anyMatch(p -> p.getIri().equals(aliasProperty.getIri())));
    }

    @Test
    public void testGetConceptsWithProperties() throws Exception {
        loadHierarchyOwlFile();
        ontologyRepository.clearCache();

        Iterable<Concept> conceptsWithProperties = ontologyRepository.getConceptsWithProperties(user, workspaceId);
        Map<String, Concept> conceptsByIri = StreamSupport.stream(conceptsWithProperties.spliterator(), false)
                .collect(Collectors.toMap(Concept::getIRI, Function.identity()));

        assertNull(conceptsByIri.get("http://visallo.org#root").getParentConceptIRI());
        assertEquals("http://visallo.org#root", conceptsByIri.get("http://www.w3.org/2002/07/owl#Thing").getParentConceptIRI());
        assertEquals("http://www.w3.org/2002/07/owl#Thing", conceptsByIri.get("http://visallo.org/testhierarchy#contact").getParentConceptIRI());
        assertEquals("http://visallo.org/testhierarchy#contact", conceptsByIri.get("http://visallo.org/testhierarchy#person").getParentConceptIRI());
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
        userPrivileges.add(Privilege.ONTOLOGY_ADD);

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
        userPrivileges.add(Privilege.ONTOLOGY_ADD);

        Concept thing = ontologyRepository.getEntityConcept(user, workspaceId);
        Concept sandboxedConcept = ontologyRepository.getOrCreateConcept(thing, SANDBOX_IRI, SANDBOX_DISPLAY_NAME, null, user, workspaceId);
        ontologyRepository.clearCache();

        ontologyRepository.publishConcept(sandboxedConcept, user, workspaceId);
    }

    @Test
    public void publishingConcepts() {
        userPrivileges.add(Privilege.ONTOLOGY_ADD);
        userPrivileges.add(Privilege.ONTOLOGY_PUBLISH);

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
        ontologyRepository.getOrCreateRelationshipType(null, thing, thing, PUBLIC_IRI, true, null, null);
    }

    @Test(expected = VisalloAccessDeniedException.class)
    public void creatingPublicOntologyRelationshipsWithoutPublishPrivilege() {
        List<Concept> thing = Collections.singletonList(ontologyRepository.getEntityConcept(user, workspaceId));
        ontologyRepository.getOrCreateRelationshipType(null, thing, thing, PUBLIC_IRI, true, user, null);
    }

    @Test
    public void creatingPublicOntologyRelationships() {
        List<Concept> thing = Collections.singletonList(ontologyRepository.getEntityConcept(user, workspaceId));
        ontologyRepository.getOrCreateRelationshipType(null, thing, thing, PUBLIC_IRI, true, systemUser, null);
        ontologyRepository.clearCache();

        Relationship noWorkspace = ontologyRepository.getRelationshipByIRI(PUBLIC_IRI, user, null);
        assertEquals(PUBLIC_IRI, noWorkspace.getIRI());

        Relationship withWorkspace = ontologyRepository.getRelationshipByIRI(PUBLIC_IRI, user, workspaceId);
        assertEquals(PUBLIC_IRI, withWorkspace.getIRI());
    }

    @Test(expected = VisalloAccessDeniedException.class)
    public void creatingSandboxedOntologyRelationshipsWithoutAddPermissionPrivilege() {
        List<Concept> thing = Collections.singletonList(ontologyRepository.getEntityConcept(user, workspaceId));
        ontologyRepository.getOrCreateRelationshipType(null, thing, thing, PUBLIC_IRI, true, user, workspaceId);
    }

    @Test
    public void creatingSandboxedOntologyRelationships() {
        userPrivileges.add(Privilege.ONTOLOGY_ADD);

        List<Concept> thing = Collections.singletonList(ontologyRepository.getEntityConcept(user, workspaceId));
        ontologyRepository.getOrCreateRelationshipType(null, thing, thing, SANDBOX_IRI, true, user, workspaceId);
        ontologyRepository.clearCache();

        Relationship noWorkspace = ontologyRepository.getRelationshipByIRI(SANDBOX_IRI, user, null);
        assertNull(noWorkspace);

        Relationship withWorkspace = ontologyRepository.getRelationshipByIRI(SANDBOX_IRI, user, workspaceId);
        assertEquals(SANDBOX_IRI, withWorkspace.getIRI());
    }

    private void validateTestOwlRelationship() {
        Relationship relationship = ontologyRepository.getRelationshipByIRI(TEST_IRI + "#personKnowsPerson", user, workspaceId);
        assertEquals("Knows", relationship.getDisplayName());
        assertEquals("prop('http://visallo.org/test#firstMet') || ''", relationship.getTimeFormula());
        assertTrue(relationship.getRangeConceptIRIs().contains("http://visallo.org/test#person"));
        assertTrue(relationship.getDomainConceptIRIs().contains("http://visallo.org/test#person"));

        relationship = ontologyRepository.getRelationshipByIRI(TEST_IRI + "#personIsRelatedToPerson", user, workspaceId);
        assertEquals("Is Related To", relationship.getDisplayName());
        String[] intents = relationship.getIntents();
        assertTrue(intents.length == 1);
        assertEquals("test", intents[0]);
        assertTrue(relationship.getRangeConceptIRIs().contains("http://visallo.org/test#person"));
        assertTrue(relationship.getDomainConceptIRIs().contains("http://visallo.org/test#person"));
    }

    private void validateTestOwlProperties() {
        OntologyProperty nameProperty = ontologyRepository.getPropertyByIRI(TEST_IRI + "#name", user, workspaceId);
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

        Concept person = ontologyRepository.getConceptByIRI(TEST_IRI + "#person", user, workspaceId);
        assertTrue(person.getProperties()
                .stream()
                .anyMatch(p -> p.getIri().equals(nameProperty.getIri()))
        );

        OntologyProperty firstMetProperty = ontologyRepository.getPropertyByIRI(TEST_IRI + "#firstMet", user, workspaceId);
        assertEquals("First Met", firstMetProperty.getDisplayName());
        assertEquals(PropertyType.DATE, firstMetProperty.getDataType());

        Relationship relationship = ontologyRepository.getRelationshipByIRI(TEST_IRI + "#personKnowsPerson", user, workspaceId);
        assertTrue(relationship.getProperties().stream().anyMatch(p -> p.getIri().equals(firstMetProperty.getIri())));
    }

    private void validateTestOwlConcepts(int expectedIntentSize, int expectedIriSize) throws IOException {
        Concept contact = ontologyRepository.getConceptByIRI(TEST_IRI + "#contact", user, workspaceId);
        assertEquals("Contact", contact.getDisplayName());
        assertEquals("rgb(149, 138, 218)", contact.getColor());
        assertEquals("test", contact.getDisplayType());
        List<String> intents = Arrays.asList(contact.getIntents());
        assertEquals(expectedIntentSize, intents.size());
        assertTrue(intents.contains("face"));

        Concept person = ontologyRepository.getConceptByIRI(TEST_IRI + "#person", user, workspaceId);
        assertEquals("Person", person.getDisplayName());
        intents = Arrays.asList(person.getIntents());
        assertEquals(expectedIntentSize, intents.size());
        assertTrue(intents.contains("person"));
        assertEquals("prop('http://visallo.org/test#birthDate') || ''", person.getTimeFormula());
        assertEquals("prop('http://visallo.org/test#name') || ''", person.getTitleFormula());

        byte[] bytes = IOUtils.toByteArray(VertexiumOntologyRepositoryTest.class.getResourceAsStream("glyphicons_003_user@2x.png"));
        assertArrayEquals(bytes, person.getGlyphIcon());
        assertEquals("rgb(28, 137, 28)", person.getColor());

        Set<Concept> conceptAndAllChildren = ontologyRepository.getConceptAndAllChildren(contact, user, workspaceId);
        List<String> iris = Lists.newArrayList();
        conceptAndAllChildren.stream()
                .forEach(c -> iris.add(c.getIRI()));
        assertEquals(expectedIriSize, iris.size());
        assertTrue(iris.contains(contact.getIRI()));
        assertTrue(iris.contains(person.getIRI()));
    }

    private void validateChangedOwlRelationships() throws IOException {
        Relationship relationship = ontologyRepository.getRelationshipByIRI(TEST_IRI + "#personKnowsPerson", user, workspaceId);
        assertEquals("Person Knows Person", relationship.getDisplayName());
        assertNull(relationship.getTimeFormula());
        assertFalse(relationship.getRangeConceptIRIs().contains("http://visallo.org/test#person2"));
        assertTrue(relationship.getRangeConceptIRIs().contains("http://visallo.org/test#person"));
        assertTrue(relationship.getDomainConceptIRIs().contains("http://visallo.org/test#person"));
    }

    private void validateChangedOwlConcepts() throws IOException {
        Concept contact = ontologyRepository.getConceptByIRI(TEST_IRI + "#contact", user, workspaceId);
        Concept person = ontologyRepository.getConceptByIRI(TEST_IRI + "#person", user, workspaceId);
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

        Set<Concept> conceptAndAllChildren = ontologyRepository.getConceptAndAllChildren(contact, user, workspaceId);
        List<String> iris = Lists.newArrayList();
        conceptAndAllChildren.stream().forEach(c -> iris.add(c.getIRI()));
        assertEquals(2, iris.size());
        assertTrue(iris.contains(contact.getIRI()));
        assertTrue(iris.contains(person.getIRI()));
    }

    private void validateChangedOwlProperties() throws IOException {
        OntologyProperty nameProperty = ontologyRepository.getPropertyByIRI(TEST_IRI + "#name", user, workspaceId);
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

        OntologyProperty firstMetProperty = ontologyRepository.getPropertyByIRI(TEST_IRI + "#firstMet", user, workspaceId);
        assertEquals("First Met", firstMetProperty.getDisplayName());
        assertEquals(PropertyType.DATE, firstMetProperty.getDataType());

        Relationship relationship = ontologyRepository.getRelationshipByIRI(TEST_IRI + "#personKnowsPerson", user, workspaceId);
        assertTrue(relationship.getProperties()
                .stream()
                .anyMatch(p -> p.getIri().equals(firstMetProperty.getIri()))
        );
    }

    private void loadTestOwlFile() throws Exception {
        File testOwl = new File(VertexiumOntologyRepositoryTest.class.getResource(TEST_OWL).toURI());
        ontologyRepository.importFile(testOwl, IRI.create(TEST_IRI), new InMemoryAuthorizations());

        validateTestOwlRelationship();
        validateTestOwlConcepts(1, 2);
        validateTestOwlProperties();
    }


    private void loadHierarchyOwlFile() throws Exception {
        File testOwl = new File(VertexiumOntologyRepositoryTest.class.getResource(TEST_HIERARCHY_OWL).toURI());
        ontologyRepository.importFile(testOwl, IRI.create(TEST_HIERARCHY_IRI), new InMemoryAuthorizations());
    }
}

