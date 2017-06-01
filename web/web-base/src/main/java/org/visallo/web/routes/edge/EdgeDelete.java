package org.visallo.web.routes.edge;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Required;
import org.vertexium.*;
import org.visallo.core.exception.VisalloAccessDeniedException;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.model.workspace.WorkspaceHelper;
import org.visallo.core.security.ACLProvider;
import org.visallo.core.user.User;
import org.visallo.core.util.SandboxStatusUtil;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.VisalloResponse;
import org.visallo.web.clientapi.model.ClientApiSuccess;
import org.visallo.web.clientapi.model.SandboxStatus;
import org.visallo.web.parameterProviders.ActiveWorkspaceId;

public class EdgeDelete implements ParameterizedHandler {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(EdgeDelete.class);
    private final Graph graph;
    private final WorkspaceHelper workspaceHelper;
    private String entityHasImageIri;
    private final OntologyRepository ontologyRepository;
    private final ACLProvider aclProvider;
    private final WorkQueueRepository workQueueRepository;

    @Inject
    public EdgeDelete(
            final Graph graph,
            final WorkspaceHelper workspaceHelper,
            final OntologyRepository ontologyRepository,
            final ACLProvider aclProvider,
            final WorkQueueRepository workQueueRepository
            ) {
        this.graph = graph;
        this.workspaceHelper = workspaceHelper;
        this.ontologyRepository = ontologyRepository;
        this.aclProvider = aclProvider;
        this.workQueueRepository = workQueueRepository;

        this.entityHasImageIri = ontologyRepository.getRelationshipIRIByIntent("entityHasImage");
        if (this.entityHasImageIri == null) {
            LOGGER.warn("'entityHasImage' intent has not been defined. Please update your ontology.");
        }
    }

    @Handle
    public ClientApiSuccess handle(
            @Required(name = "edgeId") String edgeId,
            @ActiveWorkspaceId String workspaceId,
            User user,
            Authorizations authorizations
    ) throws Exception {
        if (this.entityHasImageIri == null) {
            this.entityHasImageIri = ontologyRepository.getRequiredRelationshipIRIByIntent("entityHasImage");
        }

        Edge edge = graph.getEdge(edgeId, authorizations);
        if (!aclProvider.canDeleteElement(edge, user, workspaceId)) {
            throw new VisalloAccessDeniedException("Edge " + edgeId + " is not deleteable", user, edge.getId());
        }

        Vertex outVertex = edge.getVertex(Direction.OUT, authorizations);
        Vertex inVertex = edge.getVertex(Direction.IN, authorizations);

        SandboxStatus sandboxStatus = SandboxStatusUtil.getSandboxStatus(edge, workspaceId);

        boolean isPublicEdge = sandboxStatus == SandboxStatus.PUBLIC;

        workspaceHelper.deleteEdge(workspaceId, edge, outVertex, inVertex, isPublicEdge, Priority.HIGH, authorizations, user);

        return VisalloResponse.SUCCESS;
    }
}
