package org.visallo.web.structuredingest;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.semanticweb.owlapi.model.IRI;
import org.vertexium.*;
import org.vertexium.inmemory.InMemoryAuthorizations;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.user.PrivilegeRepository;
import org.visallo.core.model.workspace.WorkspaceHelper;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloInMemoryTestBase;
import org.visallo.web.clientapi.model.Privilege;
import org.visallo.web.clientapi.model.VisibilityJson;
import org.visallo.web.structuredingest.core.StructuredIngestOntology;
import org.visallo.web.structuredingest.core.model.ClientApiParseErrors;
import org.visallo.web.structuredingest.core.util.GraphBuilderParserHandler;
import org.visallo.web.structuredingest.core.util.mapping.ParseMapping;
import org.visallo.web.structuredingest.core.util.mapping.PropertyMapping;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.function.Predicate;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.visallo.web.structuredingest.mapping.MappingTestHelpers.createIndexedMap;

@SuppressWarnings("ConstantConditions")
public class GraphBuilderParserHandlerTest extends VisalloInMemoryTestBase {
    private static final String WORKSPACE_ID = "testWorkspaceId";

    private static final String OWL_BASE_URI = "http://visallo.org/structured-file-test";
    private static final String PERSON_CONCEPT_TYPE = OWL_BASE_URI + "#person";
    private static final String TX_CONCEPT_TYPE = OWL_BASE_URI + "#transaction";
    private static final String PERSON_NAME_NAME = OWL_BASE_URI + "#name";
    private static final String TX_DATE_NAME = OWL_BASE_URI + "#transactionDate";
    private static final String TX_FRAUD_NAME = OWL_BASE_URI + "#suspectedFraud";
    private static final String EDGE_LABEL = OWL_BASE_URI + "#personHasTransaction";

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    private Authorizations authorizations = new InMemoryAuthorizations(WORKSPACE_ID);

    private Vertex structuredFileVertex;
    private ParseMapping parseMapping;
    private GraphBuilderParserHandler parserHandler;

    @Before
    public void setup() throws Exception {
        WorkspaceHelper workspaceHelper = new WorkspaceHelper(
                getTermMentionRepository(),
                getWorkQueueRepository(),
                getGraph(),
                getOntologyRepository(),
                getWorkspaceRepository(),
                getPrivilegeRepository(),
                getAuthorizationRepository()
        );

        byte[] inFileData = IOUtils.toByteArray(this.getClass().getResourceAsStream("sample.owl"));
        getOntologyRepository().importFileData(
                inFileData,
                IRI.create("http://visallo.org/structured-file-test"),
                null,
                authorizations
        );

        VertexBuilder structuredFileVertexBuilder = getGraph().prepareVertex(getVisibilityTranslator().getDefaultVisibility());
        VisibilityJson visibilityJson = VisibilityJson.updateVisibilitySourceAndAddWorkspaceId(new VisibilityJson(), "", WORKSPACE_ID);
        VisalloProperties.VISIBILITY_JSON.setProperty(structuredFileVertexBuilder, visibilityJson, new Visibility(""));
        structuredFileVertex = structuredFileVertexBuilder.save(authorizations);
        dateFormat.setTimeZone(TimeZone.getTimeZone("America/New_York"));

        User user = getUserRepository().findOrAddUser(
                "junit",
                "JUnit",
                "junit@v5analytics.com",
                "password"
        );
        getWorkspaceRepository().add(WORKSPACE_ID, "Default Junit", user);

        PrivilegeRepository privilegeRepository = mock(PrivilegeRepository.class);
        when(privilegeRepository.hasPrivilege(any(User.class), eq(Privilege.PUBLISH))).thenReturn(true);

        InputStream parseMappingJson = this.getClass().getResourceAsStream("parsemapping.json");
        parseMapping = new ParseMapping(getOntologyRepository(), null, null, IOUtils.toString(parseMappingJson, "UTF-8"));
        parserHandler = new GraphBuilderParserHandler(
                getGraph(),
                user,
                getVisibilityTranslator(),
                privilegeRepository,
                getGraph().createAuthorizations(WORKSPACE_ID),
                getWorkspaceRepository(),
                workspaceHelper,
                WORKSPACE_ID,
                false,
                structuredFileVertex,
                parseMapping,
                null
        );

        parserHandler.newSheet("SheetA");
    }

    @Test
    public void testAddRow() throws Exception {
        doParse(false, true, 0, new String[]{"John Smith", "3/13/2015", "yes"});

        Iterable<Vertex> vertices = getGraph().getVertices(authorizations);
        assertEquals("Expected new vertices to be created", 3, Iterables.size(vertices)); // CSV, PERSON, TX

        List<Vertex> generated = getGenerated();
        assertEquals("Should have created 2 entities", 2, generated.size());
        for (Vertex vertex : generated) {
            String conceptType = VisalloProperties.CONCEPT_TYPE.getPropertyValue(vertex);

            if (PERSON_CONCEPT_TYPE.equals(conceptType)) {
                assertEquals("Person name not set properly", "John Smith", vertex.getPropertyValue(PERSON_NAME_NAME));

                List<Property> property = Lists.newArrayList(vertex.getProperties(VisalloProperties.VISIBILITY_JSON.getPropertyName()));
                assertEquals("One visibility property", 1, property.size());

            } else if (TX_CONCEPT_TYPE.equals(conceptType)) {
                assertEquals(
                        "Incorrect concept type on tx vertex",
                        TX_CONCEPT_TYPE,
                        VisalloProperties.CONCEPT_TYPE.getPropertyValue(vertex)
                );
                assertEquals(
                        "Incorrect transaction date on tx vertex",
                        "2015-03-13",
                        dateFormat.format(vertex.getPropertyValue(TX_DATE_NAME))
                );
                assertEquals("Incorrect fraud indicator on tx vertex", Boolean.TRUE, vertex.getPropertyValue(TX_FRAUD_NAME));
            } else fail("Concept type not expected: " + conceptType);
        }

        List<Edge> edges = Lists.newArrayList(getGraph().getEdges(authorizations));
        assertEquals("Found the source and created edges", 3, edges.size());
        assertTrue("Found the edge", edges.stream().anyMatch(edge -> edge.getLabel().equals(EDGE_LABEL)));
    }

    @Test
    public void testAddRowMultipleTimes() throws Exception {
        String[] row = new String[]{"John Smith", "3/13/2015", "yes"};

        doParse(false, true, 0, row);

        Iterable<Vertex> vertices = getGraph().getVertices(authorizations);
        assertEquals("Expected new vertices to be created", 3, Iterables.size(vertices));

        List<Vertex> generated = getGenerated();
        assertEquals("Should have created 2 entities", 2, generated.size());

        Predicate<? super Vertex> hasOneConceptType = vertex -> {
            List<Property> properties = Lists.newArrayList(vertex.getProperties(VisalloProperties.CONCEPT_TYPE.getPropertyName()));
            return properties.size() == 1;
        };

        assertTrue("All have one concept type", generated.stream().allMatch(hasOneConceptType));

        parserHandler.cleanUpExistingImport();
        assertEquals("No linked entities after cleaning", 0, getGenerated().size());

        doParse(false, true, 0, row);
        assertTrue("All have one concept type after reimport", getGenerated().stream().allMatch(hasOneConceptType));
    }

        @Test
    public void testAddRowDryRun() throws Exception {
        doParse(true, true, 0, new String[]{"John Smith", "3/13/2015", "yes"});

        Iterable<Vertex> vertices = getGraph().getVertices(authorizations);
        assertEquals("Expected no new vertices to be created", 1, Iterables.size(vertices)); // CSV only
    }

    @Test
    public void testAddRowWithTooManyErrors() throws Exception {
        parserHandler.maxParseErrors = 1;

        doParse(true, false, 1, new String[]{"John Smith", "3/13/2015", "you bet"});

        Iterable<Vertex> vertices = getGraph().getVertices(authorizations);
        assertEquals("Expected no new vertices to be created", 1, Iterables.size(vertices)); // CSV only
    }

    @Test
    public void testAddRowWithUnhandledError() throws Exception {
        try {
            doParse(false, false, 1, new String[]{"John Smith", "3/13/2015", "you bet"});
            fail("An exception should have been thrown.");
        } catch (VisalloException ve) {
            // we expect this
        }
    }

    @Test
    public void testAddRowWithErrorThatSkipsCell() throws Exception {
        PropertyMapping fraudMapping = findPropertyMapping(TX_FRAUD_NAME);
        fraudMapping.errorHandlingStrategy = PropertyMapping.ErrorHandlingStrategy.SKIP_CELL;

        doParse(false, true, 0, new String[]{"John Smith", "3/13/2015", "you bet"});

        Optional<Vertex> txVertexOpt = getGenerated().stream()
                .filter(vertex -> VisalloProperties.CONCEPT_TYPE.getPropertyValue(vertex).equals(TX_CONCEPT_TYPE))
                .findFirst();

        assertTrue("Unable to find new transaction vertex", txVertexOpt.isPresent());
        Vertex txVertex = txVertexOpt.get();
        assertNull("Incorrect fraud indicator on tx vertex", txVertex.getPropertyValue(TX_FRAUD_NAME));
    }

    @Test
    public void testAddRowWithErrorThatSkipsVertex() throws Exception {
        PropertyMapping fraudMapping = findPropertyMapping(TX_FRAUD_NAME);
        fraudMapping.errorHandlingStrategy = PropertyMapping.ErrorHandlingStrategy.SKIP_VERTEX;

        doParse(false, true, 0, new String[]{"John Smith", "3/13/2015", "you bet"});

        Iterable<Vertex> vertices = getGraph().getVertices(authorizations);
        assertEquals("Expected new vertices to be created", 2, Iterables.size(vertices)); // CSV, PERSON

        assertTrue("Unable to find new person vertex",
                getGenerated().stream()
                    .anyMatch(vertex -> VisalloProperties.CONCEPT_TYPE.getPropertyValue(vertex).equals(PERSON_CONCEPT_TYPE)));

        assertFalse("Should not have found the transaction vertex",
                getGenerated().stream()
                    .anyMatch(vertex -> VisalloProperties.CONCEPT_TYPE.getPropertyValue(vertex).equals(TX_CONCEPT_TYPE)));
    }

    @Test
    public void testAddRowWithErrorThatSkipsRow() throws Exception {
        PropertyMapping fraudMapping = findPropertyMapping(TX_FRAUD_NAME);
        fraudMapping.errorHandlingStrategy = PropertyMapping.ErrorHandlingStrategy.SKIP_ROW;

        doParse(false, true, 0, new String[]{"John Smith", "3/13/2015", "you bet"});

        Iterable<Vertex> vertices = getGraph().getVertices(authorizations);
        assertEquals("Expected new vertices to be created", 1, Iterables.size(vertices)); // CSV, PERSON

        Vertex personVertex = getGraph().getVertex("PERSON_VERTEX", authorizations);
        assertNull("Should not have found new person vertex", personVertex);

        Vertex txVertex = getGraph().getVertex("TX_VERTEX", authorizations);
        assertNull("Should not have found the transaction vertex", txVertex);
    }

    @Test
    public void testAddRowWithErrorThatSetsErrorProperty() throws Exception {
        PropertyMapping fraudMapping = findPropertyMapping(TX_FRAUD_NAME);
        fraudMapping.errorHandlingStrategy = PropertyMapping.ErrorHandlingStrategy.SET_CELL_ERROR_PROPERTY;

        doParse(false, true, 0, new String[]{"John Smith", "3/13/2015", "you bet"});

        Optional<Vertex> txVertexOpt = getGenerated().stream()
                .filter(vertex -> VisalloProperties.CONCEPT_TYPE.getPropertyValue(vertex).equals(TX_CONCEPT_TYPE))
                .findFirst();

        assertTrue("Unable to find new transaction vertex", txVertexOpt.isPresent());
        Vertex txVertex = txVertexOpt.get();

        String multiKey = "0_0";
        assertEquals(
                "Incorrect error message on tx vertex",
                "Unrecognized boolean value: you bet",
                StructuredIngestOntology.ERROR_MESSAGE_PROPERTY.getPropertyValue(txVertex, multiKey)
        );
        assertEquals(
                "Incorrect raw value on tx vertex",
                "you bet",
                StructuredIngestOntology.RAW_CELL_VALUE_PROPERTY.getPropertyValue(txVertex, multiKey)
        );
        assertEquals(
                "Incorrect target property on tx vertex",
                TX_FRAUD_NAME,
                StructuredIngestOntology.TARGET_PROPERTY.getPropertyValue(txVertex, multiKey)
        );
        assertEquals(
                "Incorrect sheet on tx vertex",
                "0",
                StructuredIngestOntology.SHEET_PROPERTY.getPropertyValue(txVertex, multiKey)
        );
        assertEquals(
                "Incorrect row on tx vertex",
                "0",
                StructuredIngestOntology.ROW_PROPERTY.getPropertyValue(txVertex, multiKey)
        );
    }

    @Test
    public void testAddRowWithTooManyErrorsButNotDryRun() throws Exception {
        parserHandler.maxParseErrors = 1;

        PropertyMapping fraudMapping = findPropertyMapping(TX_FRAUD_NAME);
        fraudMapping.errorHandlingStrategy = PropertyMapping.ErrorHandlingStrategy.SKIP_CELL;

        doParse(false, true, 0, new String[]{"John Smith", "3/13/2015", "you bet"});

        Iterable<Vertex> vertices = getGraph().getVertices(authorizations);
        assertEquals("Expected new vertices to be created", 3, Iterables.size(vertices)); // CSV, PERSON, TX

        Optional<Vertex> txVertexOpt = getGenerated().stream()
                .filter(vertex -> VisalloProperties.CONCEPT_TYPE.getPropertyValue(vertex).equals(TX_CONCEPT_TYPE))
                .findFirst();

        assertTrue("Unable to find new transaction vertex", txVertexOpt.isPresent());
        Vertex txVertex = txVertexOpt.get();

        assertEquals(
                "Incorrect concept type on tx vertex",
                TX_CONCEPT_TYPE,
                VisalloProperties.CONCEPT_TYPE.getPropertyValue(txVertex)
        );
        assertEquals(
                "Incorrect transaction date on tx vertex",
                "2015-03-13",
                dateFormat.format(txVertex.getPropertyValue(TX_DATE_NAME))
        );
        assertNull(
                "Malformed boolean property value should not have been set",
                txVertex.getPropertyValue(TX_FRAUD_NAME)
        );
    }

    @Test
    public void testAddRowMaxErrorsDisabled() throws Exception {
        parserHandler.maxParseErrors = -1;
        for (int i = 0; i < 100; i++) {
            parserHandler.parseErrors.errors.add(new ClientApiParseErrors.ParseError());
        }

        doParse(true, true, 101, new String[]{"John Smith", "3/13/2015", "you bet"});

        Iterable<Vertex> vertices = getGraph().getVertices(authorizations);
        assertEquals("Expected no new vertices to be created", 1, Iterables.size(vertices)); // CSV only
    }

    @Test
    public void testAddRowThatErrorsAreRecordedProperlyInDryRun() throws Exception {
        doParse(true, true, 2, new String[]{"John Smith", "SUNDAY", "you bet"});

        ClientApiParseErrors.ParseError dateError = parserHandler.parseErrors.errors.get(0);
        Assert.assertEquals(TX_DATE_NAME, dateError.propertyMapping.name);
        assertEquals("SUNDAY", dateError.rawPropertyValue);
        assertEquals(0, dateError.rowIndex);
        assertEquals(0, dateError.sheetIndex);
        assertEquals("Unrecognized date value: SUNDAY", dateError.message);

        ClientApiParseErrors.ParseError booleanError = parserHandler.parseErrors.errors.get(1);
        Assert.assertEquals(TX_FRAUD_NAME, booleanError.propertyMapping.name);
        assertEquals("you bet", booleanError.rawPropertyValue);
        assertEquals(0, booleanError.rowIndex);
        assertEquals(0, booleanError.sheetIndex);
        assertEquals("Unrecognized boolean value: you bet", booleanError.message);

        Iterable<Vertex> vertices = getGraph().getVertices(authorizations);
        assertEquals("Expected no new vertices to be created", 1, Iterables.size(vertices)); // CSV only
    }

    @Test
    public void testCleanUpExistingImport() throws Exception {
        doParse(false, true, 0, new String[]{"John Smith", "3/13/2015", "yes"});

        Iterable<Vertex> vertices = getGraph().getVertices(authorizations);
        assertEquals("Expected new vertices to be created", 3, Iterables.size(vertices)); // CSV, PERSON, TX

        boolean cleanupResult = parserHandler.cleanUpExistingImport();

        assertTrue("Expected the result of cleaning up to be success", cleanupResult);

        vertices = getGraph().getVertices(authorizations);
        assertEquals("Expected new vertices to be created", 1, Iterables.size(vertices)); // CSV only
        assertEquals(
                "Only remaining vertex should be the structured file vertex",
                structuredFileVertex.getId(),
                Iterables.get(vertices, 0).getId()
        );
    }

    private PropertyMapping findPropertyMapping(String name) {
        for (int i = 0; i < parseMapping.vertexMappings.size(); i++) {
            for (int j = 0; j < parseMapping.vertexMappings.get(i).propertyMappings.size(); j++) {
                PropertyMapping propertyMapping = parseMapping.vertexMappings.get(i).propertyMappings.get(j);
                if (name.equals(propertyMapping.name)) {
                    return propertyMapping;
                }
            }
        }
        fail("Unable to find fraud property mapping: " + name);
        return null;
    }

    private List<Vertex> getGenerated() {
        return Lists.newArrayList(structuredFileVertex.getVertices(Direction.BOTH, StructuredIngestOntology.ELEMENT_HAS_SOURCE_IRI, authorizations));
    }

    private void doParse(boolean dryRun, boolean expectedKeepGoing, int expectedErrors, String[] rowValues) {
        parserHandler.dryRun = dryRun;
        Map<String, Object> row = createIndexedMap(rowValues);

        boolean keepGoing = parserHandler.addRow(row, 0);

        assertEquals("Incorrect return value from parserHandler.addRow", expectedKeepGoing, keepGoing);
        Assert.assertEquals(
                "Incorrect number of parsing errors recorded.",
                expectedErrors,
                parserHandler.parseErrors.errors.size()
        );
    }
}
