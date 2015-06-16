package org.visallo.web.routes.workspace;

import com.google.common.base.Joiner;
import com.google.inject.Inject;
import com.v5analytics.webster.HandlerChain;
import org.json.JSONArray;
import org.vertexium.*;
import org.vertexium.util.IterableUtils;
import org.visallo.core.config.Configuration;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.model.workspace.diff.WorkspaceDiffHelper;
import org.visallo.core.user.User;
import org.visallo.core.util.SandboxStatusUtil;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.BaseRequestHandler;
import org.visallo.web.clientapi.model.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class WorkspaceUndo extends BaseRequestHandler {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(WorkspaceUndo.class);
    private final Graph graph;
    private final WorkQueueRepository workQueueRepository;
    private final WorkspaceHelper workspaceHelper;
    private String entityHasImageIri;
    private String artifactContainsImageOfEntityIri;
    private final OntologyRepository ontologyRepository;

    @Inject
    public WorkspaceUndo(
            final Configuration configuration,
            final Graph graph,
            final UserRepository userRepository,
            final WorkspaceHelper workspaceHelper,
            final WorkspaceRepository workspaceRepository,
            final WorkQueueRepository workQueueRepository,
            final OntologyRepository ontologyRepository) {
        super(userRepository, workspaceRepository, configuration);
        this.graph = graph;
        this.workspaceHelper = workspaceHelper;
        this.workQueueRepository = workQueueRepository;
        this.ontologyRepository = ontologyRepository;

        this.entityHasImageIri = ontologyRepository.getRelationshipIRIByIntent("entityHasImage");
        if (this.entityHasImageIri == null) {
            LOGGER.warn("'entityHasImage' intent has not been defined. Please update your ontology.");
        }

        this.artifactContainsImageOfEntityIri = ontologyRepository.getRelationshipIRIByIntent("artifactContainsImageOfEntity");
        if (this.artifactContainsImageOfEntityIri == null) {
            LOGGER.warn("'artifactContainsImageOfEntity' intent has not been defined. Please update your ontology.");
        }
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        if (this.entityHasImageIri == null) {
            this.entityHasImageIri = ontologyRepository.getRequiredRelationshipIRIByIntent("entityHasImage");
        }
        if (this.artifactContainsImageOfEntityIri == null) {
            this.artifactContainsImageOfEntityIri = ontologyRepository.getRequiredRelationshipIRIByIntent("artifactContainsImageOfEntity");
        }

        String undoDataString = getRequiredParameter(request, "undoData");
        ClientApiUndoItem[] undoData = getObjectMapper().readValue(undoDataString, ClientApiUndoItem[].class);
        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);
        String workspaceId = getActiveWorkspaceId(request);

        LOGGER.debug("undoing:\n%s", Joiner.on("\n").join(undoData));
        ClientApiWorkspaceUndoResponse workspaceUndoResponse = new ClientApiWorkspaceUndoResponse();
        undoVertices(undoData, workspaceUndoResponse, workspaceId, Priority.HIGH, user, authorizations);
        undoEdges(undoData, workspaceUndoResponse, workspaceId, Priority.HIGH, authorizations, user);
        undoProperties(undoData, workspaceUndoResponse, workspaceId, authorizations);
        LOGGER.debug("undoing results: %s", workspaceUndoResponse);
        respondWithClientApiObject(response, workspaceUndoResponse);
    }

    private void undoVertices(ClientApiUndoItem[] undoItem, ClientApiWorkspaceUndoResponse workspaceUndoResponse, String workspaceId, Priority priority, User user, Authorizations authorizations) {
        LOGGER.debug("BEGIN undoVertices");
        JSONArray verticesDeleted = new JSONArray();
        for (ClientApiUndoItem data : undoItem) {
            try {
                if (!(data instanceof ClientApiVertexUndoItem)) {
                    continue;
                }
                ClientApiVertexUndoItem vertexUndoItem = (ClientApiVertexUndoItem) data;
                String vertexId = vertexUndoItem.getVertexId();
                checkNotNull(vertexId);
                Vertex vertex = graph.getVertex(vertexId, FetchHint.ALL_INCLUDING_HIDDEN, authorizations);
                checkNotNull(vertex);
                if (WorkspaceDiffHelper.isPublicDelete(vertex, authorizations)) {
                    LOGGER.debug("un-hiding vertex: %s (workspaceId: %s)", vertex.getId(), workspaceId);
                    // TODO see WorkspaceHelper.deleteVertex for all the other things we need to bring back
                    graph.markVertexVisible(vertex, new Visibility(workspaceId), authorizations);
                    graph.flush();
                    workQueueRepository.broadcastUndoVertexDelete(vertex);
                } else if (SandboxStatusUtil.getSandboxStatus(vertex, workspaceId) == SandboxStatus.PUBLIC) {
                    String msg = "Cannot undo a public vertex";
                    LOGGER.warn(msg);
                    data.setErrorMessage(msg);
                    workspaceUndoResponse.addFailure(data);
                } else {
                    workspaceHelper.deleteVertex(vertex, workspaceId, false, priority, authorizations, user);
                    verticesDeleted.put(vertexId);
                    graph.flush();
                    workQueueRepository.broadcastUndoVertex(vertex);
                }
            } catch (Exception ex) {
                LOGGER.error("Error undoing %s", data.toString(), ex);
                data.setErrorMessage(ex.getMessage());
                workspaceUndoResponse.addFailure(data);
            }
        }
        LOGGER.debug("END undoVertices");
        if (verticesDeleted.length() > 0) {
            workQueueRepository.pushVerticesDeletion(verticesDeleted);
        }
        graph.flush();
    }

    private void undoEdges(
            ClientApiUndoItem[] undoItem,
            ClientApiWorkspaceUndoResponse workspaceUndoResponse,
            String workspaceId,
            Priority priority,
            Authorizations authorizations,
            User user
    ) {
        LOGGER.debug("BEGIN undoEdges");
        for (ClientApiUndoItem data : undoItem) {
            try {
                if (!(data instanceof ClientApiRelationshipUndoItem)) {
                    continue;
                }

                ClientApiRelationshipUndoItem relationshipUndoItem = (ClientApiRelationshipUndoItem) data;
                Edge edge = graph.getEdge(relationshipUndoItem.getEdgeId(), FetchHint.ALL_INCLUDING_HIDDEN, authorizations);
                if (edge == null) {
                    continue;
                }
                Vertex sourceVertex = edge.getVertex(Direction.OUT, authorizations);
                Vertex destVertex = edge.getVertex(Direction.IN, authorizations);
                if (sourceVertex == null || destVertex == null) {
                    continue;
                }

                checkNotNull(edge);

                if (WorkspaceDiffHelper.isPublicDelete(edge, authorizations)) {
                    LOGGER.debug("un-hiding edge: %s (workspaceId: %s)", edge.getId(), workspaceId);
                    // TODO see workspaceHelper.deleteEdge for all the other things we need to bring back
                    graph.markEdgeVisible(edge, new Visibility(workspaceId), authorizations);
                    graph.flush();
                    workQueueRepository.broadcastUndoEdgeDelete(edge);
                } else if (SandboxStatusUtil.getSandboxStatus(edge, workspaceId) == SandboxStatus.PUBLIC) {
                    String error_msg = "Cannot undo a public edge";
                    LOGGER.warn(error_msg);
                    data.setErrorMessage(error_msg);
                    workspaceUndoResponse.addFailure(data);
                } else {
                    workspaceHelper.deleteEdge(workspaceId, edge, sourceVertex, destVertex, false, priority, authorizations, user);
                    graph.flush();
                    workQueueRepository.broadcastUndoEdge(edge);
                }
            } catch (Exception ex) {
                LOGGER.error("Error publishing %s", data.toString(), ex);
                data.setErrorMessage(ex.getMessage());
                workspaceUndoResponse.addFailure(data);
            }
        }
        LOGGER.debug("END undoEdges");
        graph.flush();
    }

    private void undoProperties(ClientApiUndoItem[] undoItem, ClientApiWorkspaceUndoResponse workspaceUndoResponse, String workspaceId, Authorizations authorizations) {
        LOGGER.debug("BEGIN undoProperties");
        for (ClientApiUndoItem data : undoItem) {
            try {
                if (!(data instanceof ClientApiPropertyUndoItem)) {
                    continue;
                }
                ClientApiPropertyUndoItem propertyUndoItem = (ClientApiPropertyUndoItem) data;
                Vertex vertex = graph.getVertex(propertyUndoItem.getVertexId(), FetchHint.ALL_INCLUDING_HIDDEN, authorizations);
                if (vertex == null) {
                    continue;
                }
                String propertyKey = propertyUndoItem.getKey();
                String propertyName = propertyUndoItem.getName();
                String propertyVisibilityString = propertyUndoItem.getVisibilityString();
                List<Property> properties = IterableUtils.toList(vertex.getProperties(propertyKey, propertyName));
                SandboxStatus[] sandboxStatuses = SandboxStatusUtil.getPropertySandboxStatuses(properties, workspaceId);
                Property publicProperty = null;
                for (int propertyIndex = 0; propertyIndex < properties.size(); propertyIndex++) {
                    Property property = properties.get(propertyIndex);
                    if (propertyVisibilityString != null &&
                            !property.getVisibility().getVisibilityString().equals(propertyVisibilityString)) {
                        continue;
                    }
                    SandboxStatus propertySandboxStatus = sandboxStatuses[propertyIndex];

                    if (WorkspaceDiffHelper.isPublicDelete(property, authorizations)) {
                        if (WorkspaceDiffHelper.isPublicPropertyEdited(properties, sandboxStatuses, property)) {
                            publicProperty = property;
                        } else {
                            LOGGER.debug("un-hiding property: %s (workspaceId: %s)", property, workspaceId);
                            vertex.markPropertyVisible(property, new Visibility(workspaceId), authorizations);
                            graph.flush();
                            workQueueRepository.broadcastUndoPropertyDelete(vertex, propertyKey, propertyName);
                        }
                    } else if (propertySandboxStatus == SandboxStatus.PUBLIC) {
                        String error_msg = "Cannot undo a public property";
                        LOGGER.warn(error_msg);
                        data.setErrorMessage(error_msg);
                        workspaceUndoResponse.addFailure(data);
                    } else if (propertySandboxStatus == SandboxStatus.PUBLIC_CHANGED) {
                        vertex.softDeleteProperty(propertyKey, propertyName, property.getVisibility(), authorizations);
                        if (publicProperty != null) {
                            vertex.markPropertyVisible(publicProperty, new Visibility(workspaceId), authorizations);
                        }
                        graph.flush();
                        workQueueRepository.broadcastUndoPropertyDelete(vertex, propertyKey, propertyName);
                    } else {
                        workspaceHelper.deleteProperty(vertex, property, false, workspaceId, Priority.HIGH, authorizations);
                        graph.flush();
                        workQueueRepository.broadcastUndoProperty(vertex, propertyKey, propertyName);
                    }
                }
            } catch (Exception ex) {
                LOGGER.error("Error publishing %s", data.toString(), ex);
                data.setErrorMessage(ex.getMessage());
                workspaceUndoResponse.addFailure(data);
            }
        }
        LOGGER.debug("End undoProperties");
        graph.flush();
    }

}
