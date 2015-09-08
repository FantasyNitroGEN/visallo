package org.visallo.core.ingest.graphProperty;

import com.codahale.metrics.Counter;
import com.google.common.collect.Lists;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.vertexium.*;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.status.MetricsManager;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class GraphPropertyRunnerTest {
    private static final Object MESSAGE_ID = "messageId";
    private static final String VERTEX_ID = "vertexID";
    private static final String EDGE_ID = "edgeID";

    private static final String PROP_NAME = "propName";
    private static final String PROP_KEY = "propKey";
    private static final String PROP_VALUE = "propValue";

    private GraphPropertyRunner _testSubject;
    private Graph _graph;

    @Before
    public void before(){
        _testSubject = new GraphPropertyRunner();
        _graph = mock(Graph.class);
        _testSubject.setGraph(_graph);
        _testSubject.setWorkQueueRepository(mock(WorkQueueRepository.class));
    }

    @Test(expected = VisalloException.class)
    public void testUnknownProcessingMessageThrowsException() throws Exception {
        JSONObject obj = createTestJSONGPWMessage();
        _testSubject.process(MESSAGE_ID, obj);
    }

    @Test
    public void testHandlePropertyOnVertexIsHandledByGPWS() throws Exception {
        CountingGPWStub countingGPWStub = new CountingGPWStub();
        JSONObject message = createVertexPropertyGPWMessage(VERTEX_ID, PROP_NAME + "0", PROP_KEY + "0");
        inflateVertexAndAddToGraph(VERTEX_ID, 1L);
        runTests(countingGPWStub, message);

        assertThat(countingGPWStub.isExecutingCount.get(), is(1L));
        assertThat(countingGPWStub.isHandledCount.get(), is(1L));
    }

    @Test
    public void testAllPropertiesOnVertexAreProcessedByGraphPropertyWorkers() throws Exception {
        CountingGPWStub countingGPWStub = new CountingGPWStub();

        JSONObject message = createVertexIdJSONGPWMessage(VERTEX_ID);
        inflateVertexAndAddToGraph(VERTEX_ID, 11L);
        runTests(countingGPWStub, message);

        assertThat(countingGPWStub.isExecutingCount.get(), is(11L));
        assertThat(countingGPWStub.isHandledCount.get(), is(11L));
    }

    @Test
    public void testHandlePropertyOnEdgeIsHandledByGPWS() throws Exception {
        CountingGPWStub countingGPWStub = new CountingGPWStub();

        JSONObject message = createEdgeIdJSONGPWMessage(EDGE_ID, PROP_NAME + "0", PROP_KEY + "0");
        inflateEdgeAndAddToGraph(EDGE_ID, 1L);
        runTests(countingGPWStub, message);

        assertThat(countingGPWStub.isExecutingCount.get(), is(1L));
        assertThat(countingGPWStub.isHandledCount.get(), is(1L));
    }

    @Test
    public void testAllPropertiesOnEdgeAreProcessedByGraphPropertyWorkers() throws Exception {
        CountingGPWStub countingGPWStub = new CountingGPWStub();

        JSONObject message = createEdgeIdJSONGPWMessage(EDGE_ID);
        inflateEdgeAndAddToGraph(EDGE_ID, 14L);
        runTests(countingGPWStub, message);

        assertThat(countingGPWStub.isExecutingCount.get(), is(14L));
        assertThat(countingGPWStub.isHandledCount.get(), is(14L));
    }

    private void runTests(GraphPropertyWorker worker, JSONObject message) throws Exception {
        GraphPropertyThreadedWrapper graphPropertyThreadedWrapper = startInThread(worker);

        _testSubject.addGraphPropertyThreadedWrappers(graphPropertyThreadedWrapper);

        _testSubject.process(MESSAGE_ID, message);

        stopInThread(graphPropertyThreadedWrapper);
    }

    private void inflateVertexAndAddToGraph(String vertexId, long numProperties){
        Property[] props = createNumProperties(numProperties);
        Vertex mockedVertex = createMockedVertex(vertexId, props);
        registerVertexWithGraph(vertexId, mockedVertex);
    }

    private void inflateEdgeAndAddToGraph(String edgeId, long numProperties){
        Property[] props = createNumProperties(numProperties);
        Edge mockedEdge = createMockedEdge(edgeId, props);
        registerEdgeWithGraph(edgeId, mockedEdge);
    }

    private GraphPropertyThreadedWrapper createTestGPWThreadedWrapper(GraphPropertyWorker worker){
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

    private class CountingGPWStub extends GraphPropertyWorker{
        public AtomicLong isHandledCount = new AtomicLong(0);
        public AtomicLong isExecutingCount = new AtomicLong(0);

        @Override
        public void execute(InputStream in, GraphPropertyWorkData data) throws Exception {
            isExecutingCount.incrementAndGet();
        }

        @Override
        public boolean isHandled(Element element, Property property) {
            isHandledCount.incrementAndGet();
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

    private Property[] createNumProperties(long num){
        List<Property> props = Lists.newArrayList();

        for(long i = 0; i < num; i++){
            Property prop = mock(Property.class);
            when(prop.getName()).thenReturn(PROP_NAME + i);
            when(prop.getKey()).thenReturn(PROP_KEY + i);
            when(prop.getValue()).thenReturn(PROP_VALUE + i);
            props.add(prop);
        }

        return props.toArray(new Property[0]);
    }

    private Vertex createMockedVertex(String id, Property... properties){
        List<Property> propList = Lists.newArrayList(properties);
        Vertex v = mock(Vertex.class);
        when(v.getId()).thenReturn(id);
        when(v.getProperties()).thenReturn(propList);
        for(Property property : properties) {
            when(v.getProperty(property.getKey(), property.getName())).thenReturn(property);
            when(v.getProperty(property.getName())).thenReturn(property);
        }

        return v;
    }

    private void registerVertexWithGraph(String id, Vertex v){
        when(_graph.getVertex(eq(id), any(Authorizations.class))).thenReturn(v);
    }

    private void registerEdgeWithGraph(String edgeId, Edge e) {
        when(_graph.getEdge(eq(edgeId), any(Authorizations.class))).thenReturn(e);
    }

    private static JSONObject createVertexPropertyGPWMessage(String vertexId, String propertyName, String propertyKey){
        return createVertexIdJSONGPWMessage(vertexId).put(GraphPropertyMessage.PROPERTY_KEY, propertyKey).put(GraphPropertyMessage.PROPERTY_NAME, propertyName);
    }


    private static JSONObject createEdgeIdJSONGPWMessage(String edgeId, String propertyName, String propertyKey){
        return createTestJSONGPWMessage().put(GraphPropertyMessage.GRAPH_EDGE_ID, edgeId);
    }

    private static JSONObject createVertexIdJSONGPWMessage(String vertexId){
        return createTestJSONGPWMessage().put(GraphPropertyMessage.GRAPH_VERTEX_ID, vertexId);
    }

    private static JSONObject createEdgeIdJSONGPWMessage(String edgeId){
        return createTestJSONGPWMessage().put(GraphPropertyMessage.GRAPH_EDGE_ID, edgeId);
    }

    private static JSONObject createTestJSONGPWMessage(){
        return new JSONObject().put(GraphPropertyMessage.PRIORITY, "1").put(GraphPropertyMessage.VISIBILITY_SOURCE, "").put(GraphPropertyMessage.WORKSPACE_ID, "wsId");
    }
}
