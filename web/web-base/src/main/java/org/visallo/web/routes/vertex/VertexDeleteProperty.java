package org.visallo.web.routes.vertex;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Required;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.Vertex;
import org.visallo.core.exception.VisalloAccessDeniedException;
import org.visallo.core.model.ontology.OntologyProperty;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.workspace.WorkspaceHelper;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.security.ACLProvider;
import org.visallo.core.user.User;
import org.visallo.web.VisalloResponse;
import org.visallo.web.clientapi.model.ClientApiSuccess;
import org.visallo.web.parameterProviders.ActiveWorkspaceId;

public class VertexDeleteProperty implements ParameterizedHandler {
    private final Graph graph;
    private final WorkspaceHelper workspaceHelper;
    private final OntologyRepository ontologyRepository;
    private final WorkspaceRepository workspaceRepository;
    private final ACLProvider aclProvider;

    @Inject
    public VertexDeleteProperty(
            final Graph graph,
            final WorkspaceHelper workspaceHelper,
            final OntologyRepository ontologyRepository,
            final WorkspaceRepository workspaceRepository,
            final ACLProvider aclProvider
    ) {
        this.graph = graph;
        this.workspaceHelper = workspaceHelper;
        this.ontologyRepository = ontologyRepository;
        this.workspaceRepository = workspaceRepository;
        this.aclProvider = aclProvider;
    }

    @Handle
    public ClientApiSuccess handle(
            @Required(name = "graphVertexId") String graphVertexId,
            @Required(name = "propertyKey") String propertyKey,
            @Required(name = "propertyName") String propertyName,
            @ActiveWorkspaceId String workspaceId,
            User user,
            Authorizations authorizations
    ) throws Exception {
        OntologyProperty ontologyProperty = ontologyRepository.getPropertyByIRI(propertyName);

        Vertex graphVertex = graph.getVertex(graphVertexId, authorizations);
        if (!aclProvider.canDeleteProperty(graphVertex, propertyKey, propertyName, user)) {
            throw new VisalloAccessDeniedException(propertyName + " is not deleteable", user, graphVertexId);
        }

        workspaceHelper.deleteProperties(graphVertex, propertyKey, propertyName, ontologyProperty, workspaceId, authorizations, user);

        return VisalloResponse.SUCCESS;
    }
}
