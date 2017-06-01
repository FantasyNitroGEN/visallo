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
        @JsonSubTypes.Type(value = ClientApiVertex.class, name = "vertex"),
        @JsonSubTypes.Type(value = ClientApiExtendedDataRow.class, name = "extendedDataRow")
})
public class ClientApiVertexiumObject implements ClientApiObject {
    private Double score;
    private List<ClientApiProperty> properties = new ArrayList<ClientApiProperty>();

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

    public List<ClientApiProperty> getProperties() {
        return properties;
    }

    public ClientApiProperty getProperty(String propertyKey, String propertyName) {
        for (ClientApiProperty property : getProperties()) {
            if (property.getKey().equals(propertyKey)
                    && property.getName().equals(propertyName)) {
                return property;
            }
        }
        return null;
    }

    public Iterable<ClientApiProperty> getProperties(String name) {
        List<ClientApiProperty> results = new ArrayList<ClientApiProperty>();
        for (ClientApiProperty property : getProperties()) {
            if (property.getName().equals(name)) {
                results.add(property);
            }
        }
        return results;
    }

    @Override
    public String toString() {
        return ClientApiConverter.clientApiToString(this);
    }
}
