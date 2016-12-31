package org.visallo.web.structuredingest.core.model;

import org.json.JSONObject;
import org.vertexium.Authorizations;
import org.visallo.core.util.ClientApiConverter;

public class StructuredIngestQueueItem {
    private String mapping;
    private String workspaceId;
    private String userId;
    private String[] authorizations;
    private String vertexId;
    private ParseOptions parseOptions;
    private String type;
    private boolean publish;

    public StructuredIngestQueueItem() {

    }

    public StructuredIngestQueueItem(String workspaceId, String mapping, String userId, String vertexId, String type, ParseOptions options, boolean publish, Authorizations authorizations) {
        this.workspaceId = workspaceId;
        this.mapping = mapping;
        this.userId = userId;
        this.vertexId = vertexId;
        this.type = type;
        this.authorizations = authorizations.getAuthorizations();
        this.parseOptions = options;
        this.publish = publish;
    }

    public String getType() {
        return type;
    }

    public String getUserId() {
        return userId;
    }

    public String getVertexId() {
        return vertexId;
    }

    public String getMapping() {
        return mapping;
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public boolean isPublish() {
        return publish;
    }

    public ParseOptions getParseOptions() {
        return parseOptions;
    }

    public String[] getAuthorizations() {
        return authorizations;
    }

    public JSONObject toJson() {
        return new JSONObject(ClientApiConverter.clientApiToString(this));
    }
}
