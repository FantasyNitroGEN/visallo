package org.visallo.vertexium.model.ontology;

import com.beust.jcommander.internal.Lists;
import com.google.common.collect.ImmutableList;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.semanticweb.owlapi.model.IRI;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.inmemory.InMemoryAuthorizations;
import org.vertexium.inmemory.InMemoryGraph;
import org.visallo.core.config.Configuration;
import org.visallo.core.model.lock.LockRepository;
import org.visallo.core.model.lock.NonLockingLockRepository;
import org.visallo.core.model.ontology.Concept;
import org.visallo.core.model.ontology.OntologyProperty;
import org.visallo.core.model.ontology.Relationship;
import org.visallo.core.model.user.GraphAuthorizationRepository;
import org.visallo.core.model.user.InMemoryGraphAuthorizationRepository;
import org.visallo.web.clientapi.model.PropertyType;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

public class VertexiumOntologyRepositoryTest {
    private static final String TEST_OWL = "test.owl";
    private static final String TEST_CHANGED_OWL = "test_changed.owl";
    private static final String TEST01_OWL = "test01.owl";
    private static final String TEST_IRI = "http://visallo.org/test";
    private static final String TEST01_IRI = "http://visallo.org/test01";
    private VertexiumOntologyRepository ontologyRepository;
    private Authorizations authorizations;
    private Graph graph;
    private GraphAuthorizationRepository graphAuthorizationRepository;
    private LockRepository lockRepository;
    @Mock
    private Configuration configuration;

    @Before
    public void setup() throws Exception {
        graph = InMemoryGraph.create();
        authorizations = new InMemoryAuthorizations();
        graphAuthorizationRepository = new InMemoryGraphAuthorizationRepository();
        lockRepository = new NonLockingLockRepository();
        ontologyRepository = new VertexiumOntologyRepository(graph, configuration,
                                                             graphAuthorizationRepository, lockRepository) {
            @Override
            public void loadOntologies(Configuration config, Authorizations authorizations) throws Exception {
                Concept rootConcept = getOrCreateConcept(null, ROOT_CONCEPT_IRI, "root", null);
                getOrCreateConcept(rootConcept, ENTITY_CONCEPT_IRI, "thing", null);
                clearCache();
                return;
            }
        };

        File testOwl = new File(VertexiumOntologyRepositoryTest.class.getResource(TEST_OWL).toURI());
        ontologyRepository.importFile(testOwl, IRI.create(TEST_IRI), authorizations);
        validateTestOwlRelationship();
        validateTestOwlConcepts(2, 2);
        validateTestOwlProperties();
    }

    @Test
    public void changingDisplayAnnotationsShouldSucceed() throws Exception {
        File changedOwl = new File(VertexiumOntologyRepositoryTest.class.getResource(TEST_CHANGED_OWL).toURI());

        ontologyRepository.importFile(changedOwl, IRI.create(TEST_IRI), authorizations);

        validateChangedOwlRelationships();
        validateChangedOwlConcepts();
        validateChangedOwlProperties();
    }

    @Test
    public void dependenciesBetweenOntologyFilesShouldNotChangeParentProperties() throws Exception {
        File changedOwl = new File(VertexiumOntologyRepositoryTest.class.getResource(TEST01_OWL).toURI());

        ontologyRepository.importFile(changedOwl, IRI.create(TEST01_IRI), authorizations);
        validateTestOwlRelationship();
        validateTestOwlConcepts(2, 3);
        validateTestOwlProperties();

        OntologyProperty aliasProperty = ontologyRepository.getPropertyByIRI(TEST01_IRI + "#alias");
        Concept person = ontologyRepository.getConceptByIRI(TEST_IRI + "#person");
        assertTrue(person.getProperties()
                .stream()
                .anyMatch(p -> p.getIri().equals(aliasProperty.getIri()))
        );
    }

    private void validateTestOwlRelationship() {
        Relationship relationship = ontologyRepository.getRelationshipByIRI(TEST_IRI + "#personKnowsPerson");
        assertEquals("Knows", relationship.getDisplayName());
        assertEquals("prop('http://visallo.org/test#firstMet') || ''", relationship.getTimeFormula());
        assertTrue(relationship.getRangeConceptIRIs().contains("http://visallo.org/test#person"));
        assertTrue(relationship.getDomainConceptIRIs().contains("http://visallo.org/test#person"));
    }

    private void validateTestOwlProperties() {
        OntologyProperty nameProperty = ontologyRepository.getPropertyByIRI(TEST_IRI + "#name");
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
        assertEquals("dependentProp('http://visallo.org/test#lastName') && dependentProp('http://visallo.org/test#firstName')",
                nameProperty.getValidationFormula());
        assertEquals("Personal Information", nameProperty.getPropertyGroup());
        assertEquals("test", nameProperty.getDisplayType());
        assertFalse(nameProperty.getAddable());
        assertFalse(nameProperty.getUpdateable());
        assertFalse(nameProperty.getDeleteable());
        Map<String, String> possibleValues = nameProperty.getPossibleValues();
        assertEquals(2, possibleValues.size());
        assertEquals("test 1", possibleValues.get("T1"));
        assertEquals("test 2", possibleValues.get("T2"));

        Concept person = ontologyRepository.getConceptByIRI(TEST_IRI + "#person");
        assertTrue(person.getProperties()
                .stream()
                .anyMatch(p -> p.getIri().equals(nameProperty.getIri()))
        );

        OntologyProperty firstMetProperty = ontologyRepository.getPropertyByIRI(TEST_IRI + "#firstMet");
        assertEquals("First Met", firstMetProperty.getDisplayName());
        assertEquals(PropertyType.DATE, firstMetProperty.getDataType());

        Relationship relationship = ontologyRepository.getRelationshipByIRI(TEST_IRI + "#personKnowsPerson");
        assertTrue(relationship.getProperties()
                .stream()
                .anyMatch(p -> p.getIri().equals(firstMetProperty.getIri()))
        );
    }

    private void validateTestOwlConcepts(int expectedIntentSize, int expectedIriSize) throws IOException {
        Concept contact = ontologyRepository.getConceptByIRI(TEST_IRI + "#contact");
        assertEquals("Contact", contact.getDisplayName());
        assertEquals("rgb(149, 138, 218)", contact.getColor());
        assertEquals("test", contact.getDisplayType());

        Concept person = ontologyRepository.getConceptByIRI(TEST_IRI + "#person");
        assertEquals("Person", person.getDisplayName());
        List<String> intents = Arrays.asList(person.getIntents());
        assertEquals(expectedIntentSize, intents.size());
        assertTrue(intents.contains("person"));
        assertTrue(intents.contains("face"));
        assertEquals("prop('http://visallo.org/test#birthDate') || ''", person.getTimeFormula());
        assertEquals("prop('http://visallo.org/test#name') || ''", person.getTitleFormula());

        byte[] bytes = IOUtils.toByteArray(VertexiumOntologyRepositoryTest.class.getResourceAsStream("glyphicons_003_user@2x.png"));
        assertArrayEquals(bytes, person.getGlyphIcon());
        assertEquals("rgb(28, 137, 28)", person.getColor());

        Set<Concept> conceptAndAllChildren = ontologyRepository.getConceptAndAllChildren(contact);
        List<String> iris = Lists.newArrayList();
        conceptAndAllChildren.stream()
                .forEach(c -> iris.add(c.getIRI()));
        assertEquals(expectedIriSize, iris.size());
        assertTrue(iris.contains(contact.getIRI()));
        assertTrue(iris.contains(person.getIRI()));
    }

    private void validateChangedOwlRelationships() throws IOException {
        Relationship relationship = ontologyRepository.getRelationshipByIRI(TEST_IRI + "#personKnowsPerson");
        assertEquals("Person Knows Person", relationship.getDisplayName());
        assertNull(relationship.getTimeFormula());
        assertFalse(relationship.getRangeConceptIRIs().contains("http://visallo.org/test#person2"));
        assertTrue(relationship.getRangeConceptIRIs().contains("http://visallo.org/test#person"));
        assertTrue(relationship.getDomainConceptIRIs().contains("http://visallo.org/test#person"));
    }

    private void validateChangedOwlConcepts() throws IOException {
        Concept contact = ontologyRepository.getConceptByIRI(TEST_IRI + "#contact");
        Concept person = ontologyRepository.getConceptByIRI(TEST_IRI + "#person");
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

        Set<Concept> conceptAndAllChildren = ontologyRepository.getConceptAndAllChildren(contact);
        List<String> iris = Lists.newArrayList();
        conceptAndAllChildren.stream()
                .forEach(c -> iris.add(c.getIRI()));
        assertEquals(2, iris.size());
        assertTrue(iris.contains(contact.getIRI()));
        assertTrue(iris.contains(person.getIRI()));
    }

    private void validateChangedOwlProperties() throws IOException {
        OntologyProperty nameProperty = ontologyRepository.getPropertyByIRI(TEST_IRI + "#name");
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
        assertEquals("dependentProp('http://visallo.org/test#lastName') && dependentProp('http://visallo.org/test#firstName')",
                nameProperty.getValidationFormula());
        assertEquals("Personal Information", nameProperty.getPropertyGroup());
        assertEquals("test 2", nameProperty.getDisplayType());
        assertTrue(nameProperty.getAddable());
        assertTrue(nameProperty.getUpdateable());
        assertTrue(nameProperty.getDeleteable());
        Map<String, String> possibleValues = nameProperty.getPossibleValues();
        assertNull(possibleValues);

        OntologyProperty firstMetProperty = ontologyRepository.getPropertyByIRI(TEST_IRI + "#firstMet");
        assertEquals("First Met", firstMetProperty.getDisplayName());
        assertEquals(PropertyType.DATE, firstMetProperty.getDataType());

        Relationship relationship = ontologyRepository.getRelationshipByIRI(TEST_IRI + "#personKnowsPerson");
        assertTrue(relationship.getProperties()
                .stream()
                .anyMatch(p -> p.getIri().equals(firstMetProperty.getIri()))
        );
    }
}

