package org.visallo.core.model.workspace;

import com.google.inject.Inject;
import org.json.JSONArray;
import org.vertexium.*;
import org.vertexium.util.IterableUtils;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.user.User;
import org.visallo.core.util.SandboxStatusUtil;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.clientapi.model.*;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class WorkspaceUndoHelper {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(WorkspaceUndoHelper.class);

    private final Graph graph;
    private final WorkQueueRepository workQueueRepository;
    private final WorkspaceHelper workspaceHelper;

    @Inject
    public WorkspaceUndoHelper(Graph graph, WorkspaceHelper workspaceHelper, WorkQueueRepository workQueueRepository) {
        this.graph = graph;
        this.workspaceHelper = workspaceHelper;
        this.workQueueRepository = workQueueRepository;
    }

    public void undo(Iterable<ClientApiUndoItem> undoItems, ClientApiWorkspaceUndoResponse workspaceUndoResponse,
                     String workspaceId, User user, Authorizations authorizations) {
        undoVertices(undoItems, workspaceUndoResponse, workspaceId, user, authorizations);
        undoEdges(undoItems, workspaceUndoResponse, workspaceId, user, authorizations);
        undoProperties(undoItems, workspaceUndoResponse, workspaceId, authorizations);
    }

    private void undoVertices(Iterable<ClientApiUndoItem> undoItems, ClientApiWorkspaceUndoResponse workspaceUndoResponse,
                              String workspaceId, User user, Authorizations authorizations) {
        LOGGER.debug("BEGIN undoVertices");
        JSONArray verticesDeleted = new JSONArray();
        for (ClientApiUndoItem undoItem : undoItems) {
            try {
                if (!(undoItem instanceof ClientApiVertexUndoItem)) {
                    continue;
                }
                ClientApiVertexUndoItem vertexUndoItem = (ClientApiVertexUndoItem) undoItem;
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
                    undoItem.setErrorMessage(msg);
                    workspaceUndoResponse.addFailure(undoItem);
                } else {
                    workspaceHelper.deleteVertex(vertex, workspaceId, false, Priority.HIGH, authorizations, user);
                    verticesDeleted.put(vertexId);
                    graph.flush();
                    workQueueRepository.broadcastUndoVertex(vertex);
                }
            } catch (Exception ex) {
                LOGGER.error("Error undoing %s", undoItem.toString(), ex);
                undoItem.setErrorMessage(ex.getMessage());
                workspaceUndoResponse.addFailure(undoItem);
            }
        }
        LOGGER.debug("END undoVertices");
        if (verticesDeleted.length() > 0) {
            workQueueRepository.pushVerticesDeletion(verticesDeleted);
        }
        graph.flush();
    }

    private void undoEdges(Iterable<ClientApiUndoItem> undoItems, ClientApiWorkspaceUndoResponse workspaceUndoResponse,
                           String workspaceId, User user, Authorizations authorizations) {
        LOGGER.debug("BEGIN undoEdges");
        for (ClientApiUndoItem undoItem : undoItems) {
            try {
                if (!(undoItem instanceof ClientApiRelationshipUndoItem)) {
                    continue;
                }

                ClientApiRelationshipUndoItem relationshipUndoItem = (ClientApiRelationshipUndoItem) undoItem;
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
                    undoItem.setErrorMessage(error_msg);
                    workspaceUndoResponse.addFailure(undoItem);
                } else {
                    workspaceHelper.deleteEdge(workspaceId, edge, sourceVertex, destVertex, false, Priority.HIGH, authorizations, user);
                    graph.flush();
                    workQueueRepository.broadcastUndoEdge(edge);
                }
            } catch (Exception ex) {
                LOGGER.error("Error publishing %s", undoItem.toString(), ex);
                undoItem.setErrorMessage(ex.getMessage());
                workspaceUndoResponse.addFailure(undoItem);
            }
        }
        LOGGER.debug("END undoEdges");
        graph.flush();
    }

    private void undoProperties(Iterable<ClientApiUndoItem> undoItems, ClientApiWorkspaceUndoResponse workspaceUndoResponse,
                                String workspaceId, Authorizations authorizations) {
        LOGGER.debug("BEGIN undoProperties");
        for (ClientApiUndoItem undoItem : undoItems) {
            try {
                if (!(undoItem instanceof ClientApiPropertyUndoItem)) {
                    continue;
                }
                ClientApiPropertyUndoItem propertyUndoItem = (ClientApiPropertyUndoItem) undoItem;
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

                for (Property property : properties) {
                    if (WorkspaceDiffHelper.isPublicDelete(property, authorizations) &&
                            WorkspaceDiffHelper.isPublicPropertyEdited(properties, sandboxStatuses, property)) {
                        publicProperty = property;
                        break;
                    }
                }

                for (int propertyIndex = 0; propertyIndex < properties.size(); propertyIndex++) {
                    Property property = properties.get(propertyIndex);
                    if (propertyVisibilityString != null &&
                            !property.getVisibility().getVisibilityString().equals(propertyVisibilityString)) {
                        continue;
                    }
                    SandboxStatus propertySandboxStatus = sandboxStatuses[propertyIndex];

                    if (WorkspaceDiffHelper.isPublicDelete(property, authorizations)) {
                        if (publicProperty == null) {
                            LOGGER.debug("un-hiding property: %s (workspaceId: %s)", property, workspaceId);
                            vertex.markPropertyVisible(property, new Visibility(workspaceId), authorizations);
                            graph.flush();
                            workQueueRepository.broadcastUndoPropertyDelete(vertex, propertyKey, propertyName);
                        }
                    } else if (propertySandboxStatus == SandboxStatus.PUBLIC) {
                        String error_msg = "Cannot undo a public property";
                        LOGGER.warn(error_msg);
                        undoItem.setErrorMessage(error_msg);
                        workspaceUndoResponse.addFailure(undoItem);
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
                LOGGER.error("Error publishing %s", undoItem.toString(), ex);
                undoItem.setErrorMessage(ex.getMessage());
                workspaceUndoResponse.addFailure(undoItem);
            }
        }
        LOGGER.debug("End undoProperties");
        graph.flush();
    }
}
