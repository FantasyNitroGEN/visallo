package org.visallo.web.routes.edge;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Required;
import org.vertexium.*;
import org.visallo.core.exception.VisalloAccessDeniedException;
import org.visallo.core.exception.VisalloResourceNotFoundException;
import org.visallo.core.model.ontology.OntologyProperty;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.model.workspace.WorkspaceHelper;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.security.ACLProvider;
import org.visallo.core.user.User;
import org.visallo.core.util.SandboxStatusUtil;
import org.visallo.web.VisalloResponse;
import org.visallo.web.clientapi.model.ClientApiSuccess;
import org.visallo.web.clientapi.model.SandboxStatus;
import org.visallo.web.parameterProviders.ActiveWorkspaceId;

import java.util.ArrayList;
import java.util.List;

import static org.vertexium.util.IterableUtils.toList;

public class DeleteEdgeProperty implements ParameterizedHandler {
    private final Graph graph;
    private final OntologyRepository ontologyRepository;
    private final WorkQueueRepository workQueueRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceHelper workspaceHelper;
    private final ACLProvider aclProvider;

    @Inject
    public DeleteEdgeProperty(
            final OntologyRepository ontologyRepository,
            final Graph graph,
            final WorkQueueRepository workQueueRepository,
            final WorkspaceRepository workspaceRepository,
            final ACLProvider aclProvider,
            final WorkspaceHelper workspaceHelper
    ) {
        this.ontologyRepository = ontologyRepository;
        this.graph = graph;
        this.workspaceHelper = workspaceHelper;
        this.workQueueRepository = workQueueRepository;
        this.workspaceRepository = workspaceRepository;
        this.aclProvider = aclProvider;
    }

    @Handle
    public ClientApiSuccess handle(
            @Required(name = "edgeId") String edgeId,
            @Required(name = "propertyKey") String propertyKey,
            @Required(name = "propertyName") String propertyName,
            @ActiveWorkspaceId String workspaceId,
            User user,
            Authorizations authorizations
    ) throws Exception {
        OntologyProperty ontologyProperty = ontologyRepository.getPropertyByIRI(propertyName);
        if (ontologyProperty == null) {
            throw new RuntimeException("Could not find property: " + propertyName);
        }

        // TODO remove all properties from all edges? I don't think so
        Edge edge = graph.getEdge(edgeId, authorizations);

        if (!aclProvider.canDeleteProperty(edge, propertyKey, propertyName, user)) {
            throw new VisalloAccessDeniedException(propertyName + " is not deleteable", user, edge.getId());
        }

        List<Property> properties = new ArrayList<>();
        properties.addAll(toList(edge.getProperties(propertyKey, propertyName)));

        if (properties.size() == 0) {
            throw new VisalloResourceNotFoundException(String.format("Could not find property %s:%s on %s", propertyName, propertyKey, edge));
        }

        // add the vertex to the workspace so that the changes show up in the diff panel
        workspaceRepository.updateEntityOnWorkspace(workspaceId, edge.getVertexId(Direction.IN), null, null, user);
        workspaceRepository.updateEntityOnWorkspace(workspaceId, edge.getVertexId(Direction.OUT), null, null, user);

        SandboxStatus[] sandboxStatuses = SandboxStatusUtil.getPropertySandboxStatuses(properties, workspaceId);

        for (int i = 0; i < sandboxStatuses.length; i++) {
            boolean propertyIsPublic = (sandboxStatuses[i] == SandboxStatus.PUBLIC);
            Property property = properties.get(i);
            workspaceHelper.deleteProperty(edge, property, propertyIsPublic, workspaceId, Priority.HIGH, authorizations);
        }
        graph.flush();

        return VisalloResponse.SUCCESS;
    }
}
