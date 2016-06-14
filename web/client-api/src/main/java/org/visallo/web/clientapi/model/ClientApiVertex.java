package org.visallo.web.clientapi.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeName;

import java.util.ArrayList;
import java.util.List;

@JsonTypeName("vertex")
public class ClientApiVertex extends ClientApiElement {
    private String conceptType;
    private List<String> edgeLabels = null;
    private List<ClientApiEdgeInfo> edgeInfos = null;

    public String getConceptType() {
        return conceptType;
    }

    public void setConceptType(String conceptType) {
        this.conceptType = conceptType;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public List<String> getEdgeLabels() {
        return edgeLabels;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public List<ClientApiEdgeInfo> getEdgeInfos() {
        return edgeInfos;
    }

    public void addEdgeLabel(String edgeLabel) {
        if (edgeLabels == null) {
            edgeLabels = new ArrayList<String>();
        }
        edgeLabels.add(edgeLabel);
    }

    public void addEdgeInfo(ClientApiEdgeInfo edgeInfo) {
        if (edgeInfos == null) {
            edgeInfos = new ArrayList<ClientApiEdgeInfo>();
        }
        edgeInfos.add(edgeInfo);
    }
}
