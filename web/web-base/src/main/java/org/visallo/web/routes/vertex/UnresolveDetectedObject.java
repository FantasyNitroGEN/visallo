package org.visallo.web.routes.vertex;

import com.google.inject.Inject;
import com.v5analytics.webster.HandlerChain;
import org.visallo.core.config.Configuration;
import org.visallo.core.ingest.ArtifactDetectedObject;
import org.visallo.core.model.audit.AuditAction;
import org.visallo.core.model.audit.AuditRepository;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.security.VisalloVisibility;
import org.visallo.core.security.VisibilityTranslator;
import org.visallo.core.user.User;
import org.visallo.core.util.ClientApiConverter;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.core.util.SandboxStatusUtil;
import org.visallo.web.BaseRequestHandler;
import org.visallo.web.clientapi.model.ClientApiElement;
import org.visallo.web.clientapi.model.SandboxStatus;
import org.visallo.web.clientapi.model.VisibilityJson;
import org.vertexium.Authorizations;
import org.vertexium.Edge;
import org.vertexium.Graph;
import org.vertexium.Vertex;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class UnresolveDetectedObject extends BaseRequestHandler {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(UnresolveDetectedObject.class);
    private final Graph graph;
    private final VisibilityTranslator visibilityTranslator;
    private final AuditRepository auditRepository;
    private final WorkQueueRepository workQueueRepository;

    @Inject
    public UnresolveDetectedObject(
            final Graph graph,
            final UserRepository userRepository,
            final VisibilityTranslator visibilityTranslator,
            final Configuration configuration,
            final WorkspaceRepository workspaceRepository,
            AuditRepository auditRepository,
            WorkQueueRepository workQueueRepository) {
        super(userRepository, workspaceRepository, configuration);
        this.graph = graph;
        this.visibilityTranslator = visibilityTranslator;
        this.auditRepository = auditRepository;
        this.workQueueRepository = workQueueRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final String vertexId = getRequiredParameter(request, "vertexId");
        final String multiValueKey = getRequiredParameter(request, "multiValueKey");
        String workspaceId = getActiveWorkspaceId(request);
        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);

        Vertex artifactVertex = graph.getVertex(vertexId, authorizations);
        ArtifactDetectedObject artifactDetectedObject = VisalloProperties.DETECTED_OBJECT.getPropertyValue(artifactVertex, multiValueKey);
        Edge edge = graph.getEdge(artifactDetectedObject.getEdgeId(), authorizations);
        Vertex resolvedVertex = edge.getOtherVertex(artifactVertex.getId(), authorizations);

        SandboxStatus vertexSandboxStatus = SandboxStatusUtil.getSandboxStatus(resolvedVertex, workspaceId);
        SandboxStatus edgeSandboxStatus = SandboxStatusUtil.getSandboxStatus(edge, workspaceId);
        if (vertexSandboxStatus == SandboxStatus.PUBLIC && edgeSandboxStatus == SandboxStatus.PUBLIC) {
            LOGGER.warn("Can not unresolve a public entity");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            chain.next(request, response);
            return;
        }

        VisibilityJson visibilityJson;
        if (vertexSandboxStatus == SandboxStatus.PUBLIC) {
            visibilityJson = VisalloProperties.VISIBILITY_JSON.getPropertyValue(edge);
            visibilityJson = VisibilityJson.removeFromWorkspace(visibilityJson, workspaceId);
        } else {
            visibilityJson = VisalloProperties.VISIBILITY_JSON.getPropertyValue(resolvedVertex);
            visibilityJson = VisibilityJson.removeFromWorkspace(visibilityJson, workspaceId);
        }
        VisalloVisibility visalloVisibility = visibilityTranslator.toVisibility(visibilityJson);

        // remove edge
        graph.softDeleteEdge(edge, authorizations);
        auditRepository.auditRelationship(AuditAction.DELETE, artifactVertex, resolvedVertex, edge, "", "", user, visalloVisibility.getVisibility());

        // remove property
        VisalloProperties.DETECTED_OBJECT.removeProperty(artifactVertex, multiValueKey, authorizations);

        graph.flush();

        this.workQueueRepository.pushEdgeDeletion(edge);
        this.workQueueRepository.pushGraphPropertyQueue(
                artifactVertex,
                multiValueKey,
                VisalloProperties.DETECTED_OBJECT.getPropertyName(),
                workspaceId,
                visibilityJson.getSource(),
                Priority.HIGH
        );

        auditRepository.auditVertex(AuditAction.UNRESOLVE, resolvedVertex.getId(), "", "", user, visalloVisibility.getVisibility());

        ClientApiElement result = ClientApiConverter.toClientApi(artifactVertex, workspaceId, authorizations);
        respondWithClientApiObject(response, result);
    }
}
