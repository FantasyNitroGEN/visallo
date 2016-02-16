package org.visallo.web.clientapi.model;

import com.fasterxml.jackson.databind.JsonNode;
import org.visallo.web.clientapi.util.ClientApiConverter;

import java.util.*;

public class ClientApiUser implements ClientApiObject {
    private String id;
    private String userName;
    private String displayName;
    private UserType userType;
    private String currentWorkspaceId;
    private UserStatus status;
    private String email;
    private String currentWorkspaceName;
    private String csrfToken;
    private Set<String> privileges = new HashSet<String>();
    private JsonNode uiPreferences;
    private List<String> authorizations = new ArrayList<String>();
    private List<Object> longRunningProcesses = new ArrayList<Object>();
    private List<ClientApiWorkspace> workspaces = new ArrayList<ClientApiWorkspace>();
    private Map<String, Object> properties = new HashMap<String, Object>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public UserType getUserType() {
        return userType;
    }

    public void setUserType(UserType userType) {
        this.userType = userType;
    }

    public String getCurrentWorkspaceId() {
        return currentWorkspaceId;
    }

    public void setCurrentWorkspaceId(String currentWorkspaceId) {
        this.currentWorkspaceId = currentWorkspaceId;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public void setCurrentWorkspaceName(String currentWorkspaceName) {
        this.currentWorkspaceName = currentWorkspaceName;
    }

    public String getCurrentWorkspaceName() {
        return currentWorkspaceName;
    }

    public String getCsrfToken() {
        return csrfToken;
    }

    public void setCsrfToken(String csrfToken) {
        this.csrfToken = csrfToken;
    }

    public JsonNode getUiPreferences() {
        return uiPreferences;
    }

    public void setUiPreferences(JsonNode uiPreferences) {
        this.uiPreferences = uiPreferences;
    }

    public Set<String> getPrivileges() {
        return privileges;
    }

    public List<String> getAuthorizations() {
        return authorizations;
    }

    public void addAuthorization(String auth) {
        this.authorizations.add(auth);
    }

    public List<Object> getLongRunningProcesses() {
        return longRunningProcesses;
    }

    public List<ClientApiWorkspace> getWorkspaces() {
        return workspaces;
    }

    @Override
    public String toString() {
        return ClientApiConverter.clientApiToString(this);
    }

    public Map<String, Object> getProperties() {
        return properties;
    }
}
