package org.visallo.vertexium.model.workspace;

import org.vertexium.Authorizations;
import org.vertexium.FetchHint;
import org.vertexium.Graph;
import org.vertexium.Vertex;
import org.visallo.core.model.workspace.Workspace;
import org.visallo.core.model.workspace.WorkspaceProperties;

public class VertexiumWorkspace implements Workspace {
    private static final long serialVersionUID = -1692706831716776578L;
    private String displayTitle;
    private String workspaceId;
    private transient Vertex workspaceVertex;

    public VertexiumWorkspace(Vertex workspaceVertex) {
        this.displayTitle = WorkspaceProperties.TITLE.getPropertyValue(workspaceVertex);
        this.workspaceId = workspaceVertex.getId();
        this.workspaceVertex = workspaceVertex;
    }

    @Override
    public String getWorkspaceId() {
        return workspaceId;
    }

    @Override
    public String getDisplayTitle() {
        return displayTitle;
    }

    public Vertex getVertex(Graph graph, boolean includeHidden, Authorizations authorizations) {
        if (this.workspaceVertex == null) {
            this.workspaceVertex = graph.getVertex(getWorkspaceId(), includeHidden ? FetchHint.ALL_INCLUDING_HIDDEN : FetchHint.ALL, authorizations);
        }
        return this.workspaceVertex;
    }

    @Override
    public String toString() {
        return "VertexiumWorkspace{" +
                "workspaceId='" + workspaceId + '\'' +
                ", displayTitle='" + displayTitle + '\'' +
                '}';
    }
}