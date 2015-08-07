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

    private GraphPropertyRunner _testSubject;
    private Graph _graph;
    private WorkQueueRepository _workQueueRepository;

    @Before
    public void before(){
        _testSubject = new GraphPropertyRunner();
        _graph = mock(Graph.class);
        _testSubject.setGraph(_graph);
        _workQueueRepository = mock(WorkQueueRepository.class);
        _testSubject.setWorkQueueRepository(_workQueueRepository);
    }

    @Test(expected = VisalloException.class)
    public void testUnknownProcessingMessageThrowsExceptions() throws Exception {
        JSONObject obj = createTestJSONGPWMessage();
        _testSubject.process(MESSAGE_ID, obj);
    }

    @Test
    public void testAllPropertiesGetAddedIfVertexMessageGetsReceived() throws Exception {
        Property[] props = createNumProperties(2);

        Vertex v = createMockedVertex(VERTEX_ID, props);
        registerVertexWithGraph(VERTEX_ID, v);

        JSONObject obj = createVertexIdJSONGPWMessage(VERTEX_ID);

        _testSubject.process(MESSAGE_ID, obj);

        verify(_workQueueRepository, times(props.length)).pushGraphPropertyQueue(any(Vertex.class), any(Property.class), anyString(), anyString(), any(Priority.class));
    }

    @Test
    public void testHandlePropertyIsHandledByGPWS() throws Exception {
        CountingGPWStub countingGPWStub = new CountingGPWStub();
        GraphPropertyThreadedWrapper stubGraphPropertyThreadedWrapper = new GraphPropertyThreadedWrapper(countingGPWStub);
        MetricsManager manager = mock(MetricsManager.class);
        when(manager.counter(anyString())).thenReturn(mock(Counter.class));
        when(manager.timer(anyString())).thenReturn(mock(com.codahale.metrics.Timer.class));

        stubGraphPropertyThreadedWrapper.setMetricsManager(manager);
        Thread thread = new Thread(stubGraphPropertyThreadedWrapper);
        thread.start();

        String propName = "propName";
        String propKey = "propKey";
        String propValue = "propValue";

        JSONObject obj = createVertexPropertyGPWMessage(VERTEX_ID, propName, propKey);
        Property[] props = createNumProperties(1);
        when(props[0].getName()).thenReturn(propName);
        when(props[0].getKey()).thenReturn(propKey);
        when(props[0].getValue()).thenReturn(propValue);

        Vertex mockedVertex = createMockedVertex(VERTEX_ID, props);
        registerVertexWithGraph(VERTEX_ID, mockedVertex);
        _testSubject.addGraphPropertyThreadedWrappers(Lists.newArrayList(stubGraphPropertyThreadedWrapper));

        _testSubject.process(MESSAGE_ID, obj);

        Thread.sleep(50L);
        stubGraphPropertyThreadedWrapper.stop();
        Thread.sleep(50L);
        assertThat(countingGPWStub.isExecutingCount.get(), is(1L));
        assertThat(countingGPWStub.isHandledCount.get(), is(1L));
    }

    @Test
    public void testAllPropertiesGetAddedIfEdgeMessageGetsReceived() throws Exception {
        Property[] props = createNumProperties(2);

        Edge e = createMockedEdge(EDGE_ID, props);
        registerEdgeWithGraph(EDGE_ID, e);

        JSONObject obj = createEdgeIdJSONGPWMessage(EDGE_ID);

        _testSubject.process(MESSAGE_ID, obj);

        verify(_workQueueRepository, times(props.length)).pushGraphPropertyQueue(any(Edge.class), any(Property.class), anyString(), anyString(), any(Priority.class));
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

    private Property[] createNumProperties(int num){
        List<Property> props = Lists.newArrayList();
        for(int i = 0; i < num; i++){
            props.add(mock(Property.class));
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
        return createVertexIdJSONGPWMessage(vertexId).put(GraphPropertyRunner.PROPERTY_KEY, propertyKey).put(GraphPropertyRunner.PROPERTY_NAME, propertyName);
    }

    private static JSONObject createVertexIdJSONGPWMessage(String vertexId){
        return createTestJSONGPWMessage().put(GraphPropertyRunner.GRAPH_VERTEX_ID, vertexId);
    }

    private static JSONObject createEdgeIdJSONGPWMessage(String edgeId){
        return createTestJSONGPWMessage().put(GraphPropertyRunner.GRAPH_EDGE_ID, edgeId);
    }

    private static JSONObject createTestJSONGPWMessage(){
        return new JSONObject().put(GraphPropertyRunner.PRIORITY, "1").put(GraphPropertyRunner.VISIBILITY_SOURCE, "").put(GraphPropertyRunner.WORKSPACE_ID, "wsId");
    }
}
