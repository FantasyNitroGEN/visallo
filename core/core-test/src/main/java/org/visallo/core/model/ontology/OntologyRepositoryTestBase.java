package org.visallo.core.model.ontology;

import com.beust.jcommander.internal.Lists;
import com.google.common.collect.ImmutableList;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.semanticweb.owlapi.model.IRI;
import org.vertexium.Authorizations;
import org.visallo.core.util.VisalloInMemoryTestBase;
import org.visallo.web.clientapi.model.PropertyType;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public abstract class OntologyRepositoryTestBase extends VisalloInMemoryTestBase {
    private static final String TEST_OWL = "test.owl";
    private static final String TEST_CHANGED_OWL = "test_changed.owl";
    private static final String TEST01_OWL = "test01.owl";
    private static final String GLYPH_ICON_FILE = "glyphicons_003_user@2x.png";
    private static final String TEST_IRI = "http://visallo.org/test";

    private static final String TEST_HIERARCHY_IRI = "http://visallo.org/testhierarchy";
    private static final String TEST_HIERARCHY_OWL = "test_hierarchy.owl";

    private static final String TEST01_IRI = "http://visallo.org/test01";
    private Authorizations authorizations;

    @Before
    public void before() {
        super.before();
        authorizations = getGraph().createAuthorizations();
    }

    @Test
    public void changingDisplayAnnotationsShouldSucceed() throws Exception {
        loadTestOwlFile();

        importTestOntologyFile(TEST_CHANGED_OWL, TEST_IRI);

        validateChangedOwlRelationships();
        validateChangedOwlConcepts();
        validateChangedOwlProperties();
    }

    @Test
    public void testGettingParentConceptReturnsParentProperties() throws Exception {
        importTestOntologyFile(TEST_HIERARCHY_OWL, TEST_HIERARCHY_IRI);
        Concept concept = getOntologyRepository().getConceptByIRI(TEST_HIERARCHY_IRI + "#person");
        Concept parentConcept = getOntologyRepository().getParentConcept(concept);
        assertEquals(1, parentConcept.getProperties().size());
    }

    @Test
    public void dependenciesBetweenOntologyFilesShouldNotChangeParentProperties() throws Exception {
        loadTestOwlFile();

        importTestOntologyFile(TEST01_OWL, TEST01_IRI);
        validateTestOwlRelationship();
        validateTestOwlConcepts(3);
        validateTestOwlProperties();

        OntologyProperty aliasProperty = getOntologyRepository().getPropertyByIRI(TEST01_IRI + "#alias");
        Concept person = getOntologyRepository().getConceptByIRI(TEST_IRI + "#person");
        assertTrue(person.getProperties()
                .stream()
                .anyMatch(p -> p.getIri().equals(aliasProperty.getIri()))
        );
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
        assertEquals(1, intents.length);
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

    private void validateTestOwlConcepts(int expectedIriSize) throws IOException {
        Concept contact = getOntologyRepository().getConceptByIRI(TEST_IRI + "#contact");
        assertEquals("Contact", contact.getDisplayName());
        assertEquals("rgb(149, 138, 218)", contact.getColor());
        assertEquals("test", contact.getDisplayType());
        List<String> intents = Arrays.asList(contact.getIntents());
        assertEquals(1, intents.size());
        assertTrue(intents.contains("face"));

        Concept person = getOntologyRepository().getConceptByIRI(TEST_IRI + "#person");
        assertEquals("Person", person.getDisplayName());
        intents = Arrays.asList(person.getIntents());
        assertEquals(1, intents.size());
        assertTrue(intents.contains("person"));
        assertEquals("prop('http://visallo.org/test#birthDate') || ''", person.getTimeFormula());
        assertEquals("prop('http://visallo.org/test#name') || ''", person.getTitleFormula());

        byte[] bytes = IOUtils.toByteArray(OntologyRepositoryTestBase.class.getResourceAsStream("glyphicons_003_user@2x.png"));
        assertArrayEquals(bytes, person.getGlyphIcon());
        assertEquals("rgb(28, 137, 28)", person.getColor());

        Set<Concept> conceptAndAllChildren = getOntologyRepository().getConceptAndAllChildren(contact);
        List<String> iris = Lists.newArrayList();
        conceptAndAllChildren.forEach(c -> iris.add(c.getIRI()));
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
        conceptAndAllChildren.forEach(c -> iris.add(c.getIRI()));
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
        importTestOntologyFile(TEST_OWL, TEST_IRI);
        validateTestOwlRelationship();
        validateTestOwlConcepts(2);
        validateTestOwlProperties();
    }

    private void importTestOntologyFile(String owlFileResourcePath, String iri) throws Exception {
        URI owlUri = OntologyRepositoryTestBase.class.getResource(owlFileResourcePath).toURI();
        File testOwl;
        if ("jar".equals(owlUri.getScheme())) {
            Path owlDirectoryPath = Files.createTempDirectory(OntologyRepositoryTestBase.class.getSimpleName());
            Path owlFilePath = owlDirectoryPath.resolve("test.owl");
            Path glyphIconPath = owlDirectoryPath.resolve(GLYPH_ICON_FILE);

            testOwl = owlFilePath.toFile();
            InputStream owlFileStream = OntologyRepositoryTestBase.class.getResourceAsStream(owlFileResourcePath);
            IOUtils.copy(owlFileStream, new FileOutputStream(testOwl));

            InputStream glyphIconStream = OntologyRepositoryTestBase.class.getResourceAsStream(GLYPH_ICON_FILE);
            IOUtils.copy(glyphIconStream, new FileOutputStream(glyphIconPath.toFile()));
        } else {
             testOwl = new File(owlUri);
        }
        System.out.println(testOwl);
        getOntologyRepository().importFile(testOwl, IRI.create(iri), authorizations);
    }


    @Override
    protected abstract OntologyRepository getOntologyRepository();
}
