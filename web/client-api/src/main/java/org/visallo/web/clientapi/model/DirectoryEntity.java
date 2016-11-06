package org.visallo.web.clientapi.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.visallo.web.clientapi.VisalloClientApiException;

import java.util.Map;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = DirectoryGroup.class, name = DirectoryEntity.TYPE_GROUP),
        @JsonSubTypes.Type(value = DirectoryPerson.class, name = DirectoryEntity.TYPE_PERSON)
})
public abstract class DirectoryEntity implements ClientApiObject, Comparable<DirectoryEntity> {
    public static final String TYPE_GROUP = "group";
    public static final String TYPE_PERSON = "person";
    private final String id;
    private final String displayName;

    public DirectoryEntity(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public abstract String getType();

    public static DirectoryEntity fromMap(Map map) {
        String type = (String) map.get("type");
        String id = (String) map.get("id");
        String displayName = (String) map.get("displayName");
        if (TYPE_GROUP.equalsIgnoreCase(type)) {
            return new DirectoryGroup(id, displayName);
        } else if (TYPE_PERSON.equalsIgnoreCase(type)) {
            return new DirectoryPerson(id, displayName);
        } else {
            throw new VisalloClientApiException("Unhandled type: " + type);
        }
    }

    public static boolean isEntity(Map map) {
        String id = (String) map.get("id");
        String displayName = (String) map.get("displayName");
        String type = (String) map.get("type");
        return type != null && id != null && displayName != null && isType(type);
    }

    private static boolean isType(String type) {
        return type.equalsIgnoreCase(TYPE_GROUP) || type.equalsIgnoreCase(TYPE_PERSON);
    }

    @Override
    public int compareTo(DirectoryEntity o) {
        int i = getType().compareTo(o.getType());
        if (i != 0) {
            return i;
        }
        return getId().compareTo(o.getId());
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof DirectoryEntity)) {
            return false;
        }
        DirectoryEntity other = (DirectoryEntity)o;
        return id.equals(other.id) && displayName.equals(other.displayName);
    }

    @Override
    public int hashCode() {
        return id.hashCode() + 31 * displayName.hashCode();
    }
}
