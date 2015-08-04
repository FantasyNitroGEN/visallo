package org.visallo.web.clientapi.model;

public class ClientApiWorkspaceEdge implements ClientApiObject {
    public String edgeId;
    public String label;
    public String outVertexId;
    public String inVertexId;

    public ClientApiWorkspaceEdge() {

    }

    public ClientApiWorkspaceEdge(String edgeId, String label, String outVertexId, String inVertexId) {
        this.edgeId = edgeId;
        this.label = label;
        this.outVertexId = outVertexId;
        this.inVertexId = inVertexId;
    }
}
