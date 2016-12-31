package org.visallo.web.structuredingest.parquet;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.hamcrest.Description;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.vertexium.*;
import org.vertexium.inmemory.InMemoryGraph;
import org.vertexium.inmemory.InMemoryGraphConfiguration;
import org.vertexium.util.IterableUtils;
import org.visallo.core.model.ontology.OntologyProperty;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.user.PrivilegeRepository;
import org.visallo.core.model.workspace.Workspace;
import org.visallo.core.model.workspace.WorkspaceHelper;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.security.DirectVisibilityTranslator;
import org.visallo.core.security.VisibilityTranslator;
import org.visallo.core.user.User;
import org.visallo.web.clientapi.model.Privilege;
import org.visallo.web.clientapi.model.VisibilityJson;
import org.visallo.web.structuredingest.core.StructuredIngestOntology;
import org.visallo.web.structuredingest.core.model.ClientApiAnalysis;
import org.visallo.web.structuredingest.core.model.ParseOptions;
import org.visallo.web.structuredingest.core.util.GraphBuilderParserHandler;
import org.visallo.web.structuredingest.core.util.ProgressReporter;
import org.visallo.web.structuredingest.core.util.mapping.ColumnMappingType;
import org.visallo.web.structuredingest.core.util.mapping.EdgeMapping;
import org.visallo.web.structuredingest.core.util.mapping.ParseMapping;
import org.visallo.web.structuredingest.core.util.mapping.PropertyMapping;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ParquetStructuredIngestParserTest {
    private Graph graph;
    final String WORKSPACE_ID_A = "wsA";
    final String WORKSPACE_ID_B = "wsB";
    final String PARQUET_FILE_VERTEX_ID = "fileVertex";
    private Vertex structuredFileVertex;
    private User user;
    private WorkspaceRepository workspaceRepository;
    private OntologyRepository ontologyRepository;
    private Authorizations WORKSPACE_AUTHORIZATIONS_A;
    private Authorizations WORKSPACE_AUTHORIZATIONS_B;

    @Before
    public void before() {
        Map<String, Object> configuration = Maps.newHashMap();
        graph = InMemoryGraph.create(new InMemoryGraphConfiguration(configuration));
        Authorizations emptyAuthorizations = graph.createAuthorizations();
        WORKSPACE_AUTHORIZATIONS_A = graph.createAuthorizations(WORKSPACE_ID_A);
        WORKSPACE_AUTHORIZATIONS_B = graph.createAuthorizations(WORKSPACE_ID_B);

        VertexBuilder wsAVertexBuilder = graph.prepareVertex(WORKSPACE_ID_A, Visibility.EMPTY);
        wsAVertexBuilder.save(emptyAuthorizations);
        VertexBuilder wsBVertexBuilder = graph.prepareVertex(WORKSPACE_ID_B, Visibility.EMPTY);
        wsBVertexBuilder.save(emptyAuthorizations);

        VertexBuilder structuredVertexBuilder = graph.prepareVertex(PARQUET_FILE_VERTEX_ID, Visibility.EMPTY);
        VisalloProperties.VISIBILITY_JSON.setProperty(structuredVertexBuilder, new VisibilityJson(), Visibility.EMPTY);
        this.structuredFileVertex = structuredVertexBuilder.save(emptyAuthorizations);

        graph.flush();

        this.user = mock(User.class);
        when(user.getUserId()).thenReturn("user1");
        ontologyRepository = mock(OntologyRepository.class);
        OntologyProperty ontologyProperty = mock(OntologyProperty.class);
        when(ontologyRepository.getPropertyByIRI(anyString())).thenReturn(ontologyProperty);

        this.workspaceRepository = mock(WorkspaceRepository.class);
        Workspace workspaceA = mock(Workspace.class);
        when(workspaceA.getWorkspaceId()).thenReturn(WORKSPACE_ID_A);
        Workspace workspaceB = mock(Workspace.class);
        when(workspaceB.getWorkspaceId()).thenReturn(WORKSPACE_ID_B);
        when(workspaceRepository.findById(eq(WORKSPACE_ID_A), eq(user))).thenReturn(workspaceA);
        when(workspaceRepository.findById(eq(WORKSPACE_ID_B), eq(user))).thenReturn(workspaceB);
    }

    @Test
    public void analyze() throws Exception {
        ParquetStructuredIngestParser p = new ParquetStructuredIngestParser();

        ClientApiAnalysis analyze = p.analyze(getTestFileInputStream());
        List<ClientApiAnalysis.Sheet> sheets = analyze.sheets;

        assertThat(sheets.size(), is(1));
        ClientApiAnalysis.Sheet sheet = sheets.get(0);
        List<ClientApiAnalysis.Column> columns = sheet.columns;
        List<String> columnNames = Arrays.asList("application_id",
                "application_date", "social_security_number",
                "home_phone_num", "email_address",
                "str_address_1", "str_address_2",
                "city", "zip5", "is_fraud");
        List<ColumnMappingType> columnTypes = Arrays.asList(ColumnMappingType.Number,
                ColumnMappingType.Date, ColumnMappingType.String,
                ColumnMappingType.String, ColumnMappingType.String,
                ColumnMappingType.String, ColumnMappingType.String,
                ColumnMappingType.String, ColumnMappingType.String, ColumnMappingType.Boolean);
        for (int i = 0; i < sheet.columns.size(); i++) {
            assertThat(columns.get(i).name, is(columnNames.get(i)));
            assertThat(columns.get(i).type, is(columnTypes.get(i)));
        }

        assertEquals(sheet.parsedRows.size(), columnNames.size());
        assertEquals(sheet.parsedRows.get(0).columns.get(0), "681123");
        String dateField = (String)sheet.parsedRows.get(0).columns.get(1);
        SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        assertEquals(1427760000000L, df.parse(dateField).getTime());
        assertEquals(sheet.parsedRows.get(0).columns.get(2), "552-43-0086");
        assertEquals(sheet.parsedRows.get(0).columns.get(3), "503-452-8413");
        assertEquals(sheet.parsedRows.get(0).columns.get(4), "evan.p8759@hotmail.com");
        assertEquals(sheet.parsedRows.get(0).columns.get(5), "102 S Derek Dr");
        assertEquals(sheet.parsedRows.get(0).columns.get(6), "");
        assertEquals(sheet.parsedRows.get(0).columns.get(7), "Fullerton");
        assertEquals(sheet.parsedRows.get(0).columns.get(8), "92831");
        assertEquals(sheet.parsedRows.get(0).columns.get(9), "false");
    }

    @Test
    public void ingestSetsConceptType() throws Exception {
        InputStream inputStream = getTestFileInputStream();

        List<List<JSONObject>> vertexMappings = Collections.singletonList(Arrays.asList(conceptTypeObj("testConceptType")));
        GraphBuilderParserHandler graphBuilderParserHandler = setupGraphBuilder(parseMapping(vertexMappings, null));
        new ParquetStructuredIngestParser().ingest(inputStream, new ParseOptions(), graphBuilderParserHandler);

        graph.flush();
        Vertex vertex = graph.getVertex(PARQUET_FILE_VERTEX_ID, WORKSPACE_AUTHORIZATIONS_A);
        assertThat(IterableUtils.count(graph.getVertices(WORKSPACE_AUTHORIZATIONS_A)), is(23));

        List<Vertex> vertices = Lists.newArrayList(vertex.getVertices(Direction.BOTH, StructuredIngestOntology.ELEMENT_HAS_SOURCE_IRI, WORKSPACE_AUTHORIZATIONS_A));
        assertConceptTypeOfAllVertices(vertices, "testConceptType");
    }

    @Test
    public void ingestSetsPropertyOnVertices() throws Exception {

        InputStream inputStream = getTestFileInputStream();

        List<List<JSONObject>> vertexMappings = Arrays.asList(Lists.newArrayList(
                conceptTypeObj("testConceptType"),
                nameKeyObj("http://visallo.org/testontology#ssn", "social_security_number"))
        );
        GraphBuilderParserHandler graphBuilderParserHandler = setupGraphBuilder(parseMapping(vertexMappings, null));
        new ParquetStructuredIngestParser().ingest(inputStream, new ParseOptions(), graphBuilderParserHandler);

        graph.flush();
        Vertex vertex = graph.getVertex(PARQUET_FILE_VERTEX_ID, WORKSPACE_AUTHORIZATIONS_A);

        List<Vertex> vertices = Lists.newArrayList(vertex.getVertices(Direction.BOTH, StructuredIngestOntology.ELEMENT_HAS_SOURCE_IRI, WORKSPACE_AUTHORIZATIONS_A));

        assertThat(IterableUtils.count(graph.getVertices(WORKSPACE_AUTHORIZATIONS_A)), is(23));
        assertConceptTypeOfAllVertices(vertices, "testConceptType");
        assertPropertyMatchesPattern(vertices, "http://visallo.org/testontology#ssn", "[0-9]{3}-[0-9]{2}-[0-9]{4}");
    }

    @Test
    public void ingestCreatesMultipleVertices() throws Exception {
        assertThat(Lists.newArrayList(graph.getVertices(WORKSPACE_AUTHORIZATIONS_A)).size(), is(3));


        InputStream inputStream = getTestFileInputStream();

        List<List<JSONObject>> vertexMappings = Arrays.asList(Lists.newArrayList(
                conceptTypeObj("testConceptType"),
                nameKeyObj("http://visallo.org/testontology#ssn", "social_security_number")),
                Lists.newArrayList(conceptTypeObj("otherConceptType"))
        );
        GraphBuilderParserHandler graphBuilderParserHandler = setupGraphBuilder(parseMapping(vertexMappings, null));
        new ParquetStructuredIngestParser().ingest(inputStream, new ParseOptions(), graphBuilderParserHandler);

        graph.flush();
        Vertex vertex = graph.getVertex(PARQUET_FILE_VERTEX_ID, WORKSPACE_AUTHORIZATIONS_A);

        List<Edge> edges = Lists.newArrayList(vertex.getEdges(Direction.IN, StructuredIngestOntology.ELEMENT_HAS_SOURCE_IRI, WORKSPACE_AUTHORIZATIONS_A));
        List<Vertex> vertices = Lists.newArrayList(vertex.getVertices(Direction.IN, StructuredIngestOntology.ELEMENT_HAS_SOURCE_IRI, WORKSPACE_AUTHORIZATIONS_A));
        assertThat(vertices.size(), is(40));
        assertThat(edges.size(), is(40));
        List<Vertex> allVertices = Lists.newArrayList(graph.getVertices(WORKSPACE_AUTHORIZATIONS_A));
        assertThat(allVertices.size(), is(43));

        assertConceptTypeOfAllVertices(vertices, "testConceptType", "otherConceptType");
    }

    @Test
    public void ingestCreatesMultipleVerticesWithIdentifiers() throws Exception {
        InputStream inputStream = getTestFileInputStream();

        List<List<JSONObject>> vertexMappings = Arrays.asList(
                Lists.newArrayList(
                    conceptTypeObj("testConceptType"),
                    nameKeyObj("http://visallo.org/testontology#ssn", "social_security_number").put("isIdentifier", true)
                ),
                Lists.newArrayList(conceptTypeObj("otherConceptType"))
        );
        GraphBuilderParserHandler graphBuilderParserHandler = setupGraphBuilder(parseMapping(vertexMappings, null));
        new ParquetStructuredIngestParser().ingest(inputStream, new ParseOptions(), graphBuilderParserHandler);

        graph.flush();
        Vertex vertex = graph.getVertex(PARQUET_FILE_VERTEX_ID, WORKSPACE_AUTHORIZATIONS_A);

        List<Edge> edges = Lists.newArrayList(vertex.getEdges(Direction.BOTH, StructuredIngestOntology.ELEMENT_HAS_SOURCE_IRI, WORKSPACE_AUTHORIZATIONS_A));
        assertThat(edges.size(), is(40));

        List<Vertex> vertices = getStructuredVerticesCreated();
        assertThat(vertices.size(), is(40));

        List<Vertex> allVertices = Lists.newArrayList(graph.getVertices(WORKSPACE_AUTHORIZATIONS_A));
        assertThat(allVertices.size(), is(43));

        assertConceptTypeOfAllVertices(vertices, "testConceptType", "otherConceptType");
    }

    @Test
    public void ingestAcrossWorkspaces() throws Exception {
        List<List<JSONObject>> vertexMappings = Arrays.asList(
            Lists.newArrayList(
                conceptTypeObj("testConceptType"),
                nameKeyObj("http://visallo.org/testontology#ssn", "social_security_number").put("isIdentifier", true)
            )
        );
        JSONObject mapping = parseMapping(vertexMappings, null);

        // Workspace A
        GraphBuilderParserHandler handlerA = setupGraphBuilder(mapping, WORKSPACE_ID_A);
        new ParquetStructuredIngestParser().ingest(getTestFileInputStream(), new ParseOptions(), handlerA);

        assertThat(getStructuredVerticesCreated(WORKSPACE_AUTHORIZATIONS_A).size(), is(20));

        GraphBuilderParserHandler handlerB = setupGraphBuilder(mapping, WORKSPACE_ID_B);
        new ParquetStructuredIngestParser().ingest(getTestFileInputStream(), new ParseOptions(), handlerB);

        assertThat("Vertices created afterwards in B exist",
                getStructuredVerticesCreated(WORKSPACE_AUTHORIZATIONS_B).size(), is(20));

        assertThat("Previously created are still available in A",
                getStructuredVerticesCreated(WORKSPACE_AUTHORIZATIONS_A).size(), is(20));
    }

    private List<Vertex> getStructuredVerticesCreated() {
        return getStructuredVerticesCreated(WORKSPACE_AUTHORIZATIONS_A);
    }

    private List<Vertex> getStructuredVerticesCreated(Authorizations authorizations) {
        graph.flush();
        Vertex vertex = graph.getVertex(PARQUET_FILE_VERTEX_ID, authorizations);
        List<Vertex> vertices = Lists.newArrayList(vertex.getVertices(Direction.BOTH, StructuredIngestOntology.ELEMENT_HAS_SOURCE_IRI, authorizations));
        return vertices;
    }

    @Test
    public void ingestCreatesRelationshipsOnVertices() throws Exception {
        InputStream inputStream = getTestFileInputStream();

        List<List<JSONObject>> vertexMappings = Arrays.asList(Lists.newArrayList(
                conceptTypeObj("testConceptType"),
                nameKeyObj("http://visallo.org/testontology#ssn", "social_security_number")),
                Lists.newArrayList(conceptTypeObj("otherConceptType"))
        );

        List<JSONObject> edgeMappings = Arrays.asList(createEdge(0, 1, "testEdgeLabel"));
        GraphBuilderParserHandler graphBuilderParserHandler = setupGraphBuilder(parseMapping(vertexMappings, edgeMappings));
        new ParquetStructuredIngestParser().ingest(inputStream, new ParseOptions(), graphBuilderParserHandler);

        graph.flush();
        Vertex vertex = graph.getVertex(PARQUET_FILE_VERTEX_ID, WORKSPACE_AUTHORIZATIONS_A);

        List<Vertex> vertices = Lists.newArrayList(vertex.getVertices(Direction.BOTH, StructuredIngestOntology.ELEMENT_HAS_SOURCE_IRI, WORKSPACE_AUTHORIZATIONS_A));

        assertThat(IterableUtils.count(graph.getVertices(WORKSPACE_AUTHORIZATIONS_A)), is(43));

        assertThat(IterableUtils.count(graph.getEdges(WORKSPACE_AUTHORIZATIONS_A)), is(60));
        Map<String, Integer> edgeHistogram = edgeHistogram(graph.getEdges(WORKSPACE_AUTHORIZATIONS_A));

        assertThat(edgeHistogram.get("testEdgeLabel"), is(20));
        assertThat(edgeHistogram.get(StructuredIngestOntology.ELEMENT_HAS_SOURCE_IRI), is(40));

        assertConceptTypeOfAllVertices(vertices, "testConceptType", "otherConceptType");
    }

    private Map<String, Integer> edgeHistogram(Iterable<Edge> edges) {
        Map<String, Integer> m = Maps.newHashMap();
        for (Edge edge : edges) {
            m.putIfAbsent(edge.getLabel(), 0);
            m.put(edge.getLabel(), m.get(edge.getLabel()) + 1);
        }

        return m;
    }

    private JSONObject createEdge(int inVertexId, int outVertexId, String label) {
        return new JSONObject().put(EdgeMapping.PROPERTY_MAPPING_IN_VERTEX_KEY, inVertexId)
                .put(EdgeMapping.PROPERTY_MAPPING_OUT_VERTEX_KEY, outVertexId)
                .put(EdgeMapping.PROPERTY_MAPPING_LABEL_KEY, label);
    }

    private void assertConceptTypeOfAllVertices(Iterable<Vertex> vertices, String... conceptTypes) {
        for (Vertex v : vertices) {
            assertThat(VisalloProperties.CONCEPT_TYPE.getPropertyValue(v), is(new org.hamcrest.BaseMatcher<String>() {
                @Override
                public boolean matches(Object o) {
                    return Arrays.asList(conceptTypes).contains(o.toString());
                }

                @Override
                public void describeMismatch(Object o, Description description) {
                    description.appendText(String.format("%s is not in [%s]", o.toString(), Joiner.on(",").join(conceptTypes)));
                }

                @Override
                public void describeTo(Description description) {

                }
            }));
        }
    }

    private void assertPropertyMatchesPattern(Iterable<Vertex> vertices, String iri, String matches) {
        Pattern pattern = Pattern.compile(matches);

        for (Vertex v : vertices) {
            List<Object> propertyValues = IterableUtils.toList(v.getPropertyValues(iri));
            assertThat(propertyValues.size(), is(1));
            Matcher matcher = pattern.matcher(propertyValues.get(0).toString());
            if (!matcher.matches()) {
                System.out.println(propertyValues.get(0));
            }
            assertTrue(matcher.matches());
        }
    }

    private GraphBuilderParserHandler setupGraphBuilder(JSONObject mapping) {
        return setupGraphBuilder(mapping, WORKSPACE_ID_A);
    }

    private GraphBuilderParserHandler setupGraphBuilder(JSONObject mapping, String workspaceId) {
        WorkspaceHelper workspaceHelper = mock(WorkspaceHelper.class);
        VisibilityTranslator visibilityTranslator = new DirectVisibilityTranslator();

        ParseMapping parseMapping = new ParseMapping(ontologyRepository, visibilityTranslator, workspaceId, mapping);
        ProgressReporter progressReporter = mock(ProgressReporter.class);

        PrivilegeRepository privilegeRepository = mock(PrivilegeRepository.class);
        when(privilegeRepository.hasPrivilege(any(User.class), eq(Privilege.PUBLISH))).thenReturn(true);

        GraphBuilderParserHandler graphBuilderParserHandler = new GraphBuilderParserHandler(graph, user, visibilityTranslator, privilegeRepository, graph.createAuthorizations(), workspaceRepository, workspaceHelper, workspaceId, false, structuredFileVertex, parseMapping, progressReporter);
        graphBuilderParserHandler.dryRun = false;
        return graphBuilderParserHandler;
    }

    private static InputStream getTestFileInputStream() {
        return ParquetStructuredIngestParserTest.class.getResourceAsStream("test-parquet-file.snappy.parquet");
    }

    private static JSONObject arrayWithProperties(String property, JSONObject... objs) {
        return new JSONObject().put(property, objListToArr(objs));
    }

    private static JSONObject arrayWithProperties(String property, JSONArray arr) {
        return new JSONObject().put(property, arr);
    }

    private static JSONObject parseMapping(List<List<JSONObject>> vertexMappings, List<JSONObject> edgeMappings) {
        JSONArray vertexMapping = new JSONArray();
        for (List<JSONObject> objs : vertexMappings) {
            vertexMapping.put(arrayWithProperties("properties", objListToArr(objs)));
        }

        return parseMapping(vertexMapping, objListToArr(edgeMappings));
    }

    private static JSONObject conceptTypeObj(String conceptType) {
        return new JSONObject()
                .put(PropertyMapping.PROPERTY_MAPPING_NAME_KEY, VisalloProperties.CONCEPT_TYPE.getPropertyName())
                .put(PropertyMapping.PROPERTY_MAPPING_VALUE_KEY, conceptType);
    }

    private static JSONObject nameKeyObj(String name, String key) {
        return new JSONObject()
                .put(PropertyMapping.PROPERTY_MAPPING_NAME_KEY, name)
                .put(PropertyMapping.PROPERTY_MAPPING_KEY_KEY, key);
    }

    private static JSONArray objListToArr(List<JSONObject> objs) {
        JSONArray arr = new JSONArray();
        if (objs == null) {
            return arr;
        }

        for (JSONObject obj : objs) {
            arr.put(obj);
        }

        return arr;
    }

    private static JSONArray objListToArr(JSONObject... objs) {
        return objListToArr(Arrays.asList(objs));
    }

    private static JSONObject parseMapping(JSONArray vertexMapping, JSONArray edgeMapping) {
        return new JSONObject()
                .put("vertices", vertexMapping == null ? new JSONArray() : vertexMapping)
                .put("edges", edgeMapping == null ? new JSONArray() : edgeMapping);
    }
}