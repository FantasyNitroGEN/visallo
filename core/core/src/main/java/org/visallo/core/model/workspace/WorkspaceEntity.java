package org.visallo.core.model.workspace;

import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.Vertex;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.visallo.core.util.StreamUtil.stream;

public class WorkspaceEntity implements Serializable {
    static long serialVersionUID = 1L;
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(WorkspaceEntity.class);
    private final String entityVertexId;
    private transient Vertex vertex;

    public WorkspaceEntity(
            String entityVertexId,
            Vertex vertex
    ) {
        this.entityVertexId = entityVertexId;
        this.vertex = vertex;
    }

    public String getEntityVertexId() {
        return entityVertexId;
    }

    public Vertex getVertex() {
        return vertex;
    }

    public static Iterable<Vertex> toVertices(final Iterable<WorkspaceEntity> workspaceEntities, final Graph graph, final Authorizations authorizations) {
        List<String> vertexIdsToFetch = stream(workspaceEntities)
                .filter(we -> we.getVertex() == null)
                .map(we -> we.getEntityVertexId())
                .collect(Collectors.toList());
        Map<String, Vertex> fetchedVerticesMap = stream(graph.getVertices(vertexIdsToFetch, authorizations))
                .distinct()
                .collect(Collectors.toMap(v -> v.getId(), v -> v));
        return stream(workspaceEntities)
                .map(workspaceEntity -> {
                    if (workspaceEntity.getVertex() == null) {
                        workspaceEntity.vertex = fetchedVerticesMap.get(workspaceEntity.getEntityVertexId());
                        if (workspaceEntity.vertex == null) {
                            LOGGER.error("Could not find vertex for WorkspaceEntity: %s", workspaceEntity);
                            return null;
                        }
                    }
                    return workspaceEntity.getVertex();
                })
                .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return "WorkspaceEntity{" +
                "entityVertexId='" + entityVertexId + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        WorkspaceEntity that = (WorkspaceEntity) o;

        return entityVertexId.equals(that.entityVertexId);

    }

    @Override
    public int hashCode() {
        return entityVertexId.hashCode();
    }
}
