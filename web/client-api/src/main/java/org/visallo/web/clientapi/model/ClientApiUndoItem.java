package org.visallo.web.clientapi.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.visallo.web.clientapi.util.ClientApiConverter;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ClientApiVertexUndoItem.class, name = "vertex"),
        @JsonSubTypes.Type(value = ClientApiPropertyUndoItem.class, name = "property"),
        @JsonSubTypes.Type(value = ClientApiRelationshipUndoItem.class, name = "relationship")
})
public abstract class ClientApiUndoItem implements ClientApiObject {
    private String errorMessage;

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public abstract String getType();

    @Override
    public String toString() {
        return ClientApiConverter.clientApiToString(this);
    }
}
