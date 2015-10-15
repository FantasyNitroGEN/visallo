package org.visallo.web.clientapi.model;

import com.fasterxml.jackson.annotation.JsonTypeName;

import java.util.ArrayList;
import java.util.List;

@JsonTypeName("vertex")
public class ClientApiVertex extends ClientApiElement {
    private List<String> edgeLabels = new ArrayList<String>();

    public List<String> getEdgeLabels() {
        return edgeLabels;
    }

    public void setEdgeLabels(List<String> edgeLabels) {
        this.edgeLabels = edgeLabels;
    }
}
