package org.visallo.web.clientapi.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.visallo.web.clientapi.util.ClientApiConverter;

import java.util.HashSet;
import java.util.Set;

public class VisibilityJson {
    private String source = "";
    private Set<String> workspaces = new HashSet<String>();

    public VisibilityJson() {

    }

    public VisibilityJson(String source) {
        if (source == null) {
            source = "";
        }
        this.source = source;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public Set<String> getWorkspaces() {
        return workspaces;
    }

    public void addWorkspace(String workspaceId) {
        workspaces.add(workspaceId);
    }

    @Override
    public String toString() {
        return ClientApiConverter.clientApiToString(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        VisibilityJson that = (VisibilityJson) o;
        if (source != null ? !source.equals(that.source) : that.source != null) return false;
        if (workspaces != null ? !workspaces.equals(that.workspaces) : that.workspaces != null) return false;
        return true;
    }

    @Override
    public int hashCode() {
        return source.hashCode();
    }

    public static VisibilityJson removeFromWorkspace(VisibilityJson json, String workspaceId) {
        if (json == null) {
            json = new VisibilityJson();
        }

        json.getWorkspaces().remove(workspaceId);
        return json;
    }

    public static VisibilityJson removeFromAllWorkspace(VisibilityJson json) {
        if (json == null) {
            json = new VisibilityJson();
        }

        json.getWorkspaces().clear();
        return json;
    }

    public static VisibilityJson updateVisibilitySourceAndAddWorkspaceId(VisibilityJson visibilityJson, String visibilitySource, String workspaceId) {
        if (visibilityJson == null) {
            visibilityJson = new VisibilityJson();
        }

        visibilityJson.setSource(visibilitySource);

        if (workspaceId != null) {
            visibilityJson.addWorkspace(workspaceId);
        }

        return visibilityJson;
    }

    public static VisibilityJson updateVisibilitySource(VisibilityJson visibilityJson, String visibilitySource) {
        if (visibilityJson == null) {
            visibilityJson = new VisibilityJson();
        }

        visibilityJson.setSource(visibilitySource);
        return visibilityJson;
    }
}
