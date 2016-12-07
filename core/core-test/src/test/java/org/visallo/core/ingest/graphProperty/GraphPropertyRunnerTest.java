package org.visallo.core.ingest.graphProperty;

import com.codahale.metrics.Counter;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.vertexium.*;
import org.visallo.core.config.Configuration;
import org.visallo.core.model.user.AuthorizationRepository;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.status.JmxMetricsManager;
import org.visallo.core.status.MetricsManager;
import org.visallo.core.status.StatusRepository;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class GraphPropertyRunnerTest {
    private static final String VERTEX_ID = "vertexID";
    private static final String EDGE_ID = "edgeID";

    private static final String PROP_NAME = "propName";
    private static final String PROP_KEY = "propKey";
    private static final String PROP_VALUE = "propValue";

    private GraphPropertyRunner testSubject;
    private Graph graph;

    @Mock
    private WorkQueueRepository workQueueRepository;

    @Mock
    private StatusRepository statusRepository;

    @Mock
    private Configuration configuration;

    @Mock
    private AuthorizationRepository authorizationRepository;

    @Before
    public void before() {
        testSubject = new GraphPropertyRunner(
                workQueueRepository,
                statusRepository,
                configuration,
                new JmxMetricsManager(),
                authorizationRepository
        );
        graph = mock(Graph.class);
        testSubject.setGraph(graph);
    }

    @Test
    public void testHandlePropertyOnVertexIsHandledByGPWS() throws Exception {
        TestCountingGPWStub countingGPWStub = new TestCountingGPWStub();

        GraphPropertyMessage message = createVertexPropertyGPWMessage(VERTEX_ID, PROP_NAME + "0", PROP_KEY + "0");
        inflateVertexAndAddToGraph(VERTEX_ID, 1L);
        runTests(countingGPWStub, message);

        assertThat(countingGPWStub.isExecutingCount.get(), is(1L));
        assertThat(countingGPWStub.isHandledCount.get(), is(1L));
    }

    @Test
    public void testAllPropertiesOnVertexAreProcessedByGraphPropertyWorkers() throws Exception {
        TestCountingGPWStub countingGPWStub = new TestCountingGPWStub();

        GraphPropertyMessage message = createVertexIdJSONGPWMessage(VERTEX_ID);
        inflateVertexAndAddToGraph(VERTEX_ID, 11L);
        runTests(countingGPWStub, message);

        assertThat(countingGPWStub.isExecutingCount.get(), is(12L));
        assertThat(countingGPWStub.isHandledCount.get(), is(12L));
    }

    @Test
    public void testHandlePropertyOnEdgeIsHandledByGPWS() throws Exception {
        TestCountingGPWStub countingGPWStub = new TestCountingGPWStub();

        GraphPropertyMessage message = createEdgeIdJSONGPWMessage(EDGE_ID, PROP_NAME + "0", PROP_KEY + "0");
        inflateEdgeAndAddToGraph(EDGE_ID, 1L);
        runTests(countingGPWStub, message);

        assertThat(countingGPWStub.isExecutingCount.get(), is(2L));
        assertThat(countingGPWStub.isHandledCount.get(), is(2L));
    }

    @Test
    public void testAllPropertiesOnEdgeAreProcessedByGraphPropertyWorkers() throws Exception {
        TestCountingGPWStub countingGPWStub = new TestCountingGPWStub();

        GraphPropertyMessage message = createEdgeIdJSONGPWMessage(EDGE_ID);
        inflateEdgeAndAddToGraph(EDGE_ID, 14L);
        runTests(countingGPWStub, message);

        assertThat(countingGPWStub.isExecutingCount.get(), is(15L));
        assertThat(countingGPWStub.isHandledCount.get(), is(15L));
    }

    @Test
    public void testMultipleEdgesAreProcessedInMultiEdgeMessage() throws Exception {
        int numMessages = 5;
        int numProperties = 11;
        String[] ids = new String[numMessages];

        for (int i = 0; i < numMessages; i++) {
            ids[i] = EDGE_ID + "_" + i;
            inflateEdgeAndAddToGraph(ids[i], numProperties);
        }

        GraphPropertyMessage message = createMultiEdgeIdJSONGPWMessage(ids);

        testMultiElementMessage(numMessages, numProperties, message);
    }

    @Test
    public void testMultipleVerticesAreProcessedInMultiVertexMessage() throws Exception {
        int numMessages = 5;
        int numProperties = 11;
        String[] ids = new String[numMessages];

        for (int i = 0; i < numMessages; i++) {
            ids[i] = VERTEX_ID + "_" + i;
            inflateVertexAndAddToGraph(ids[i], numProperties);
        }

        testMultiElementMessage(numMessages, numProperties, createMultiVertexIdJSONGPWMessage(ids));
    }

    @Test
    public void testMultipleElementsOnSinglePropertyRunsOnPropertyOnAllElements() throws Exception {
        int numElements = 5;
        Property prop = createProperty(PROP_NAME, PROP_KEY, PROP_VALUE);
        String[] ids = new String[numElements];

        for (int i = 0; i < numElements; i++) {
            ids[i] = VERTEX_ID + "_" + i;
            inflateVertexAndAddToGraph(ids[i], prop);
        }

        GraphPropertyMessage message = createMultiVertexPropertyMessage(ids, prop);
        TestCountingGPWStub countingGPWStub = new TestCountingGPWStub();
        runTests(countingGPWStub, message);

        long expectedNumProperties = (long) (numElements);

        assertThat(countingGPWStub.isExecutingCount.get(), is(expectedNumProperties));
        assertThat(countingGPWStub.isHandledCount.get(), is(expectedNumProperties));
        assertThat(countingGPWStub.workedOnProperties.size(), is(1));
        Property next = countingGPWStub.workedOnProperties.iterator().next();
        assertThat(next.getName(), is(prop.getName()));
        assertThat(next.getName(), is(prop.getName()));
        assertThat(next.getKey(), is(prop.getKey()));
        assertThat(next.getValue(), is(prop.getValue()));
    }

    private void testMultiElementMessage(int numMessages, int numProperties, GraphPropertyMessage message) throws Exception {
        TestCountingGPWStub countingGPWStub = new TestCountingGPWStub();
        runTests(countingGPWStub, message);

        long expectedNumProperties = (long) (numMessages * numProperties + numMessages);

        assertThat(countingGPWStub.isExecutingCount.get(), is(expectedNumProperties));
        assertThat(countingGPWStub.isHandledCount.get(), is(expectedNumProperties));
    }

    private void runTests(GraphPropertyWorker worker, GraphPropertyMessage message) throws Exception {
        GraphPropertyThreadedWrapper graphPropertyThreadedWrapper = startInThread(worker);

        testSubject.addGraphPropertyThreadedWrappers(graphPropertyThreadedWrapper);

        GraphPropertyWorkerItem workerItem = testSubject.tupleDataToWorkerItem(message.toBytes());
        testSubject.process(workerItem);

        stopInThread(graphPropertyThreadedWrapper);
    }

    private void inflateVertexAndAddToGraph(String vertexId, long numProperties) {
        inflateVertexAndAddToGraph(vertexId, createNumProperties(numProperties));
    }

    private void inflateVertexAndAddToGraph(String vertexId, Property... properties) {
        Vertex mockedVertex = createMockedVertex(vertexId, properties);
        registerVertexWithGraph(vertexId, mockedVertex);
    }

    private void inflateEdgeAndAddToGraph(String edgeId, long numProperties) {
        Property[] props = createNumProperties(numProperties);
        Edge mockedEdge = createMockedEdge(edgeId, props);
        registerEdgeWithGraph(edgeId, mockedEdge);
    }

    private GraphPropertyThreadedWrapper createTestGPWThreadedWrapper(GraphPropertyWorker worker) {
        GraphPropertyThreadedWrapper stubGraphPropertyThreadedWrapper = new GraphPropertyThreadedWrapper(worker);
        MetricsManager manager = mock(MetricsManager.class);
        when(manager.counter(anyString())).thenReturn(mock(Counter.class));
        when(manager.timer(anyString())).thenReturn(mock(com.codahale.metrics.Timer.class));

        stubGraphPropertyThreadedWrapper.setMetricsManager(manager);
        return stubGraphPropertyThreadedWrapper;
    }

    private GraphPropertyThreadedWrapper startInThread(GraphPropertyWorker worker) throws InterruptedException {
        GraphPropertyThreadedWrapper testGPWThreadedWrapper = createTestGPWThreadedWrapper(worker);
        Thread thread = new Thread(testGPWThreadedWrapper);
        thread.start();
        return testGPWThreadedWrapper;
    }

    private void stopInThread(GraphPropertyThreadedWrapper... wrappers) throws InterruptedException {
        for (GraphPropertyThreadedWrapper wrapper : wrappers) {
            sleep();
            wrapper.stop();
            sleep();
        }
    }

    private void sleep() throws InterruptedException {
        Thread.sleep(50L);
    }

    private class TestCountingGPWStub extends GraphPropertyWorker {
        public AtomicLong isHandledCount = new AtomicLong(0);
        public AtomicLong isExecutingCount = new AtomicLong(0);
        public Set<Property> workedOnProperties = Sets.newHashSet();

        @Override
        public void execute(InputStream in, GraphPropertyWorkData data) throws Exception {
            isExecutingCount.incrementAndGet();
        }

        @Override
        public boolean isHandled(Element element, Property property) {
            isHandledCount.incrementAndGet();
            workedOnProperties.add(property);
            return true;
        }
    }

    private Edge createMockedEdge(String edgeId, Property... props) {
        List<Property> propList = Lists.newArrayList(props);
        Edge e = mock(Edge.class);
        when(e.getId()).thenReturn(edgeId);
        when(e.getProperties()).thenReturn(propList);
        return e;
    }

    private Property[] createNumProperties(long num) {
        List<Property> props = Lists.newArrayList();

        for (long i = 0; i < num; i++) {
            props.add(createProperty(PROP_NAME + i, PROP_KEY + i, PROP_VALUE + i));
        }

        return props.toArray(new Property[0]);
    }

    private Property createProperty(String name, String key, String value) {
        Property prop = mock(Property.class);
        when(prop.getName()).thenReturn(name);
        when(prop.getKey()).thenReturn(key);
        when(prop.getValue()).thenReturn(value);
        when(prop.getVisibility()).thenReturn(Visibility.EMPTY);
        return prop;
    }

    private Vertex createMockedVertex(String id, Property... properties) {
        List<Property> propList = Lists.newArrayList(properties);
        Vertex v = mock(Vertex.class);
        when(v.getId()).thenReturn(id);
        when(v.getProperties()).thenReturn(propList);
        for (Property property : properties) {
            String key = property.getKey();
            String name = property.getName();
            when(v.getProperty(key, name)).thenReturn(property);
            when(v.getProperty(name)).thenReturn(property);
            when(v.getProperties(name)).thenReturn(Collections.singletonList(property));
            when(v.getProperties(key, name)).thenReturn(Collections.singletonList(property));
        }

        return v;
    }

    private void registerVertexWithGraph(String id, Vertex v) {
        when(graph.getVertex(eq(id), any(Authorizations.class))).thenReturn(v);
    }

    private void registerEdgeWithGraph(String edgeId, Edge e) {
        when(graph.getEdge(eq(edgeId), any(Authorizations.class))).thenReturn(e);
    }

    private static GraphPropertyMessage createMultiEdgeIdJSONGPWMessage(String... edgeIds) {
        return createTestJSONGPWMessage().setGraphEdgeId(edgeIds);
    }

    private static GraphPropertyMessage createMultiVertexPropertyMessage(String[] vertexIds, Property property) {
        return createTestJSONGPWMessage()
                .setGraphVertexId(vertexIds)
                .setPropertyKey(property.getKey())
                .setPropertyName(property.getName());
    }

    private static GraphPropertyMessage createMultiVertexIdJSONGPWMessage(String... vertexIds) {
        return createTestJSONGPWMessage()
                .setGraphVertexId(vertexIds);
    }

    private static GraphPropertyMessage createVertexPropertyGPWMessage(String vertexId, String propertyName, String propertyKey) {
        return createVertexIdJSONGPWMessage(vertexId)
                .setPropertyKey(propertyKey)
                .setPropertyName(propertyName);
    }

    private static GraphPropertyMessage createEdgeIdJSONGPWMessage(String edgeId, String propertyName, String propertyKey) {
        return createTestJSONGPWMessage()
                .setGraphEdgeId(new String[]{edgeId});
    }

    private static GraphPropertyMessage createVertexIdJSONGPWMessage(String vertexId) {
        return createTestJSONGPWMessage()
                .setGraphVertexId(new String[]{vertexId});
    }

    private static GraphPropertyMessage createEdgeIdJSONGPWMessage(String edgeId) {
        return createTestJSONGPWMessage()
                .setGraphEdgeId(new String[]{edgeId});
    }

    private static GraphPropertyMessage createTestJSONGPWMessage() {
        GraphPropertyMessage message = new GraphPropertyMessage();
        message.setPriority(Priority.LOW);
        message.setVisibilitySource("");
        message.setWorkspaceId("wsId");
        return message;
    }
}
