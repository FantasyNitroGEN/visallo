package org.visallo.core.ingest.graphProperty;

import org.json.JSONObject;
import org.junit.Test;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.util.JSONUtil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GraphPropertyMessageTest {
    @Test
    public void testVerticesMessage() {
        String jsonString = "{" +
                "  \"propertyKey\": \"key1\"," +
                "  \"graphVertexId\": [" +
                "    \"v1\"," +
                "    \"v2\"" +
                "  ]," +
                "  \"propertyName\": \"name1\"," +
                "  \"beforeActionTimestamp\": 123456789," +
                "  \"visibilitySource\": \"visibilitySourceValue\"," +
                "  \"priority\": \"HIGH\"," +
                "  \"workspaceId\": \"wsTest\"," +
                "  \"status\": \"UPDATE\"" +
                "}";
        GraphPropertyMessage message = GraphPropertyMessage.create(jsonString.getBytes());
        assertEquals("wsTest", message.getWorkspaceId());
        assertEquals("visibilitySourceValue", message.getVisibilitySource());
        assertEquals("key1", message.getPropertyKey());
        assertEquals("name1", message.getPropertyName());
        assertEquals(ElementOrPropertyStatus.UPDATE, message.getStatus());
        assertEquals(123456789, message.getBeforeActionTimestamp().longValue());
        assertEquals(Priority.HIGH, message.getPriority());
        assertEquals(2, message.getGraphVertexId().length);
        assertEquals("v1", message.getGraphVertexId()[0]);
        assertEquals("v2", message.getGraphVertexId()[1]);
        assertTrue(
                new JSONObject(message.toJsonString()).toString(2),
                JSONUtil.areEqual(new JSONObject(jsonString), new JSONObject(message.toJsonString()))
        );
    }

    @Test
    public void testSingleVertexMessage() {
        String jsonString = "{" +
                "  \"propertyKey\": \"key1\"," +
                "  \"graphVertexId\": \"v1\"," +
                "  \"propertyName\": \"name1\"," +
                "  \"beforeActionTimestamp\": 123456789," +
                "  \"visibilitySource\": \"visibilitySourceValue\"," +
                "  \"priority\": \"HIGH\"," +
                "  \"workspaceId\": \"wsTest\"," +
                "  \"status\": \"UPDATE\"" +
                "}";
        GraphPropertyMessage message = GraphPropertyMessage.create(jsonString.getBytes());
        assertEquals("wsTest", message.getWorkspaceId());
        assertEquals("visibilitySourceValue", message.getVisibilitySource());
        assertEquals("key1", message.getPropertyKey());
        assertEquals("name1", message.getPropertyName());
        assertEquals(ElementOrPropertyStatus.UPDATE, message.getStatus());
        assertEquals(123456789, message.getBeforeActionTimestamp().longValue());
        assertEquals(Priority.HIGH, message.getPriority());
        assertEquals(1, message.getGraphVertexId().length);
        assertEquals("v1", message.getGraphVertexId()[0]);
        assertTrue(
                new JSONObject(message.toJsonString()).toString(2),
                JSONUtil.areEqual(new JSONObject(jsonString), new JSONObject(message.toJsonString()))
        );
    }

    @Test
    public void testMultiplePropertiesMessage() {
        String jsonString = "{" +
                "  \"graphVertexId\": \"v1\"," +
                "  \"visibilitySource\": \"visibilitySourceValue\"," +
                "  \"priority\": \"HIGH\"," +
                "  \"properties\": [" +
                "    {" +
                "      \"propertyKey\": \"key1\"," +
                "      \"propertyName\": \"name1\"," +
                "      \"beforeActionTimestamp\": 123456," +
                "      \"status\": \"UPDATE\"" +
                "    }," +
                "    {" +
                "      \"propertyKey\": \"key2\"," +
                "      \"propertyName\": \"name2\"," +
                "      \"beforeActionTimestamp\": 234567," +
                "      \"status\": \"UPDATE\"" +
                "    }" +
                "  ]," +
                "  \"workspaceId\": \"wsTest\"" +
                "}";
        GraphPropertyMessage message = GraphPropertyMessage.create(jsonString.getBytes());
        assertEquals("wsTest", message.getWorkspaceId());
        assertEquals("visibilitySourceValue", message.getVisibilitySource());
        assertEquals(Priority.HIGH, message.getPriority());
        assertEquals(1, message.getGraphVertexId().length);
        assertEquals("v1", message.getGraphVertexId()[0]);

        assertEquals(2, message.getProperties().length);
        GraphPropertyMessage.Property[] properties = message.getProperties();
        GraphPropertyMessage.Property property = properties[0];
        assertEquals("key1", property.getPropertyKey());
        assertEquals("name1", property.getPropertyName());
        assertEquals(ElementOrPropertyStatus.UPDATE, property.getStatus());
        assertEquals(123456, property.getBeforeActionTimestamp().longValue());
        property = properties[1];
        assertEquals("key2", property.getPropertyKey());
        assertEquals("name2", property.getPropertyName());
        assertEquals(ElementOrPropertyStatus.UPDATE, property.getStatus());
        assertEquals(234567, property.getBeforeActionTimestamp().longValue());
        assertTrue(
                new JSONObject(message.toJsonString()).toString(2),
                JSONUtil.areEqual(new JSONObject(jsonString), new JSONObject(message.toJsonString()))
        );
    }

    @Test
    public void testEdgesMessage() {
        String jsonString = "{" +
                "  \"propertyKey\": \"key1\"," +
                "  \"graphEdgeId\": [" +
                "    \"e1\"," +
                "    \"e2\"" +
                "  ]," +
                "  \"propertyName\": \"name1\"," +
                "  \"beforeActionTimestamp\": 123456789," +
                "  \"visibilitySource\": \"visibilitySourceValue\"," +
                "  \"priority\": \"HIGH\"," +
                "  \"workspaceId\": \"wsTest\"," +
                "  \"status\": \"UPDATE\"" +
                "}";
        GraphPropertyMessage message = GraphPropertyMessage.create(jsonString.getBytes());
        assertEquals("wsTest", message.getWorkspaceId());
        assertEquals("visibilitySourceValue", message.getVisibilitySource());
        assertEquals("key1", message.getPropertyKey());
        assertEquals("name1", message.getPropertyName());
        assertEquals(ElementOrPropertyStatus.UPDATE, message.getStatus());
        assertEquals(123456789, message.getBeforeActionTimestamp().longValue());
        assertEquals(Priority.HIGH, message.getPriority());
        assertEquals(2, message.getGraphEdgeId().length);
        assertEquals("e1", message.getGraphEdgeId()[0]);
        assertEquals("e2", message.getGraphEdgeId()[1]);
        assertTrue(
                new JSONObject(message.toJsonString()).toString(2),
                JSONUtil.areEqual(new JSONObject(jsonString), new JSONObject(message.toJsonString()))
        );
    }

    @Test
    public void testSingleEdgeMessage() {
        String jsonString = "{" +
                "  \"propertyKey\": \"key1\"," +
                "  \"graphEdgeId\": \"e1\"," +
                "  \"propertyName\": \"name1\"," +
                "  \"beforeActionTimestamp\": 123456789," +
                "  \"visibilitySource\": \"visibilitySourceValue\"," +
                "  \"priority\": \"HIGH\"," +
                "  \"workspaceId\": \"wsTest\"," +
                "  \"status\": \"UPDATE\"" +
                "}";
        GraphPropertyMessage message = GraphPropertyMessage.create(jsonString.getBytes());
        assertEquals("wsTest", message.getWorkspaceId());
        assertEquals("visibilitySourceValue", message.getVisibilitySource());
        assertEquals("key1", message.getPropertyKey());
        assertEquals("name1", message.getPropertyName());
        assertEquals(ElementOrPropertyStatus.UPDATE, message.getStatus());
        assertEquals(123456789, message.getBeforeActionTimestamp().longValue());
        assertEquals(Priority.HIGH, message.getPriority());
        assertEquals(1, message.getGraphEdgeId().length);
        assertEquals("e1", message.getGraphEdgeId()[0]);
        assertTrue(
                new JSONObject(message.toJsonString()).toString(2),
                JSONUtil.areEqual(new JSONObject(jsonString), new JSONObject(message.toJsonString()))
        );
    }
}