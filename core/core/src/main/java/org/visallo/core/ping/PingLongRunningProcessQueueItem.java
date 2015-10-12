package org.visallo.core.ping;

import org.json.JSONObject;
import org.vertexium.Vertex;
import org.visallo.core.util.ClientApiConverter;
import org.visallo.web.clientapi.model.ClientApiObject;

public class PingLongRunningProcessQueueItem implements ClientApiObject {
    public static final String TYPE = "ping";

    private String type;
    private String id;
    private String vertexId;

    public PingLongRunningProcessQueueItem() {

    }

    public PingLongRunningProcessQueueItem(Vertex vertex) {
        type = TYPE;
        id = vertex.getId();
        vertexId = vertex.getId();
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getVertexId() {
        return vertexId;
    }

    public void setVertexId(String vertexId) {
        this.vertexId = vertexId;
    }

    public JSONObject toJson() {
        return new JSONObject(ClientApiConverter.clientApiToString(this));
    }

    public static boolean isHandled(JSONObject jsonObject) {
        return jsonObject.getString("type").equals(TYPE);
    }
}
