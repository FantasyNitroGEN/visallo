package org.visallo.web.routes.edge;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Required;
import org.vertexium.Authorizations;
import org.vertexium.Edge;
import org.vertexium.Graph;
import org.visallo.core.exception.VisalloAccessDeniedException;
import org.visallo.core.model.ontology.OntologyProperty;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.model.workspace.WorkspaceHelper;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.security.ACLProvider;
import org.visallo.core.user.User;
import org.visallo.web.VisalloResponse;
import org.visallo.web.clientapi.model.ClientApiSuccess;
import org.visallo.web.parameterProviders.ActiveWorkspaceId;

public class EdgeDeleteProperty implements ParameterizedHandler {
    private final Graph graph;
    private final OntologyRepository ontologyRepository;
    private final WorkQueueRepository workQueueRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceHelper workspaceHelper;
    private final ACLProvider aclProvider;

    @Inject
    public EdgeDeleteProperty(
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

        workspaceHelper.deleteProperties(edge, propertyKey, propertyName, ontologyProperty, workspaceId, authorizations, user);

        graph.flush();

        return VisalloResponse.SUCCESS;
    }
}
