package org.visallo.web.clientapi.model;

import com.fasterxml.jackson.annotation.JsonTypeName;

import java.util.ArrayList;
import java.util.List;

@JsonTypeName("vertex")
public class ClientApiVertex extends ClientApiElement {
    private String conceptType;
    private List<String> edgeLabels = new ArrayList<String>();

    public String getConceptType() {
        return conceptType;
    }

    public void setConceptType(String conceptType) {
        this.conceptType = conceptType;
    }

    public List<String> getEdgeLabels() {
        return edgeLabels;
    }

    public void setEdgeLabels(List<String> edgeLabels) {
        this.edgeLabels = edgeLabels;
    }
}
