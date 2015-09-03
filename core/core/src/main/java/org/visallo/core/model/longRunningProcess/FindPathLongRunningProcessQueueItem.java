package org.visallo.core.model.longRunningProcess;

import org.json.JSONObject;
import org.vertexium.Authorizations;
import org.visallo.core.util.ClientApiConverter;

public class FindPathLongRunningProcessQueueItem {
    private String sourceVertexId;
    private String destVertexId;
    private String[] labels;
    private int hops;
    private String workspaceId;
    private String[] authorizations;

    public FindPathLongRunningProcessQueueItem() {

    }

    public FindPathLongRunningProcessQueueItem(String sourceVertexId, String destVertexId, String[] labels, int hops, String workspaceId, Authorizations authorizations) {
        this.sourceVertexId = sourceVertexId;
        this.destVertexId = destVertexId;
        this.labels = labels;
        this.hops = hops;
        this.workspaceId = workspaceId;
        this.authorizations = authorizations.getAuthorizations();
    }

    public String getSourceVertexId() {
        return sourceVertexId;
    }

    public String getDestVertexId() {
        return destVertexId;
    }

    public String[] getLabels() {
        return labels;
    }

    public int getHops() {
        return hops;
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public String[] getAuthorizations() {
        return authorizations;
    }

    public String getType() {
        return "findPath";
    }

    public JSONObject toJson() {
        return new JSONObject(ClientApiConverter.clientApiToString(this));
    }
}
