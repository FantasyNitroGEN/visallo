package org.visallo.web.clientapi.model;

public class ClientApiEdgeInfo {
    private final String edgeId;
    private final String label;
    private final String vertexId;

    public ClientApiEdgeInfo(String edgeId, String label, String vertexId) {
        this.edgeId = edgeId;
        this.label = label;
        this.vertexId = vertexId;
    }

    public String getEdgeId() {
        return edgeId;
    }

    public String getLabel() {
        return label;
    }

    public String getVertexId() {
        return vertexId;
    }
}
