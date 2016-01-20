package org.visallo.web.clientapi.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName(DirectoryEntity.TYPE_GROUP)
public class DirectoryGroup extends DirectoryEntity {
    @JsonCreator
    public DirectoryGroup(
            @JsonProperty("id") String id,
            @JsonProperty("displayName") String displayName
    ) {
        super(id, displayName);
    }

    @Override
    public String getType() {
        return TYPE_GROUP;
    }
}
