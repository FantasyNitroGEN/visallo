package org.visallo.web.clientapi.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName(DirectoryEntity.TYPE_PERSON)
public class DirectoryPerson extends DirectoryEntity {
    private final String email;

    @JsonCreator
    public DirectoryPerson(
            @JsonProperty("id") String id,
            @JsonProperty("displayName") String displayName
    ) {
        this(id, displayName, null);
    }

    @JsonCreator
    public DirectoryPerson(
            @JsonProperty("id") String id,
            @JsonProperty("displayName") String displayName,
            @JsonProperty("email") String email
    ) {
        super(id, displayName);
        this.email = email;
    }

    @Override
    public String getType() {
        return TYPE_PERSON;
    }

    public String getEmail() {
        return email;
    }
}
