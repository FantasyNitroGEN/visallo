package org.visallo.web.clientapi.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.visallo.web.clientapi.util.ClientApiConverter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ClientApiEdge.class, name = "edge"),
        @JsonSubTypes.Type(value = ClientApiVertex.class, name = "vertex")
})
public abstract class ClientApiElement extends ClientApiVertexiumObject {
    private String id;
    private Set<String> extendedDataTableNames = new HashSet<String>();
    private SandboxStatus sandboxStatus;
    private String visibilitySource;
    private Integer commonCount;
    private Boolean deleteable;
    private Boolean updateable;
    private ClientApiElementAcl acl;

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

    public Set<String> getExtendedDataTableNames() {
        return extendedDataTableNames;
    }

    public String getVisibilitySource() {
        return visibilitySource;
    }

    public void setVisibilitySource(String visibilitySource) {
        this.visibilitySource = visibilitySource;
    }

    public Boolean getDeleteable() {
        return deleteable;
    }

    public void setDeleteable(Boolean deleteable) {
        this.deleteable = deleteable;
    }

    public Boolean getUpdateable() {
        return updateable;
    }

    public void setUpdateable(Boolean updateable) {
        this.updateable = updateable;
    }

    public ClientApiElementAcl getAcl() {
        return acl;
    }

    public void setAcl(ClientApiElementAcl acl) {
        this.acl = acl;
    }
}
