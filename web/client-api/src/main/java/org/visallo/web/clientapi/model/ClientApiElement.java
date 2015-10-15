package org.visallo.web.clientapi.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.visallo.web.clientapi.util.ClientApiConverter;

import java.util.ArrayList;
import java.util.List;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ClientApiEdge.class, name = "edge"),
        @JsonSubTypes.Type(value = ClientApiVertex.class, name = "vertex")
})
public abstract class ClientApiElement implements ClientApiObject {
    private String id;
    private List<ClientApiProperty> properties = new ArrayList<ClientApiProperty>();
    private SandboxStatus sandboxStatus;
    private String visibilitySource;
    private Double score;
    private Integer commonCount;

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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public SandboxStatus getSandboxStatus() {
        return sandboxStatus;
    }

    public void setSandboxStatus(SandboxStatus sandboxStatus) {
        this.sandboxStatus = sandboxStatus;
    }

    public List<ClientApiProperty> getProperties() {
        return properties;
    }

    public String getVisibilitySource() {
        return visibilitySource;
    }

    public void setVisibilitySource(String visibilitySource) {
        this.visibilitySource = visibilitySource;
    }

    @Override
    public String toString() {
        return ClientApiConverter.clientApiToString(this);
    }
}
