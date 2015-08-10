package org.visallo.core.ingest.graphProperty;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.util.JSONUtil;

import java.util.ArrayList;
import java.util.List;

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

    public List<String> getVertexIds() {
        return getListOfItemsFromJSONKey(_obj, GRAPH_VERTEX_ID);
    }

    public List<String> getEdgeIds(){
        return getListOfItemsFromJSONKey(_obj, GRAPH_EDGE_ID);
    }

    public boolean canHandleVertex(){
        return canHandleElementById(getVertexIds());
    }

    public boolean canHandleEdge(){
        return canHandleElementById(getEdgeIds());
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

    private static boolean canHandleElementById(List<String> id){
        return id != null && !id.isEmpty();
    }

    private static List<String> getListOfItemsFromJSONKey(JSONObject obj, String key){
        Object edges = obj.opt(key);

        if(edges == null){
            return Lists.newArrayList();
        }
        if(edges instanceof JSONArray){
            return JSONUtil.toStringList((JSONArray) edges);
        }
        else if(edges instanceof String){
            return Lists.newArrayList((String)edges);
        }
        else{
            throw new VisalloException("unknown format to parse messages");
        }
    }
}
