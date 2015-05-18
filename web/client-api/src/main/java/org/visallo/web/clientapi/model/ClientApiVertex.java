package org.visallo.web.clientapi.model;

import com.fasterxml.jackson.annotation.JsonTypeName;

import java.util.ArrayList;
import java.util.List;

@JsonTypeName("vertex")
public class ClientApiVertex extends ClientApiElement {
    private Double score;
    private Integer commonCount;
    private List<String> edgeLabels = new ArrayList<>();

    public List<String> getEdgeLabels() {
        return edgeLabels;
    }

    /**
     * search score
     */
    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public double getScore(double defaultValue) {
        if (this.score == null) {
            return defaultValue;
        }
        return this.score;
    }

    public Integer getCommonCount() {
        return commonCount;
    }

    public void setCommonCount(Integer commonCount) {
        this.commonCount = commonCount;
    }

    public void setEdgeLabels(List<String> edgeLabels) {
        this.edgeLabels = edgeLabels;
    }
}
