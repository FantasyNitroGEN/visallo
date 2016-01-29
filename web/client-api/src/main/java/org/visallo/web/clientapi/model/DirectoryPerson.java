package org.visallo.web.clientapi.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName(DirectoryEntity.TYPE_PERSON)
public class DirectoryPerson extends DirectoryEntity {
    @JsonCreator
    public DirectoryPerson(
            @JsonProperty("id") String id,
            @JsonProperty("displayName") String displayName
    ) {
        super(id, displayName);
    }

    @Override
    public String getType() {
        return TYPE_PERSON;
    }
}
