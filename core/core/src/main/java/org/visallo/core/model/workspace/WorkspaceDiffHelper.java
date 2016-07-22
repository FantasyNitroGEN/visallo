package org.visallo.core.model.workspace;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import org.vertexium.*;
import org.visallo.core.formula.FormulaEvaluator;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.user.AuthorizationRepository;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.trace.Traced;
import org.visallo.core.user.User;
import org.visallo.core.util.JSONUtil;
import org.visallo.core.util.JsonSerializer;
import org.visallo.core.util.SandboxStatusUtil;
import org.visallo.web.clientapi.model.ClientApiWorkspaceDiff;
import org.visallo.web.clientapi.model.SandboxStatus;

import java.util.ArrayList;
import java.util.List;

import static org.vertexium.util.IterableUtils.toList;

public class WorkspaceDiffHelper {
    private final Graph graph;
    private final UserRepository userRepository;
    private final AuthorizationRepository authorizationRepository;
    private final FormulaEvaluator formulaEvaluator;

    @Inject
    public WorkspaceDiffHelper(
            Graph graph,
            UserRepository userRepository,
            AuthorizationRepository authorizationRepository,
            FormulaEvaluator formulaEvaluator
    ) {
        this.graph = graph;
        this.userRepository = userRepository;
        this.authorizationRepository = authorizationRepository;
        this.formulaEvaluator = formulaEvaluator;
    }

    @Traced
    public ClientApiWorkspaceDiff diff(
            Workspace workspace,
            Iterable<WorkspaceEntity> workspaceEntities,
            Iterable<Edge> workspaceEdges,
            FormulaEvaluator.UserContext userContext,
            User user
    ) {
        Authorizations authorizations = authorizationRepository.getGraphAuthorizations(
                user,
                WorkspaceRepository.VISIBILITY_STRING,
                workspace.getWorkspaceId()
        );

        ClientApiWorkspaceDiff result = new ClientApiWorkspaceDiff();
        for (WorkspaceEntity workspaceEntity : workspaceEntities) {
            List<ClientApiWorkspaceDiff.Item> entityDiffs = diffWorkspaceEntity(
                    workspace,
                    workspaceEntity,
                    userContext,
                    authorizations
            );
            if (entityDiffs != null) {
                result.addAll(entityDiffs);
            }
        }

        for (Edge workspaceEdge : workspaceEdges) {
            List<ClientApiWorkspaceDiff.Item> entityDiffs = diffEdge(workspace, workspaceEdge, authorizations);
            if (entityDiffs != null) {
                result.addAll(entityDiffs);
            }
        }

        return result;
    }

    @Traced
    protected List<ClientApiWorkspaceDiff.Item> diffEdge(
            Workspace workspace,
            Edge edge,
            Authorizations hiddenAuthorizations
    ) {
        List<ClientApiWorkspaceDiff.Item> result = new ArrayList<>();

        SandboxStatus sandboxStatus = SandboxStatusUtil.getSandboxStatus(edge, workspace.getWorkspaceId());
        boolean isPrivateChange = sandboxStatus != SandboxStatus.PUBLIC;
        boolean isPublicDelete = WorkspaceDiffHelper.isPublicDelete(edge, hiddenAuthorizations);
        if (isPrivateChange || isPublicDelete) {
            result.add(createWorkspaceDiffEdgeItem(edge, sandboxStatus, isPublicDelete));
        }

        // don't report properties individually when deleting the edge
        if (!isPublicDelete) {
            diffProperties(workspace, edge, result, hiddenAuthorizations);
        }

        return result;
    }

    public static boolean isPublicDelete(Edge edge, Authorizations authorizations) {
        return edge.isHidden(authorizations);
    }

    public static boolean isPublicDelete(Vertex vertex, Authorizations authorizations) {
        return vertex.isHidden(authorizations);
    }

    public static boolean isPublicDelete(Property property, Authorizations authorizations) {
        return property.isHidden(authorizations);
    }

    private ClientApiWorkspaceDiff.EdgeItem createWorkspaceDiffEdgeItem(
            Edge edge,
            SandboxStatus sandboxStatus,
            boolean deleted
    ) {
        Property visibilityJsonProperty = VisalloProperties.VISIBILITY_JSON.getProperty(edge);
        JsonNode visibilityJson = visibilityJsonProperty == null ? null : JSONUtil.toJsonNode(JsonSerializer.toJsonProperty(
                visibilityJsonProperty));
        return new ClientApiWorkspaceDiff.EdgeItem(
                edge.getId(),
                edge.getLabel(),
                edge.getVertexId(Direction.OUT),
                edge.getVertexId(Direction.IN),
                visibilityJson,
                sandboxStatus,
                deleted
        );
    }

    @Traced
    public List<ClientApiWorkspaceDiff.Item> diffWorkspaceEntity(
            Workspace workspace,
            WorkspaceEntity workspaceEntity,
            FormulaEvaluator.UserContext userContext,
            Authorizations authorizations
    ) {
        List<ClientApiWorkspaceDiff.Item> result = new ArrayList<>();

        Vertex entityVertex = this.graph.getVertex(
                workspaceEntity.getEntityVertexId(),
                FetchHint.ALL_INCLUDING_HIDDEN,
                authorizations
        );

        // vertex can be null if the user doesn't have access to the entity
        if (entityVertex == null) {
            return null;
        }

        SandboxStatus sandboxStatus = SandboxStatusUtil.getSandboxStatus(entityVertex, workspace.getWorkspaceId());
        boolean isPrivateChange = sandboxStatus != SandboxStatus.PUBLIC;
        boolean isPublicDelete = WorkspaceDiffHelper.isPublicDelete(entityVertex, authorizations);
        if (isPrivateChange || isPublicDelete) {
            result.add(createWorkspaceDiffVertexItem(
                    entityVertex,
                    sandboxStatus,
                    userContext,
                    workspaceEntity.isVisible(),
                    isPublicDelete
            ));
        }

        // don't report properties individually when deleting the vertex
        if (!isPublicDelete) {
            diffProperties(workspace, entityVertex, result, authorizations);
        }

        return result;
    }

    private ClientApiWorkspaceDiff.VertexItem createWorkspaceDiffVertexItem(
            Vertex vertex,
            SandboxStatus sandboxStatus,
            FormulaEvaluator.UserContext userContext,
            boolean visible,
            boolean deleted
    ) {
        String vertexId = vertex.getId();
        String title = formulaEvaluator.evaluateTitleFormula(vertex, userContext, null);
        String conceptType = VisalloProperties.CONCEPT_TYPE.getPropertyValue(vertex);
        Property visibilityJsonProperty = VisalloProperties.VISIBILITY_JSON.getProperty(vertex);
        JsonNode visibilityJson = visibilityJsonProperty == null ? null : JSONUtil.toJsonNode(JsonSerializer.toJsonProperty(
                visibilityJsonProperty));
        return new ClientApiWorkspaceDiff.VertexItem(
                vertexId,
                title,
                conceptType,
                visibilityJson,
                sandboxStatus,
                deleted,
                visible
        );
    }

    @Traced
    protected void diffProperties(
            Workspace workspace,
            Element element,
            List<ClientApiWorkspaceDiff.Item> result,
            Authorizations hiddenAuthorizations
    ) {
        List<Property> properties = toList(element.getProperties());
        SandboxStatus[] propertyStatuses = SandboxStatusUtil.getPropertySandboxStatuses(
                properties,
                workspace.getWorkspaceId()
        );
        for (int i = 0; i < properties.size(); i++) {
            Property property = properties.get(i);
            boolean isPrivateChange = propertyStatuses[i] != SandboxStatus.PUBLIC;
            boolean isPublicDelete = WorkspaceDiffHelper.isPublicDelete(property, hiddenAuthorizations);
            if (isPrivateChange || isPublicDelete) {
                Property existingProperty = null;
                if (isPublicDelete && isPublicPropertyEdited(properties, propertyStatuses, property)) {
                    continue;
                } else if (isPrivateChange) {
                    existingProperty = findExistingProperty(properties, propertyStatuses, property);
                }
                result.add(createWorkspaceDiffPropertyItem(
                        element,
                        property,
                        existingProperty,
                        propertyStatuses[i],
                        isPublicDelete
                ));
            }
        }
    }

    private ClientApiWorkspaceDiff.PropertyItem createWorkspaceDiffPropertyItem(
            Element element,
            Property workspaceProperty,
            Property existingProperty,
            SandboxStatus sandboxStatus,
            boolean deleted
    ) {
        JsonNode oldData = null;
        if (existingProperty != null) {
            oldData = JSONUtil.toJsonNode(JsonSerializer.toJsonProperty(existingProperty));
        }
        JsonNode newData = JSONUtil.toJsonNode(JsonSerializer.toJsonProperty(workspaceProperty));
        return new ClientApiWorkspaceDiff.PropertyItem(
                ElementType.getTypeFromElement(element).name().toLowerCase(),
                element.getId(),
                VisalloProperties.CONCEPT_TYPE.getPropertyValue(element),
                workspaceProperty.getName(),
                workspaceProperty.getKey(),
                oldData,
                newData,
                sandboxStatus,
                deleted,
                workspaceProperty.getVisibility().getVisibilityString()
        );
    }

    private Property findExistingProperty(
            List<Property> properties,
            SandboxStatus[] propertyStatuses,
            Property workspaceProperty
    ) {
        for (int i = 0; i < properties.size(); i++) {
            Property property = properties.get(i);
            if (property.getName().equals(workspaceProperty.getName())
                    && property.getKey().equals(workspaceProperty.getKey())
                    && propertyStatuses[i] == SandboxStatus.PUBLIC) {
                return property;
            }
        }
        return null;
    }

    public static boolean isPublicPropertyEdited(
            List<Property> properties,
            SandboxStatus[] propertyStatuses,
            Property workspaceProperty
    ) {
        for (int i = 0; i < properties.size(); i++) {
            Property property = properties.get(i);
            if (property.getName().equals(workspaceProperty.getName())
                    && property.getKey().equals(workspaceProperty.getKey())
                    && propertyStatuses[i] == SandboxStatus.PUBLIC_CHANGED) {
                return true;
            }
        }
        return false;
    }
}
