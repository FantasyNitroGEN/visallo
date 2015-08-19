package org.visallo.core.ingest.graphProperty;

import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.workQueue.Priority;

public class GraphPropertyMessage {
    public static final String PROPERTY_KEY = "propertyKey";
    public static final String PROPERTY_NAME = "propertyName";
    public static final String GRAPH_VERTEX_ID = "graphVertexId";
    public static final String GRAPH_EDGE_ID = "graphEdgeId";
    public static final String WORKSPACE_ID = "workspaceId";
    public static final String VISIBILITY_SOURCE = "visibilitySource";
    public static final String PRIORITY = "priority";

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

    public String getVisibilitySource(){
        return _obj.optString(VISIBILITY_SOURCE, null);
    }

    public Priority getPriority(){
        String priorityString = _obj.optString(PRIORITY, null);
        return Priority.safeParse(priorityString);
    }

    public String getPropertyKey(){
        return _obj.optString(PROPERTY_KEY, "");
    }

    public String getPropertyName() {
        return _obj.optString(PROPERTY_NAME, "");
    }

    public String getVertexId() {
        return _obj.optString(GRAPH_VERTEX_ID);
    }

    public String getEdgeId(){
        return _obj.optString(GRAPH_EDGE_ID);
    }

    public boolean canHandleVertex(){
        return canHandleElementById(getVertexId());
    }

    public boolean canHandleEdge(){
        return canHandleElementById(getEdgeId());
    }

    public boolean canHandleByProperty(){
        return _obj.has(PROPERTY_KEY) || this._obj.has(PROPERTY_NAME);
    }

    public ProcessingType findProcessingType(){
        if(canHandleByProperty()){
            return ProcessingType.PROPERTY;
        }
        else if(canHandleVertex() || canHandleEdge()) {
            return ProcessingType.ELEMENT;
        }

        throw new VisalloException(String.format("Unable to determine processing type from invalid message %s", _obj.toString()));
    }

    private static boolean canHandleElementById(String id){
        return StringUtils.isNotEmpty(id);
    }
}
