package org.visallo.web.routes.workspace;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.Vertex;
import org.vertexium.util.LookAheadIterable;
import org.visallo.core.model.workspace.Workspace;
import org.visallo.core.model.workspace.WorkspaceEntity;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.user.User;
import org.visallo.core.util.ClientApiConverter;
import org.visallo.web.VisalloResponse;
import org.visallo.web.clientapi.model.ClientApiWorkspaceVertices;
import org.visallo.web.parameterProviders.ActiveWorkspaceId;

import java.util.Iterator;
import java.util.List;

public class WorkspaceVertices implements ParameterizedHandler {
    private final Graph graph;
    private final WorkspaceRepository workspaceRepository;

    @Inject
    public WorkspaceVertices(
            final Graph graph,
            final WorkspaceRepository workspaceRepository) {
        this.graph = graph;
        this.workspaceRepository = workspaceRepository;
    }

    @Handle
    public void handle(
            User user,
            Authorizations authorizations,
            @ActiveWorkspaceId String workspaceId,
            VisalloResponse response
    ) throws Exception {
        Workspace workspace = workspaceRepository.findById(workspaceId, user);
        final List<WorkspaceEntity> workspaceEntities = workspaceRepository.findEntities(workspace, user);
        Iterable<String> vertexIds = getVisibleWorkspaceEntityIds(workspaceEntities);
        Iterable<Vertex> graphVertices = graph.getVertices(vertexIds, ClientApiConverter.SEARCH_FETCH_HINTS, authorizations);
        ClientApiWorkspaceVertices results = new ClientApiWorkspaceVertices();
        results.getVertices().addAll(ClientApiConverter.toClientApiVertices(graphVertices, workspaceId, authorizations));
        response.respondWithClientApiObject(results);
    }

    private LookAheadIterable<WorkspaceEntity, String> getVisibleWorkspaceEntityIds(final List<WorkspaceEntity> workspaceEntities) {
        return new LookAheadIterable<WorkspaceEntity, String>() {
            @Override
            protected boolean isIncluded(WorkspaceEntity workspaceEntity, String entityVertexId) {
                return workspaceEntity.isVisible();
            }

            @Override
            protected String convert(WorkspaceEntity workspaceEntity) {
                return workspaceEntity.getEntityVertexId();
            }

            @Override
            protected Iterator<WorkspaceEntity> createIterator() {
                return workspaceEntities.iterator();
            }
        };
    }
}
