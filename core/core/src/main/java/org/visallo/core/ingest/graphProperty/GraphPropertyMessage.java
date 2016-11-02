package org.visallo.core.ingest.graphProperty;

import com.google.common.collect.Lists;
import org.json.JSONArray;
import org.json.JSONObject;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.util.JSONUtil;

import java.util.List;

public class GraphPropertyMessage {
    public static final String PROPERTY_KEY = "propertyKey";
    public static final String PROPERTY_NAME = "propertyName";
    public static final String GRAPH_VERTEX_ID = "graphVertexId";
    public static final String GRAPH_EDGE_ID = "graphEdgeId";
    public static final String WORKSPACE_ID = "workspaceId";
    public static final String VISIBILITY_SOURCE = "visibilitySource";
    public static final String PRIORITY = "priority";
    public static final String STATUS = "status";
    public static final String BEFORE_ACTION_TIMESTAMP = "beforeActionTimestamp";
    public static final String PROPERTIES = "properties";

    private JSONObject _obj;

    public boolean isValid() {
        return canHandleVertex() || canHandleEdge();
    }

    public enum ProcessingType {
        PROPERTY,
        ELEMENT
    }

    public GraphPropertyMessage(JSONObject obj) {
        _obj = obj;
    }

    public String getWorkspaceId() {
        return _obj.optString(WORKSPACE_ID, null);
    }

    public String getVisibilitySource() {
        return _obj.optString(VISIBILITY_SOURCE, null);
    }

    public Priority getPriority() {
        String priorityString = _obj.optString(PRIORITY, null);
        return Priority.safeParse(priorityString);
    }

    public String getPropertyKey() {
        return _obj.optString(PROPERTY_KEY, "");
    }

    public String getPropertyName() {
        return _obj.optString(PROPERTY_NAME, "");
    }

    public List<String> getVertexIds() {
        return getListOfItemsFromJSONKey(_obj, GRAPH_VERTEX_ID);
    }

    public List<String> getEdgeIds() {
        return getListOfItemsFromJSONKey(_obj, GRAPH_EDGE_ID);
    }

    public ElementOrPropertyStatus getStatus() {
        String status = _obj.optString(STATUS, null);
        return ElementOrPropertyStatus.safeParse(status);
    }

    public long getBeforeActionTimestamp() {
        return _obj.optLong(BEFORE_ACTION_TIMESTAMP, -1L);
    }

    public JSONArray getProperties() {
        return _obj.optJSONArray(PROPERTIES);
    }

    public boolean canHandleVertex() {
        return canHandleElementById(getVertexIds());
    }

    public boolean canHandleEdge() {
        return canHandleElementById(getEdgeIds());
    }

    public boolean canHandleByProperty() {
        return _obj.has(PROPERTY_KEY) || this._obj.has(PROPERTY_NAME);
    }

    public boolean canHandleByProperties() {
        return _obj.has(PROPERTIES);
    }

    private static boolean canHandleElementById(List<String> id) {
        return id != null && !id.isEmpty();
    }

    private static List<String> getListOfItemsFromJSONKey(JSONObject obj, String key) {
        Object edges = obj.opt(key);

        if (edges == null) {
            return Lists.newArrayList();
        }

        if (edges instanceof JSONArray) {
            return JSONUtil.toStringList((JSONArray) edges);
        }

        if (edges instanceof String) {
            return Lists.newArrayList((String) edges);
        }

        throw new VisalloException("unknown format to parse messages");
    }
}
