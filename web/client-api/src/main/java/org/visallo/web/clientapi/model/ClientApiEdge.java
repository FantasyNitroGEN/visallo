package org.visallo.web.clientapi.model;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("edge")
public class ClientApiEdge extends ClientApiElement {
    private String label;
    private String outVertexId;
    private String inVertexId;

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getOutVertexId() {
        return outVertexId;
    }

    public void setOutVertexId(String outVertexId) {
        this.outVertexId = outVertexId;
    }

    public String getInVertexId() {
        return inVertexId;
    }

    public void setInVertexId(String inVertexId) {
        this.inVertexId = inVertexId;
    }
}
