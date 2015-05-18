package org.visallo.web.routes.edge;

import com.google.inject.Inject;
import org.visallo.core.config.Configuration;
import org.visallo.core.model.audit.AuditAction;
import org.visallo.core.model.audit.AuditRepository;
import org.visallo.core.model.ontology.OntologyProperty;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.security.VisalloVisibility;
import org.visallo.core.user.User;
import org.visallo.web.BaseRequestHandler;
import org.vertexium.*;
import com.v5analytics.webster.HandlerChain;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class DeleteEdgeProperty extends BaseRequestHandler {
    private final Graph graph;
    private final OntologyRepository ontologyRepository;
    private final AuditRepository auditRepository;
    private final WorkQueueRepository workQueueRepository;

    @Inject
    public DeleteEdgeProperty(
            final OntologyRepository ontologyRepository,
            final Graph graph,
            final AuditRepository auditRepository,
            final UserRepository userRepository,
            final Configuration configuration,
            final WorkspaceRepository workspaceRepository,
            final WorkQueueRepository workQueueRepository) {
        super(userRepository, workspaceRepository, configuration);
        this.ontologyRepository = ontologyRepository;
        this.graph = graph;
        this.auditRepository = auditRepository;
        this.workQueueRepository = workQueueRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        VisalloVisibility visalloVisibility = new VisalloVisibility();
        final String propertyName = getRequiredParameter(request, "propertyName");
        final String propertyKey = getRequiredParameter(request, "propertyKey");
        final String edgeId = getRequiredParameter(request, "edgeId");

        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);
        String workspaceId = getActiveWorkspaceId(request);

        OntologyProperty property = ontologyRepository.getPropertyByIRI(propertyName);
        if (property == null) {
            throw new RuntimeException("Could not find property: " + propertyName);
        }

        // TODO remove all properties from all edges? I don't think so
        Edge edge = graph.getEdge(edgeId, authorizations);
        Object oldValue = null;
        Property oldProperty = edge.getProperty(propertyKey, propertyName);
        if (oldProperty != null) {
            oldValue = oldProperty.getValue();
        }
        // TODO: replace "" when we implement commenting on ui
        auditRepository.auditRelationshipProperty(AuditAction.DELETE, edge.getVertexId(Direction.OUT), edge.getVertexId(Direction.IN), propertyKey, property.getTitle(),
                oldValue, null, edge, "", "", user, visalloVisibility.getVisibility());
        edge.softDeleteProperty(propertyKey, propertyName, authorizations);
        graph.flush();

        workQueueRepository.pushGraphPropertyQueue(edge, (String) null, propertyName, workspaceId, null, Priority.HIGH);

        respondWithSuccessJson(response);
    }
}
