package org.visallo.core.model.workspace;

import org.vertexium.Vertex;
import org.vertexium.util.ConvertingIterable;

public class WorkspaceEntity {
    private final String entityVertexId;
    private final boolean visible;
    private final Integer graphPositionX;
    private final Integer graphPositionY;
    private final String graphLayoutJson;
    private final Vertex vertex;

    public WorkspaceEntity(
            String entityVertexId,
            boolean visible,
            Integer graphPositionX,
            Integer graphPositionY,
            String graphLayoutJson,
            Vertex vertex
    ) {
        this.entityVertexId = entityVertexId;
        this.visible = visible;
        this.graphPositionX = graphPositionX;
        this.graphPositionY = graphPositionY;
        this.graphLayoutJson = graphLayoutJson;
        this.vertex = vertex;
    }

    public String getEntityVertexId() {
        return entityVertexId;
    }

    public Integer getGraphPositionX() {
        return graphPositionX;
    }

    public Integer getGraphPositionY() {
        return graphPositionY;
    }

    public String getGraphLayoutJson() {
        return graphLayoutJson;
    }

    public boolean isVisible() {
        return visible;
    }

    public Vertex getVertex() {
        return vertex;
    }

    public static Iterable<Vertex> toVertices(Iterable<WorkspaceEntity> workspaceEntities) {
        return new ConvertingIterable<WorkspaceEntity, Vertex>(workspaceEntities) {
            @Override
            protected Vertex convert(WorkspaceEntity o) {
                return o.getVertex();
            }
        };
    }

    @Override
    public String toString() {
        return "WorkspaceEntity{" +
                "entityVertexId='" + entityVertexId + '\'' +
                '}';
    }
}
