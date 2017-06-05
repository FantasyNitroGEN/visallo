package org.visallo.web.clientapi.model;

public class ClientApiVertexPublishItem extends ClientApiPublishItem {
    private String vertexId;

    public String getVertexId() {
        return vertexId;
    }

    public void setVertexId(String vertexId) {
        this.vertexId = vertexId;
    }

    @Override
    public String getType() {
        return "vertex";
    }

    @Override
    public boolean validate() {
        if (vertexId == null) {
            setErrorMessage("Vertex ID must be provided for publishing");
            return false;
        }
        return true;
    }
}
