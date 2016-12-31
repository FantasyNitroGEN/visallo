package org.visallo.core.ingest.cloud;

import org.json.JSONObject;
import org.vertexium.Authorizations;
import org.visallo.core.util.ClientApiConverter;

public class CloudImportLongRunningProcessQueueItem {
    private String destination;
    private String configuration;
    private String workspaceId;
    private String userId;
    private String[] authorizations;


    public CloudImportLongRunningProcessQueueItem() {

    }

    public CloudImportLongRunningProcessQueueItem(String destination, String configuration, String userId, String workspaceId, Authorizations authorizations) {
        this.destination = destination;
        this.configuration = configuration;
        this.workspaceId = workspaceId;
        this.userId = userId;
        this.authorizations = authorizations.getAuthorizations();
    }

    public String getDestination() {
        return destination;
    }

    public String getConfiguration() { return configuration; }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public String[] getAuthorizations() {
        return authorizations;
    }

    public String getType() {
        return "org-visallo-ingest-cloud";
    }

    public String getUserId() {
        return userId;
    }

    public JSONObject toJson() {
        return new JSONObject(ClientApiConverter.clientApiToString(this));
    }
}
