package org.visallo.core.model.graph;

import com.google.common.collect.ImmutableSet;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.vertexium.*;
import org.vertexium.id.QueueIdGenerator;
import org.vertexium.inmemory.InMemoryGraph;
import org.vertexium.inmemory.InMemoryGraphConfiguration;
import org.vertexium.mutation.ElementMutation;
import org.vertexium.search.DefaultSearchIndex;
import org.visallo.core.config.Configuration;
import org.visallo.core.config.ConfigurationLoader;
import org.visallo.core.config.HashMapConfigurationLoader;
import org.visallo.core.exception.VisalloResourceNotFoundException;
import org.visallo.core.model.WorkQueueNames;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.properties.types.PropertyMetadata;
import org.visallo.core.model.termMention.TermMentionRepository;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.security.DirectVisibilityTranslator;
import org.visallo.core.security.VisalloVisibility;
import org.visallo.core.user.User;
import org.visallo.model.queue.inmemory.InMemoryWorkQueueRepository;
import org.visallo.web.clientapi.model.ClientApiSourceInfo;
import org.visallo.web.clientapi.model.VisibilityJson;

import java.util.*;

import static org.junit.Assert.*;
import static org.vertexium.util.IterableUtils.toList;

@RunWith(MockitoJUnitRunner.class)
public class GraphRepositoryTest {
    private static final String WORKSPACE_ID = "testWorkspaceId";
    private static final String ENTITY_1_VERTEX_ID = "entity1Id";
    private static final Visibility SECRET_VISALLO_VIZ = new VisalloVisibility(
            Visibility.and(ImmutableSet.of("secret"))).getVisibility();
    private static final Visibility SECRET_AND_WORKSPACE_VISALLO_VIZ = new VisalloVisibility(
            Visibility.and(ImmutableSet.of("secret", WORKSPACE_ID))).getVisibility();
    private static final Visibility WORKSPACE_VIZ = new Visibility(WORKSPACE_ID);

    private GraphRepository graphRepository;
    private InMemoryGraph graph;

    @Mock
    private User user1;

    @Mock
    private TermMentionRepository termMentionRepository;

    private WorkQueueRepository workQueueRepository;

    private Authorizations defaultAuthorizations;
    private DirectVisibilityTranslator visibilityTranslator;

    @Before
    public void setup() throws Exception {
        Map config = new HashMap();
        ConfigurationLoader hashMapConfigurationLoader = new HashMapConfigurationLoader(config);
        Configuration configuration = new Configuration(hashMapConfigurationLoader, new HashMap<>());

        InMemoryGraphConfiguration graphConfig = new InMemoryGraphConfiguration(new HashMap<>());
        QueueIdGenerator idGenerator = new QueueIdGenerator();
        visibilityTranslator = new DirectVisibilityTranslator();
        graph = InMemoryGraph.create(graphConfig, idGenerator, new DefaultSearchIndex(graphConfig));
        defaultAuthorizations = graph.createAuthorizations();
        WorkQueueNames workQueueNames = new WorkQueueNames(configuration);
        workQueueRepository = new InMemoryWorkQueueRepository(graph, workQueueNames, configuration);

        graphRepository = new GraphRepository(
                graph,
                visibilityTranslator,
                termMentionRepository,
                workQueueRepository
        );
    }

    @Test
    public void testUpdatePropertyVisibilitySource() {
        Authorizations authorizations = graph.createAuthorizations("A");
        Visibility newVisibility = visibilityTranslator.toVisibility("A").getVisibility();

        Vertex v1 = graph.prepareVertex(ENTITY_1_VERTEX_ID, new VisalloVisibility().getVisibility())
                .addPropertyValue("k1", "p1", "value1", new Visibility(""))
                .save(authorizations);

        Property p1 = graphRepository.updatePropertyVisibilitySource(
                v1,
                "k1",
                "p1",
                "",
                "A",
                WORKSPACE_ID,
                user1,
                defaultAuthorizations
        );
        assertEquals(newVisibility, p1.getVisibility());
        graph.flush();

        v1 = graph.getVertex(ENTITY_1_VERTEX_ID, authorizations);
        p1 = v1.getProperty("k1", "p1", newVisibility);
        assertNotNull("could not find p1", p1);
        assertEquals(newVisibility, p1.getVisibility());
        VisibilityJson visibilityJson = VisalloProperties.VISIBILITY_JSON_METADATA
                .getMetadataValue(p1.getMetadata());
        assertEquals("A", visibilityJson.getSource());
    }

    @Test
    public void testUpdatePropertyVisibilitySourceMissingProperty() {
        Authorizations authorizations = graph.createAuthorizations("A");

        Vertex v1 = graph.prepareVertex(ENTITY_1_VERTEX_ID, new VisalloVisibility().getVisibility())
                .addPropertyValue("k1", "p1", "value1", new Visibility(""))
                .save(authorizations);

        try {
            graphRepository.updatePropertyVisibilitySource(
                    v1,
                    "k1",
                    "pNotFound",
                    "",
                    "A",
                    WORKSPACE_ID,
                    user1,
                    defaultAuthorizations
            );
            fail("expected exception");
        } catch (VisalloResourceNotFoundException ex) {
            // OK
        }
    }

    @Test
    public void testSetWorkspaceOnlyChangePropertyTwice() {
        Vertex vertex = graph.prepareVertex(ENTITY_1_VERTEX_ID, new VisalloVisibility().getVisibility())
                .save(defaultAuthorizations);

        final Authorizations workspaceAuthorizations = graph.createAuthorizations(WORKSPACE_ID);

        setProperty(vertex, "newValue1", WORKSPACE_ID, workspaceAuthorizations);

        vertex = graph.getVertex(vertex.getId(), defaultAuthorizations);
        List<Property> properties = toList(vertex.getProperties());
        assertEquals(0, properties.size());

        vertex = graph.getVertex(vertex.getId(), workspaceAuthorizations);
        properties = toList(vertex.getProperties());
        assertEquals(1, properties.size());
        assertEquals("newValue1", properties.get(0).getValue());

        setProperty(vertex, "newValue2", WORKSPACE_ID, workspaceAuthorizations);

        vertex = graph.getVertex(vertex.getId(), defaultAuthorizations);
        properties = toList(vertex.getProperties());
        assertEquals(0, properties.size());

        vertex = graph.getVertex(vertex.getId(), workspaceAuthorizations);
        properties = toList(vertex.getProperties());
        assertEquals(1, properties.size());
        assertEquals("newValue2", properties.get(0).getValue());
    }

    @Test
    public void testSandboxPropertyChangesShouldUpdateSameProperty() {
        final Authorizations authorizations = graph.createAuthorizations("foo", "bar", "baz", WORKSPACE_ID);

        Vertex vertex = graph.prepareVertex(ENTITY_1_VERTEX_ID, new VisalloVisibility().getVisibility())
                .save(authorizations);

        // new property with visibility
        String propertyValue = "newValue1";
        setProperty(vertex, propertyValue, null, "foo", WORKSPACE_ID, authorizations);

        List<Property> properties = toList(vertex.getProperties());
        Visibility fooVisibility = new VisalloVisibility(Visibility.and(ImmutableSet.of("foo", WORKSPACE_ID)))
                .getVisibility();
        assertEquals(1, properties.size());
        assertEquals(propertyValue, properties.get(0).getValue());
        assertEquals(fooVisibility, properties.get(0).getVisibility());

        // existing property, new visibility
        setProperty(vertex, propertyValue, "foo", "bar", WORKSPACE_ID, authorizations);

        properties = toList(vertex.getProperties());
        Visibility barVisibility = new VisalloVisibility(Visibility.and(ImmutableSet.of("bar", WORKSPACE_ID)))
                .getVisibility();
        assertEquals(1, properties.size());
        assertEquals(propertyValue, properties.get(0).getValue());
        assertEquals(barVisibility, properties.get(0).getVisibility());

        // existing property, new value
        propertyValue = "newValue2";
        setProperty(vertex, propertyValue, null, "bar", WORKSPACE_ID, authorizations);

        properties = toList(vertex.getProperties());
        assertEquals(1, properties.size());
        assertEquals(propertyValue, properties.get(0).getValue());
        assertEquals(barVisibility, properties.get(0).getVisibility());

        // existing property, new visibility,  new value
        propertyValue = "newValue3";
        setProperty(vertex, propertyValue, "bar", "baz", WORKSPACE_ID, authorizations);

        properties = toList(vertex.getProperties());
        Visibility bazVisibility = new VisalloVisibility(Visibility.and(ImmutableSet.of("baz", WORKSPACE_ID)))
                .getVisibility();
        assertEquals(1, properties.size());
        assertEquals(propertyValue, properties.get(0).getValue());
        assertEquals(bazVisibility, properties.get(0).getVisibility());
    }

    @Test
    public void existingPublicPropertySavedWithWorkspaceIsSandboxed() {
        final Authorizations authorizations = graph.createAuthorizations("secret", WORKSPACE_ID);

        Vertex vertex = graph.prepareVertex(ENTITY_1_VERTEX_ID, new VisalloVisibility().getVisibility())
                .save(authorizations);

        // save property without workspace, which will be public

        String publicValue = "publicValue";
        setProperty(vertex, publicValue, null, "secret", null, authorizations);

        List<Property> properties = toList(vertex.getProperties());

        assertEquals(1, properties.size());
        Property property = properties.get(0);
        assertEquals(publicValue, property.getValue());
        assertEquals(SECRET_VISALLO_VIZ, property.getVisibility());
        assertFalse(property.getHiddenVisibilities().iterator().hasNext());

        // save property with workspace, which will be sandboxed

        String sandboxedValue = "sandboxedValue";
        setProperty(vertex, sandboxedValue, null, "secret", WORKSPACE_ID, authorizations);

        properties = toList(vertex.getProperties());

        assertEquals(2, properties.size());

        property = properties.get(0); // the sandboxed property

        assertEquals(sandboxedValue, property.getValue());
        assertEquals(SECRET_AND_WORKSPACE_VISALLO_VIZ, property.getVisibility());
        assertFalse(property.getHiddenVisibilities().iterator().hasNext());

        property = properties.get(1); // the public property
        Iterator<Visibility> hiddenVisibilities = property.getHiddenVisibilities().iterator();
        assertEquals(publicValue, property.getValue());
        assertEquals(SECRET_VISALLO_VIZ, property.getVisibility());
        assertTrue(hiddenVisibilities.hasNext());
        assertEquals(WORKSPACE_VIZ, hiddenVisibilities.next());
    }

    @Test
    public void newPropertySavedWithoutWorkspaceIsPublic() {
        final Authorizations authorizations = graph.createAuthorizations("secret");

        Vertex vertex = graph.prepareVertex(ENTITY_1_VERTEX_ID, new VisalloVisibility().getVisibility())
                .save(authorizations);

        String propertyValue = "newValue";
        setProperty(vertex, propertyValue, null, "secret", null, authorizations);

        List<Property> properties = toList(vertex.getProperties());

        assertEquals(1, properties.size());
        Property property = properties.get(0);
        assertEquals(propertyValue, property.getValue());
        assertEquals(SECRET_VISALLO_VIZ, property.getVisibility());
        assertFalse(property.getHiddenVisibilities().iterator().hasNext());
    }

    @Test
    public void newPropertySavedWithWorkspaceIsSandboxed() {
        final Authorizations authorizations = graph.createAuthorizations("secret", WORKSPACE_ID);

        Vertex vertex = graph.prepareVertex(ENTITY_1_VERTEX_ID, new VisalloVisibility().getVisibility())
                .save(authorizations);

        String propertyValue = "newValue";
        setProperty(vertex, propertyValue, null, "secret", WORKSPACE_ID, authorizations);

        List<Property> properties = toList(vertex.getProperties());

        assertEquals(1, properties.size());
        Property property = properties.get(0);
        assertEquals(propertyValue, property.getValue());
        assertEquals(SECRET_AND_WORKSPACE_VISALLO_VIZ, property.getVisibility());
        assertFalse(property.getHiddenVisibilities().iterator().hasNext());
    }

    @Test
    public void testBeginGraphUpdate() throws Exception {
        Date modifiedDate = new Date();
        VisibilityJson visibilityJson = new VisibilityJson();
        PropertyMetadata metadata = new PropertyMetadata(modifiedDate, user1, visibilityJson, new Visibility(""));

        try (GraphUpdateContext ctx = graphRepository.beginGraphUpdate(Priority.NORMAL, user1, defaultAuthorizations)) {
            ElementMutation<Vertex> m = graph.prepareVertex("v1", new Visibility(""));
            Vertex v1 = ctx.update(m, modifiedDate, visibilityJson, "http://visallo.org/text#concept1", updateContext -> {
                VisalloProperties.FILE_NAME.updateProperty(updateContext, "k1", "test1.txt", metadata);
            });
            assertNotNull(v1);

            m = graph.prepareVertex("v2", new Visibility(""));
            Vertex v2 = ctx.update(m, updateContext -> {
                updateContext.updateBuiltInProperties(modifiedDate, visibilityJson);
                updateContext.setConceptType("http://visallo.org/text#concept1");
                VisalloProperties.FILE_NAME.updateProperty(updateContext, "k1", "test2.txt", metadata);
            });
            assertNotNull(v2);
        }

        List<JSONObject> queue = InMemoryWorkQueueRepository.getQueue("graphProperty");
        assertEquals(8, queue.size());
        assertWorkQueueContains(queue, "v1", "", VisalloProperties.MODIFIED_DATE.getPropertyName());
        assertWorkQueueContains(queue, "v1", "", VisalloProperties.VISIBILITY_JSON.getPropertyName());
        assertWorkQueueContains(queue, "v1", "", VisalloProperties.CONCEPT_TYPE.getPropertyName());
        assertWorkQueueContains(queue, "v1", "k1", VisalloProperties.FILE_NAME.getPropertyName());
        assertWorkQueueContains(queue, "v2", "", VisalloProperties.MODIFIED_DATE.getPropertyName());
        assertWorkQueueContains(queue, "v2", "", VisalloProperties.VISIBILITY_JSON.getPropertyName());
        assertWorkQueueContains(queue, "v2", "", VisalloProperties.CONCEPT_TYPE.getPropertyName());
        assertWorkQueueContains(queue, "v2", "k1", VisalloProperties.FILE_NAME.getPropertyName());

        Vertex v1 = graph.getVertex("v1", defaultAuthorizations);
        assertEquals("test1.txt", VisalloProperties.FILE_NAME.getFirstPropertyValue(v1));
        assertEquals("http://visallo.org/text#concept1", VisalloProperties.CONCEPT_TYPE.getPropertyValue(v1));
        assertEquals(modifiedDate, VisalloProperties.MODIFIED_DATE.getPropertyValue(v1));
        assertEquals(user1.getUserId(), VisalloProperties.MODIFIED_BY.getPropertyValue(v1));
        assertEquals(visibilityJson, VisalloProperties.VISIBILITY_JSON.getPropertyValue(v1));
    }

    private void assertWorkQueueContains(List<JSONObject> queue, String vertexId, String propertyKey, String propertyName) {
        for (JSONObject item : queue) {
            if (item.getString("graphVertexId").equals(vertexId)
                    && item.getString("propertyKey").equals(propertyKey)
                    && item.getString("propertyName").equals(propertyName)) {
                return;
            }
        }
        fail("Could not find queue item " + vertexId + ", " + propertyKey + ", " + propertyName);
    }

    private void setProperty(Vertex vertex, String value, String workspaceId, Authorizations workspaceAuthorizations) {
        setProperty(vertex, value, "", "", workspaceId, workspaceAuthorizations);
    }

    private void setProperty(
            Vertex vertex, String value, String oldVisibility, String newVisibility,
            String workspaceId, Authorizations workspaceAuthorizations
    ) {
        VisibilityAndElementMutation<Vertex> setPropertyResult = graphRepository.setProperty(
                vertex,
                "prop1",
                "key1",
                value,
                new Metadata(),
                oldVisibility,
                newVisibility,
                workspaceId,
                "I changed it",
                new ClientApiSourceInfo(),
                user1,
                workspaceAuthorizations
        );
        setPropertyResult.elementMutation.save(workspaceAuthorizations);
        graph.flush();
    }
}

