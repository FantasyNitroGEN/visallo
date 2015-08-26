package org.visallo.core.model.workspace;

import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.Vertex;
import org.vertexium.util.ConvertingIterable;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

public class WorkspaceEntity {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(WorkspaceEntity.class);
    private final String entityVertexId;
    private final boolean visible;
    private final Integer graphPositionX;
    private final Integer graphPositionY;
    private final String graphLayoutJson;
    private Vertex vertex;

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

    public static Iterable<Vertex> toVertices(final Iterable<WorkspaceEntity> workspaceEntities, final Graph graph, final Authorizations authorizations) {
        return new ConvertingIterable<WorkspaceEntity, Vertex>(workspaceEntities) {
            @Override
            protected Vertex convert(WorkspaceEntity workspaceEntity) {
                if (workspaceEntity.getVertex() == null) {
                    workspaceEntity.vertex = graph.getVertex(workspaceEntity.getEntityVertexId(), authorizations);
                    if (workspaceEntity.vertex == null) {
                        LOGGER.error("Could not find vertex for WorkspaceEntity: %s", workspaceEntity);
                        return null;
                    }
                }
                return workspaceEntity.getVertex();
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
