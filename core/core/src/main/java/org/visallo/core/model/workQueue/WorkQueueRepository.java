package org.visallo.core.model.workQueue;


import org.json.JSONArray;
import org.json.JSONObject;
import org.vertexium.*;
import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.ingest.WorkerSpout;
import org.visallo.core.ingest.graphProperty.GraphPropertyMessage;
import org.visallo.core.model.FlushFlag;
import org.visallo.core.model.WorkQueueNames;
import org.visallo.core.model.notification.SystemNotification;
import org.visallo.core.model.notification.UserNotification;
import org.visallo.core.model.properties.types.VisalloPropertyUpdate;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.status.model.Status;
import org.visallo.core.user.User;
import org.visallo.core.util.ClientApiConverter;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.clientapi.model.ClientApiWorkspace;
import org.visallo.web.clientapi.model.UserStatus;

import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class WorkQueueRepository {
    protected static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(WorkQueueRepository.class);
    protected final Configuration configuration;
    protected final WorkQueueNames workQueueNames;
    private final Graph graph;

    protected WorkQueueRepository(Graph graph, WorkQueueNames workQueueNames, Configuration configuration) {
        this.graph = graph;
        this.workQueueNames = workQueueNames;
        this.configuration = configuration;
    }

    public void pushGraphPropertyQueue(final Element element, final Property property, Priority priority) {
        checkNotNull(property, "property cannot be null");
        pushGraphPropertyQueue(element, property.getKey(), property.getName(), priority);
    }

    public void pushGraphVisalloPropertyQueue(final Element element, final Iterable<VisalloPropertyUpdate> properties, Priority priority) {
        pushGraphVisalloPropertyQueue(element, properties, null, null, priority);
    }

    public void pushGraphVisalloPropertyQueue(
            final Element element,
            final Iterable<VisalloPropertyUpdate> properties,
            String workspaceId,
            String visibilitySource,
            Priority priority
    ) {
        for (VisalloPropertyUpdate property : properties) {
            pushGraphPropertyQueue(element, property.getPropertyKey(), property.getPropertyName(), workspaceId, visibilitySource, priority, FlushFlag.DEFAULT);
        }
    }

    public void pushGraphPropertyQueue(final Element element, final Property property, String workspaceId, String visibilitySource, Priority priority) {
        pushGraphPropertyQueue(element, property.getKey(), property.getName(), workspaceId, visibilitySource, priority);
    }

    public void pushGraphPropertyQueue(final Element element, final Property property, String workspaceId, String visibilitySource, Priority priority, FlushFlag flushFlag) {
        pushGraphPropertyQueue(element, property.getKey(), property.getName(), workspaceId, visibilitySource, priority);
    }

    public void pushElementImageQueue(final Element element, final Property property, Priority priority) {
        pushElementImageQueue(element, property.getKey(), property.getName(), priority);
    }

    public void pushElementImageQueue(final Element element, String propertyKey, final String propertyName, Priority priority) {
        getGraph().flush();
        checkNotNull(element);
        JSONObject data = new JSONObject();
        if (element instanceof Vertex) {
            data.put("graphVertexId", element.getId());
        } else if (element instanceof Edge) {
            data.put("graphEdgeId", element.getId());
        } else {
            throw new VisalloException("Unexpected element type: " + element.getClass().getName());
        }
        data.put("propertyKey", propertyKey);
        data.put("propertyName", propertyName);
        pushOnQueue(workQueueNames.getGraphPropertyQueueName(), FlushFlag.DEFAULT, data, priority);

        broadcastEntityImage(element, propertyKey, propertyName);
    }

    public void pushGraphPropertyQueue(final Element element, String propertyKey, final String propertyName, Priority priority) {
        checkNotNull(element, "element cannot be null");
        pushGraphPropertyQueue(element, propertyKey, propertyName, null, null, priority);
    }

    public void pushGraphPropertyQueue(
            final Element element,
            String propertyKey,
            final String propertyName,
            String workspaceId,
            String visibilitySource,
            Priority priority
    ) {
        pushGraphPropertyQueue(element, propertyKey, propertyName, workspaceId, visibilitySource, priority, FlushFlag.DEFAULT);
    }


    public void pushMultipleGraphPropertyQueue(final Iterable<? extends Element> elements,
                                               String propertyKey,
                                               final String propertyName,
                                               String workspaceId,
                                               String visibilitySource,
                                               Priority priority,
                                               FlushFlag flushFlag) {

        checkNotNull(elements);
        if (!elements.iterator().hasNext()) {
            return;
        }

        getGraph().flush();

        JSONObject data = createPropertySpecificJSON(propertyKey, propertyName, workspaceId, visibilitySource);
        JSONArray vertices = new JSONArray();
        JSONArray edges = new JSONArray();

        for (Element element : elements) {
            if (element instanceof Vertex) {
                vertices.put(element.getId());
            } else if (element instanceof Edge) {
                edges.put(element.getId());
            } else {
                throw new VisalloException("Unexpected element type: " + element.getClass().getName());
            }
        }

        data.put(GraphPropertyMessage.GRAPH_VERTEX_ID, vertices);
        data.put(GraphPropertyMessage.GRAPH_EDGE_ID, edges);

        pushOnQueue(workQueueNames.getGraphPropertyQueueName(), flushFlag, data, priority);

        for (Element element : elements) {
            if (shouldBroadcastGraphPropertyChange(element, propertyKey, propertyName, workspaceId, priority)) {
                broadcastPropertyChange(element, propertyKey, propertyName, workspaceId);
            }
        }
    }

    public void pushGraphPropertyQueue(
            final Element element,
            String propertyKey,
            final String propertyName,
            String workspaceId,
            String visibilitySource,
            Priority priority,
            FlushFlag flushFlag
    ) {
        getGraph().flush();
        checkNotNull(element);

        JSONObject data = createPropertySpecificJSON(propertyKey, propertyName, workspaceId, visibilitySource);

        if (element instanceof Vertex) {
            data.put(GraphPropertyMessage.GRAPH_VERTEX_ID, element.getId());
        } else if (element instanceof Edge) {
            data.put(GraphPropertyMessage.GRAPH_EDGE_ID, element.getId());
        } else {
            throw new VisalloException("Unexpected element type: " + element.getClass().getName());
        }

        pushOnQueue(workQueueNames.getGraphPropertyQueueName(), flushFlag, data, priority);

        if (shouldBroadcastGraphPropertyChange(element, propertyKey, propertyName, workspaceId, priority)) {
            broadcastPropertyChange(element, propertyKey, propertyName, workspaceId);
        }
    }

    private JSONObject createPropertySpecificJSON(
            String propertyKey,
            final String propertyName,
            String workspaceId,
            String visibilitySource) {
        JSONObject data = new JSONObject();

        if (workspaceId != null && !workspaceId.equals("")) {
            data.put("workspaceId", workspaceId);
            data.put("visibilitySource", visibilitySource);
        }
        data.put("propertyKey", propertyKey);
        data.put("propertyName", propertyName);
        return data;
    }

    public void pushGraphPropertyQueue(final Element element, Priority priority) {
        pushGraphPropertyQueue(element, null, null, priority, FlushFlag.DEFAULT);
    }

    public void pushGraphPropertyQueue(
            final Element element,
            String workspaceId,
            String visibilitySource,
            Priority priority,
            FlushFlag flushFlag
    ) {
        getGraph().flush();
        checkNotNull(element);
        JSONObject data = new JSONObject();
        if (element instanceof Vertex) {
            data.put(GraphPropertyMessage.GRAPH_VERTEX_ID, element.getId());
        } else if (element instanceof Edge) {
            data.put(GraphPropertyMessage.GRAPH_EDGE_ID, element.getId());
        } else {
            throw new VisalloException("Unexpected element type: " + element.getClass().getName());
        }

        if (workspaceId != null && !workspaceId.equals("")) {
            data.put("workspaceId", workspaceId);
            data.put("visibilitySource", visibilitySource);
        }

        pushOnQueue(workQueueNames.getGraphPropertyQueueName(), flushFlag, data, priority);
    }

    public void pushVertexIds(Iterable<String> vertexIds, Priority priority, FlushFlag flushFlag) {
        for (String vertexId : vertexIds) {
            pushVertexId(vertexId, priority, flushFlag);
        }
    }

    private void pushVertexId(String vertexId, Priority priority, FlushFlag flushFlag) {
        JSONObject data = new JSONObject();
        data.put(GraphPropertyMessage.GRAPH_VERTEX_ID, vertexId);
        pushOnQueue(workQueueNames.getGraphPropertyQueueName(), flushFlag, data, priority);
    }

    protected boolean shouldBroadcastGraphPropertyChange(Element element, String propertyKey, String propertyName, String workspaceId, Priority priority) {
        return shouldBroadcast(priority);
    }

    protected boolean shouldBroadcastTextUpdate(String vertexId, Priority priority) {
        return shouldBroadcast(priority);
    }

    protected boolean shouldBroadcast(Priority priority) {
        return priority != Priority.LOW;
    }

    public void pushLongRunningProcessQueue(JSONObject queueItem) {
        pushLongRunningProcessQueue(queueItem, Priority.NORMAL);
    }

    public void pushLongRunningProcessQueue(JSONObject queueItem, Priority priority) {
        broadcastLongRunningProcessChange(queueItem);
        pushOnQueue(workQueueNames.getLongRunningProcessQueueName(), FlushFlag.DEFAULT, queueItem, priority);
    }

    public void broadcast(String type, JSONObject data, JSONObject permissions) {
        checkNotNull(type);

        JSONObject json = new JSONObject();
        json.putOpt("permissions", permissions);
        json.putOpt("data", data);
        json.put("type", type);
        broadcastJson(json);
    }

    public void broadcastLongRunningProcessDeleted(JSONObject longRunningProcessQueueItem) {
        String userId = longRunningProcessQueueItem.optString("userId");
        checkNotNull(userId, "userId cannot be null");
        JSONObject json = new JSONObject();
        json.put("type", "longRunningProcessDeleted");
        JSONObject permissions = new JSONObject();
        JSONArray users = new JSONArray();
        users.put(userId);
        permissions.put("users", users);
        json.put("permissions", permissions);
        json.put("data", longRunningProcessQueueItem.get("id"));
        broadcastJson(json);
    }

    public void broadcastLongRunningProcessChange(JSONObject longRunningProcessQueueItem) {
        String userId = longRunningProcessQueueItem.optString("userId");
        checkNotNull(userId, "userId cannot be null");
        JSONObject json = new JSONObject();
        json.put("type", "longRunningProcessChange");
        JSONObject permissions = new JSONObject();
        JSONArray users = new JSONArray();
        users.put(userId);
        permissions.put("users", users);
        json.put("permissions", permissions);
        JSONObject dataJson = new JSONObject(longRunningProcessQueueItem.toString());

        /// because results can get quite large we don't want this going on in a web socket message
        if (dataJson.has("results")) {
            dataJson.remove("results");
        }

        json.put("data", dataJson);
        broadcastJson(json);
    }

    public void broadcastElement(Element element, String workspaceId) {
        broadcastPropertyChange(element, null, null, workspaceId);
    }

    public void pushElement(Element element, Priority priority) {
        pushGraphPropertyQueue(element, null, null, priority);
    }

    public void pushElements(Iterable<? extends Element> elements) {
        pushMultipleGraphPropertyQueue(elements, null, null, null, null, Priority.NORMAL, FlushFlag.DEFAULT);
    }

    public void pushElement(Element element) {
        pushElement(element, Priority.NORMAL);
    }

    public void pushEdgeDeletion(Edge edge) {
        broadcastEdgeDeletion(edge);
    }

    protected void broadcastEdgeDeletion(Edge edge) {
        JSONObject dataJson = new JSONObject();
        if (edge != null) {
            dataJson.put("edgeId", edge.getId());
            dataJson.put("outVertexId", edge.getVertexId(Direction.OUT));
            dataJson.put("inVertexId", edge.getVertexId(Direction.IN));
        }

        JSONObject json = new JSONObject();
        json.put("type", "edgeDeletion");
        json.put("data", dataJson);
        broadcastJson(json);
    }

    public void pushVertexDeletion(Vertex vertex) {
        broadcastVertexDeletion(vertex);
    }

    protected void broadcastVertexDeletion(Vertex vertex) {
        JSONArray vertexIds = new JSONArray();
        vertexIds.put(vertex.getId());
        broadcastVerticesDeletion(vertexIds);
    }

    public void pushVerticesDeletion(JSONArray verticesDeleted) {
        broadcastVerticesDeletion(verticesDeleted);
    }

    protected void broadcastVerticesDeletion(JSONArray verticesDeleted) {
        JSONObject dataJson = new JSONObject();
        if (verticesDeleted != null) {
            dataJson.put("vertexIds", verticesDeleted);
        }

        JSONObject json = new JSONObject();
        json.put("type", "verticesDeleted");
        json.put("data", dataJson);
        broadcastJson(json);
    }

    public void pushTextUpdated(String vertexId) {
        pushTextUpdated(vertexId, Priority.NORMAL);
    }

    public void pushTextUpdated(String vertexId, Priority priority) {
        if (shouldBroadcastTextUpdate(vertexId, priority)) {
            broadcastTextUpdated(vertexId);
        }
    }

    protected void broadcastTextUpdated(String vertexId) {
        JSONObject dataJson = new JSONObject();
        if (vertexId != null) {
            dataJson.put("graphVertexId", vertexId);
        }

        JSONObject json = new JSONObject();
        json.put("type", "textUpdated");
        json.put("data", dataJson);
        broadcastJson(json);
    }

    public void pushUserStatusChange(User user, UserStatus status) {
        broadcastUserStatusChange(user, status);
    }

    protected void broadcastUserStatusChange(User user, UserStatus status) {
        JSONObject json = new JSONObject();
        json.put("type", "userStatusChange");
        JSONObject data = UserRepository.toJson(user);
        data.put("status", status.toString());
        json.put("data", data);
        broadcastJson(json);
    }

    public void pushUserCurrentWorkspaceChange(User user, String workspaceId) {
        broadcastUserWorkspaceChange(user, workspaceId);
    }

    public void pushWorkspaceChange(ClientApiWorkspace workspace, List<ClientApiWorkspace.User> previousUsers, String changedByUserId, String changedBySessionId) {
        broadcastWorkspace(workspace, previousUsers, changedByUserId, changedBySessionId);
    }

    protected void broadcastUserWorkspaceChange(User user, String workspaceId) {
        JSONObject json = new JSONObject();
        json.put("type", "userWorkspaceChange");
        JSONObject data = UserRepository.toJson(user);
        data.put("workspaceId", workspaceId);
        json.put("data", data);
        broadcastJson(json);
    }

    protected void broadcastWorkspace(ClientApiWorkspace workspace, List<ClientApiWorkspace.User> previousUsers, String changedByUserId, String changedBySessionId) {
        JSONObject json = new JSONObject();
        json.put("type", "workspaceChange");
        json.put("modifiedBy", changedByUserId);
        json.put("permissions", getPermissionsWithUsers(workspace, previousUsers));

        if (changedBySessionId != null) {
            JSONArray sessionIds = new JSONArray();
            sessionIds.put(changedBySessionId);
            json.put("excludeSessionIds", sessionIds);
        }
        json.put("data", new JSONObject(ClientApiConverter.clientApiToString(workspace)));
        broadcastJson(json);
    }

    public void pushWorkspaceDelete(ClientApiWorkspace workspace) {
        JSONObject json = new JSONObject();
        json.put("type", "workspaceDelete");
        json.put("permissions", getPermissionsWithUsers(workspace, null));
        json.put("workspaceId", workspace.getWorkspaceId());
        broadcastJson(json);
    }

    public void pushWorkspaceDelete(String workspaceId, String userId) {
        JSONObject json = new JSONObject();
        json.put("type", "workspaceDelete");
        JSONObject permissions = new JSONObject();
        JSONArray users = new JSONArray();
        users.put(userId);
        permissions.put("users", users);
        json.put("permissions", permissions);
        json.put("workspaceId", workspaceId);
        broadcastJson(json);
    }

    private JSONObject getPermissionsWithUsers(ClientApiWorkspace workspace, List<ClientApiWorkspace.User> previousUsers) {
        JSONObject permissions = new JSONObject();
        JSONArray users = new JSONArray();
        if (previousUsers != null) {
            for (ClientApiWorkspace.User user : previousUsers) {
                users.put(user.getUserId());
            }
        }
        for (ClientApiWorkspace.User user : workspace.getUsers()) {
            users.put(user.getUserId());
        }
        permissions.put("users", users);
        return permissions;
    }

    public void pushSessionExpiration(String userId, String sessionId) {
        JSONObject json = new JSONObject();
        json.put("type", "sessionExpiration");

        JSONObject permissions = new JSONObject();
        JSONArray users = new JSONArray();
        users.put(userId);
        permissions.put("users", users);
        JSONArray sessionIds = new JSONArray();
        sessionIds.put(sessionId);
        permissions.put("sessionIds", sessionIds);
        json.put("permissions", permissions);
        json.putOpt("sessionId", sessionId);
        broadcastJson(json);
    }

    public void pushUserNotification(UserNotification notification) {
        JSONObject json = new JSONObject();
        json.put("type", "notification");

        JSONObject permissions = new JSONObject();
        JSONArray users = new JSONArray();
        users.put(notification.getUserId());
        permissions.put("users", users);
        json.put("permissions", permissions);

        JSONObject data = new JSONObject();
        json.put("data", data);
        data.put("notification", notification.toJSONObject());
        broadcastJson(json);
    }

    public void pushSystemNotification(SystemNotification notification) {
        JSONObject json = new JSONObject();
        json.put("type", "notification");
        JSONObject data = new JSONObject();
        json.put("data", data);
        data.put("notification", notification.toJSONObject());
        broadcastJson(json);
    }

    public void pushSystemNotificationUpdate(SystemNotification notification) {
        JSONObject json = new JSONObject();
        json.put("type", "systemNotificationUpdated");
        JSONObject data = new JSONObject();
        json.put("data", data);
        data.put("notification", notification.toJSONObject());
        broadcastJson(json);
    }

    public void pushSystemNotificationEnded(String notificationId) {
        JSONObject json = new JSONObject();
        json.put("type", "systemNotificationEnded");
        JSONObject data = new JSONObject();
        json.put("data", data);
        data.put("notificationId", notificationId);
        broadcastJson(json);
    }

    protected void broadcastPropertyChange(Element element, String propertyKey, String propertyName, String workspaceId) {
        try {
            JSONObject json;
            if (element instanceof Vertex) {
                json = getBroadcastPropertyChangeJson((Vertex) element, propertyKey, propertyName, workspaceId);
            } else if (element instanceof Edge) {
                json = getBroadcastPropertyChangeJson((Edge) element, propertyKey, propertyName, workspaceId);
            } else {
                throw new VisalloException("Unexpected element type: " + element.getClass().getName());
            }
            broadcastJson(json);
        } catch (Exception ex) {
            throw new VisalloException("Could not broadcast property change", ex);
        }
    }

    protected void broadcastEntityImage(Element element, String propertyKey, String propertyName) {
        try {
            JSONObject json = getBroadcastEntityImageJson((Vertex) element);
            broadcastJson(json);
        } catch (Exception ex) {
            throw new VisalloException("Could not broadcast property change", ex);
        }
    }

    protected abstract void broadcastJson(JSONObject json);

    protected JSONObject getBroadcastEntityImageJson(Vertex graphVertex) {
        // TODO: only broadcast to workspace users if sandboxStatus is PRIVATE
        JSONObject json = new JSONObject();
        json.put("type", "entityImageUpdated");

        JSONObject dataJson = new JSONObject();
        dataJson.put("graphVertexId", graphVertex.getId());

        json.put("data", dataJson);
        return json;
    }

    protected JSONObject getBroadcastPropertyChangeJson(Vertex graphVertex, String propertyKey, String propertyName, String workspaceId) {
        // TODO: only broadcast to workspace users if sandboxStatus is PRIVATE
        JSONObject json = new JSONObject();
        json.put("type", "propertyChange");

        JSONObject dataJson = new JSONObject();
        dataJson.put("graphVertexId", graphVertex.getId());
        dataJson.putOpt("workspaceId", workspaceId);

        json.put("data", dataJson);

        return json;
    }

    protected JSONObject getBroadcastPropertyChangeJson(Edge edge, String propertyKey, String propertyName, String workspaceId) {
        // TODO: only broadcast to workspace users if sandboxStatus is PRIVATE
        JSONObject json = new JSONObject();
        json.put("type", "propertyChange");

        JSONObject dataJson = new JSONObject();
        dataJson.put("graphEdgeId", edge.getId());
        dataJson.putOpt("workspaceId", workspaceId);

        json.put("data", dataJson);

        return json;
    }

    public abstract void pushOnQueue(String queueName, @Deprecated FlushFlag flushFlag, JSONObject json, Priority priority);

    public void init(Map map) {

    }

    public abstract void flush();

    public abstract void format();

    public Graph getGraph() {
        return graph;
    }

    public abstract void subscribeToBroadcastMessages(BroadcastConsumer broadcastConsumer);

    public abstract WorkerSpout createWorkerSpout(String queueName);

    public void shutdown() {

    }

    public void broadcastPublishVertexDelete(Vertex vertex) {
        broadcastPublish(vertex, PublishType.DELETE);
    }

    public void broadcastPublishVertex(Vertex vertex) {
        broadcastPublish(vertex, PublishType.TO_PUBLIC);
    }

    public void broadcastUndoVertexDelete(Vertex vertex) {
        broadcastPublish(vertex, PublishType.UNDO_DELETE);
    }

    public void broadcastUndoVertex(Vertex vertex) {
        broadcastPublish(vertex, PublishType.UNDO);
    }

    public void broadcastPublishPropertyDelete(Element element, String key, String name) {
        broadcastPublish(element, key, name, PublishType.DELETE);
    }

    public void broadcastPublishProperty(Element element, String key, String name) {
        broadcastPublish(element, key, name, PublishType.TO_PUBLIC);
    }

    public void broadcastUndoPropertyDelete(Element element, String key, String name) {
        broadcastPublish(element, key, name, PublishType.UNDO_DELETE);
    }

    public void broadcastUndoProperty(Element element, String key, String name) {
        broadcastPublish(element, key, name, PublishType.UNDO);
    }

    public void broadcastPublishEdgeDelete(Edge edge) {
        broadcastPublish(edge, PublishType.DELETE);
    }

    public void broadcastPublishEdge(Edge edge) {
        broadcastPublish(edge, PublishType.TO_PUBLIC);
    }

    public void broadcastUndoEdgeDelete(Edge edge) {
        broadcastPublish(edge, PublishType.UNDO_DELETE);
    }

    public void broadcastUndoEdge(Edge edge) {
        broadcastPublish(edge, PublishType.UNDO);
    }

    private void broadcastPublish(Element element, PublishType publishType) {
        broadcastPublish(element, null, null, publishType);
    }

    private void broadcastPublish(Element element, String propertyKey, String propertyName, PublishType publishType) {
        try {
            JSONObject json;
            if (element instanceof Vertex) {
                json = getBroadcastPublishJson((Vertex) element, propertyKey, propertyName, publishType);
            } else if (element instanceof Edge) {
                json = getBroadcastPublishJson((Edge) element, propertyKey, propertyName, publishType);
            } else {
                throw new VisalloException("Unexpected element type: " + element.getClass().getName());
            }
            broadcastJson(json);
        } catch (Exception ex) {
            throw new VisalloException("Could not broadcast publish", ex);
        }
    }

    protected JSONObject getBroadcastPublishJson(Vertex graphVertex, String propertyKey, String propertyName, PublishType publishType) {
        JSONObject json = new JSONObject();
        json.put("type", "publish");

        JSONObject dataJson = new JSONObject();
        dataJson.put("graphVertexId", graphVertex.getId());
        dataJson.put("publishType", publishType.getJsonString());
        if (propertyName == null) {
            dataJson.put("objectType", "vertex");
        } else {
            dataJson.put("objectType", "property");
        }
        json.put("data", dataJson);

        return json;
    }

    protected JSONObject getBroadcastPublishJson(Edge edge, String propertyKey, String propertyName, PublishType publishType) {
        JSONObject json = new JSONObject();
        json.put("type", "publish");

        JSONObject dataJson = new JSONObject();
        dataJson.put("graphEdgeId", edge.getId());
        dataJson.put("publishType", publishType.getJsonString());
        if (propertyName == null) {
            dataJson.put("objectType", "edge");
        } else {
            dataJson.put("objectType", "property");
        }
        json.put("data", dataJson);

        return json;
    }

    public abstract Map<String, Status> getQueuesStatus();

    private enum PublishType {
        TO_PUBLIC("toPublic"),
        DELETE("delete"),
        UNDO_DELETE("undoDelete"),
        UNDO("undo");

        private final String jsonString;

        PublishType(String jsonString) {
            this.jsonString = jsonString;
        }

        public String getJsonString() {
            return jsonString;
        }
    }

    public static abstract class BroadcastConsumer {
        public abstract void broadcastReceived(JSONObject json);
    }
}
