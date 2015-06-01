package org.visallo.core.model.workspace;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.vertexium.*;
import org.vertexium.mutation.ElementMutation;
import org.vertexium.mutation.ExistingElementMutation;
import org.vertexium.util.ConvertingIterable;
import org.vertexium.util.IterableUtils;
import org.visallo.core.exception.VisalloAccessDeniedException;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.ingest.video.VideoFrameInfo;
import org.visallo.core.model.ontology.OntologyProperty;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.termMention.TermMentionRepository;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.model.workspace.diff.WorkspaceDiffHelper;
import org.visallo.core.security.VisalloVisibility;
import org.visallo.core.security.VisibilityTranslator;
import org.visallo.core.user.User;
import org.visallo.core.util.SandboxStatusUtil;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.clientapi.model.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class WorkspaceRepository {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(WorkspaceRepository.class);
    public static final String TO_ENTITY_ID_SEPARATOR = "_TO_ENTITY_";
    public static final String VISIBILITY_STRING = "workspace";
    public static final VisalloVisibility VISIBILITY = new VisalloVisibility(VISIBILITY_STRING);
    public static final String WORKSPACE_CONCEPT_IRI = "http://visallo.org/workspace#workspace";
    public static final String WORKSPACE_TO_ENTITY_RELATIONSHIP_IRI = "http://visallo.org/workspace#toEntity";
    public static final String WORKSPACE_TO_USER_RELATIONSHIP_IRI = "http://visallo.org/workspace#toUser";
    public static final String WORKSPACE_ID_PREFIX = "WORKSPACE_";
    public static final String OWL_IRI = "http://visallo.org/workspace";
    private String entityHasImageIri;
    private final Graph graph;
    private final VisibilityTranslator visibilityTranslator;
    private final TermMentionRepository termMentionRepository;
    private final OntologyRepository ontologyRepository;
    private final WorkQueueRepository workQueueRepository;

    protected WorkspaceRepository(
            final Graph graph,
            final VisibilityTranslator visibilityTranslator,
            final TermMentionRepository termMentionRepository,
            final OntologyRepository ontologyRepository,
            final WorkQueueRepository workQueueRepository
    ) {
        this.graph = graph;
        this.visibilityTranslator = visibilityTranslator;
        this.termMentionRepository = termMentionRepository;
        this.ontologyRepository = ontologyRepository;
        this.workQueueRepository = workQueueRepository;

        this.entityHasImageIri = ontologyRepository.getRelationshipIRIByIntent("entityHasImage");
        if (this.entityHasImageIri == null) {
            LOGGER.warn("'entityHasImage' intent has not been defined. Please update your ontology.");
        }
    }

    public abstract void delete(Workspace workspace, User user);

    public Workspace findById(String workspaceId, User user) {
        return findById(workspaceId, false, user);
    }

    public abstract Workspace findById(String workspaceId, boolean includeHidden, User user);

    public Iterable<Workspace> findByIds(final Iterable<String> workspaceIds, final User user) {
        return new ConvertingIterable<String, Workspace>(workspaceIds) {
            @Override
            protected Workspace convert(String workspaceId) {
                if (workspaceId == null) {
                    return null;
                }
                try {
                    return findById(workspaceId, user);
                } catch (VisalloAccessDeniedException ex) {
                    return null;
                }
            }
        };
    }

    public abstract Workspace add(String workspaceId, String title, User user);

    public Workspace add(String title, User user) {
        String workspaceId = WORKSPACE_ID_PREFIX + graph.getIdGenerator().nextId();
        return add(workspaceId, title, user);
    }

    public abstract Iterable<Workspace> findAllForUser(User user);

    public abstract void setTitle(Workspace workspace, String title, User user);

    public abstract List<WorkspaceUser> findUsersWithAccess(String workspaceId, User user);

    public abstract List<WorkspaceEntity> findEntities(Workspace workspace, User user);

    public Workspace copy(Workspace workspace, User user) {
        return copyTo(workspace, user, user);
    }

    public Workspace copyTo(Workspace workspace, User destinationUser, User user) {
        Workspace newWorkspace = add("Copy of " + workspace.getDisplayTitle(), destinationUser);
        List<WorkspaceEntity> entities = findEntities(workspace, user);
        Iterable<Update> updates = new ConvertingIterable<WorkspaceEntity, Update>(entities) {
            @Override
            protected Update convert(WorkspaceEntity entity) {
                return new Update(entity.getEntityVertexId(), entity.isVisible(), new GraphPosition(entity.getGraphPositionX(), entity.getGraphPositionY()));
            }
        };
        updateEntitiesOnWorkspace(newWorkspace, updates, destinationUser);
        return newWorkspace;
    }

    public abstract void softDeleteEntitiesFromWorkspace(Workspace workspace, List<String> entityIdsToDelete, User authUser);

    public abstract void deleteUserFromWorkspace(Workspace workspace, String userId, User user);

    public abstract void updateUserOnWorkspace(Workspace workspace, String userId, WorkspaceAccess workspaceAccess, User user);

    public abstract ClientApiWorkspaceDiff getDiff(Workspace workspace, User user, Locale locale, String timeZone);

    public String getCreatorUserId(String workspaceId, User user) {
        for (WorkspaceUser workspaceUser : findUsersWithAccess(workspaceId, user)) {
            if (workspaceUser.isCreator()) {
                return workspaceUser.getUserId();
            }
        }
        return null;
    }

    public abstract boolean hasCommentPermissions(String workspaceId, User user);

    public abstract boolean hasWritePermissions(String workspaceId, User user);

    public abstract boolean hasReadPermissions(String workspaceId, User user);

    public JSONArray toJson(Iterable<Workspace> workspaces, User user, boolean includeVertices) {
        JSONArray resultJson = new JSONArray();
        for (Workspace workspace : workspaces) {
            resultJson.put(toJson(workspace, user, includeVertices));
        }
        return resultJson;
    }

    public JSONObject toJson(Workspace workspace, User user, boolean includeVertices) {
        checkNotNull(workspace, "workspace cannot be null");
        checkNotNull(user, "user cannot be null");

        try {
            JSONObject workspaceJson = new JSONObject();
            workspaceJson.put("workspaceId", workspace.getWorkspaceId());
            workspaceJson.put("title", workspace.getDisplayTitle());

            String creatorUserId = getCreatorUserId(workspace.getWorkspaceId(), user);
            if (creatorUserId != null) {
                workspaceJson.put("createdBy", creatorUserId);
                workspaceJson.put("sharedToUser", !creatorUserId.equals(user.getUserId()));
            }
            workspaceJson.put("editable", hasWritePermissions(workspace.getWorkspaceId(), user));

            JSONArray usersJson = new JSONArray();
            for (WorkspaceUser workspaceUser : findUsersWithAccess(workspace.getWorkspaceId(), user)) {
                String userId = workspaceUser.getUserId();
                JSONObject userJson = new JSONObject();
                userJson.put("userId", userId);
                userJson.put("access", workspaceUser.getWorkspaceAccess().toString().toLowerCase());
                usersJson.put(userJson);
            }
            workspaceJson.put("users", usersJson);

            if (includeVertices) {
                JSONArray verticesJson = new JSONArray();
                for (WorkspaceEntity workspaceEntity : findEntities(workspace, user)) {
                    if (!workspaceEntity.isVisible()) {
                        continue;
                    }

                    JSONObject vertexJson = new JSONObject();
                    vertexJson.put("vertexId", workspaceEntity.getEntityVertexId());

                    Integer graphPositionX = workspaceEntity.getGraphPositionX();
                    Integer graphPositionY = workspaceEntity.getGraphPositionY();
                    if (graphPositionX != null && graphPositionY != null) {
                        JSONObject graphPositionJson = new JSONObject();
                        graphPositionJson.put("x", graphPositionX);
                        graphPositionJson.put("y", graphPositionY);
                        vertexJson.put("graphPosition", graphPositionJson);
                    }

                    verticesJson.put(vertexJson);
                }
                workspaceJson.put("vertices", verticesJson);
            }

            return workspaceJson;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public ClientApiWorkspace toClientApi(Workspace workspace, User user, boolean includeVertices) {
        checkNotNull(workspace, "workspace cannot be null");
        checkNotNull(user, "user cannot be null");

        try {
            ClientApiWorkspace workspaceClientApi = new ClientApiWorkspace();
            workspaceClientApi.setWorkspaceId(workspace.getWorkspaceId());
            workspaceClientApi.setTitle(workspace.getDisplayTitle());

            String creatorUserId = getCreatorUserId(workspace.getWorkspaceId(), user);
            if (creatorUserId != null) {
                workspaceClientApi.setCreatedBy(creatorUserId);
                workspaceClientApi.setSharedToUser(!creatorUserId.equals(user.getUserId()));
            }
            workspaceClientApi.setEditable(hasWritePermissions(workspace.getWorkspaceId(), user));
            workspaceClientApi.setCommentable(hasCommentPermissions(workspace.getWorkspaceId(), user));

            for (WorkspaceUser u : findUsersWithAccess(workspace.getWorkspaceId(), user)) {
                String userId = u.getUserId();
                ClientApiWorkspace.User workspaceUser = new ClientApiWorkspace.User();
                workspaceUser.setUserId(userId);
                workspaceUser.setAccess(u.getWorkspaceAccess());
                workspaceClientApi.addUser(workspaceUser);
            }

            if (includeVertices) {
                for (WorkspaceEntity workspaceEntity : findEntities(workspace, user)) {
                    if (!workspaceEntity.isVisible()) {
                        continue;
                    }

                    ClientApiWorkspace.Vertex v = new ClientApiWorkspace.Vertex();
                    v.setVertexId(workspaceEntity.getEntityVertexId());

                    Integer graphPositionX = workspaceEntity.getGraphPositionX();
                    Integer graphPositionY = workspaceEntity.getGraphPositionY();
                    if (graphPositionX != null && graphPositionY != null) {
                        GraphPosition graphPosition = new GraphPosition(graphPositionX, graphPositionY);
                        v.setGraphPosition(graphPosition);
                        v.setGraphLayoutJson(null);
                    } else {
                        v.setGraphPosition(null);

                        String graphLayoutJson = workspaceEntity.getGraphLayoutJson();
                        if (graphLayoutJson != null) {
                            v.setGraphLayoutJson(graphLayoutJson);
                        } else {
                            v.setGraphLayoutJson(null);
                        }
                    }

                    workspaceClientApi.addVertex(v);
                }
            } else {
                workspaceClientApi.removeVertices();
            }

            return workspaceClientApi;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    protected Graph getGraph() {
        return graph;
    }

    public abstract void updateEntitiesOnWorkspace(Workspace workspace, Iterable<Update> updates, User user);

    public void updateEntityOnWorkspace(Workspace workspace, Update update, User user) {
        List<Update> updates = new ArrayList<>();
        updates.add(update);
        updateEntitiesOnWorkspace(workspace, updates, user);
    }

    public void updateEntityOnWorkspace(Workspace workspace, String vertexId, Boolean visible, GraphPosition graphPosition, User user) {
        updateEntityOnWorkspace(workspace, new Update(vertexId, visible, graphPosition), user);
    }

    public void updateEntityOnWorkspace(String workspaceId, String vertexId, Boolean visible, GraphPosition graphPosition, User user) {
        Workspace workspace = findById(workspaceId, user);
        updateEntityOnWorkspace(workspace, vertexId, visible, graphPosition, user);
    }

    public static String getWorkspaceToEntityEdgeId(String workspaceVertexId, String entityVertexId) {
        return workspaceVertexId + TO_ENTITY_ID_SEPARATOR + entityVertexId;
    }

    public ClientApiWorkspacePublishResponse publish(ClientApiPublishItem[] publishData, String workspaceId, Authorizations authorizations) {
        if (this.entityHasImageIri == null) {
            this.entityHasImageIri = ontologyRepository.getRequiredRelationshipIRIByIntent("entityHasImage");
        }

        ClientApiWorkspacePublishResponse workspacePublishResponse = new ClientApiWorkspacePublishResponse();
        publishVertices(publishData, workspacePublishResponse, workspaceId, authorizations);
        publishEdges(publishData, workspacePublishResponse, workspaceId, authorizations);
        publishProperties(publishData, workspacePublishResponse, workspaceId, authorizations);
        return workspacePublishResponse;
    }

    private void publishVertices(ClientApiPublishItem[] publishData, ClientApiWorkspacePublishResponse workspacePublishResponse, String workspaceId, Authorizations authorizations) {
        LOGGER.debug("BEGIN publishVertices");
        for (ClientApiPublishItem data : publishData) {
            try {
                if (!(data instanceof ClientApiVertexPublishItem)) {
                    continue;
                }
                ClientApiVertexPublishItem vertexPublishItem = (ClientApiVertexPublishItem) data;
                String vertexId = vertexPublishItem.getVertexId();
                checkNotNull(vertexId);
                Vertex vertex = graph.getVertex(vertexId, FetchHint.ALL_INCLUDING_HIDDEN, authorizations);
                checkNotNull(vertex);
                if (SandboxStatusUtil.getSandboxStatus(vertex, workspaceId) == SandboxStatus.PUBLIC && !WorkspaceDiffHelper.isPublicDelete(vertex, authorizations)) {
                    String msg;
                    if (data.getAction() == ClientApiPublishItem.Action.delete) {
                        msg = "Cannot delete public vertex " + vertexId;
                    } else {
                        msg = "Vertex " + vertexId + " is already public";
                    }
                    LOGGER.warn(msg);
                    data.setErrorMessage(msg);
                    workspacePublishResponse.addFailure(data);
                    continue;
                }
                publishVertex(vertex, data.getAction(), authorizations, workspaceId);
            } catch (Exception ex) {
                LOGGER.error("Error publishing %s", data.toString(), ex);
                data.setErrorMessage(ex.getMessage());
                workspacePublishResponse.addFailure(data);
            }
        }
        LOGGER.debug("END publishVertices");
        graph.flush();
    }

    private void publishEdges(ClientApiPublishItem[] publishData, ClientApiWorkspacePublishResponse workspacePublishResponse, String workspaceId, Authorizations authorizations) {
        LOGGER.debug("BEGIN publishEdges");
        for (ClientApiPublishItem data : publishData) {
            try {
                if (!(data instanceof ClientApiRelationshipPublishItem)) {
                    continue;
                }
                ClientApiRelationshipPublishItem relationshipPublishItem = (ClientApiRelationshipPublishItem) data;
                Edge edge = graph.getEdge(relationshipPublishItem.getEdgeId(), FetchHint.ALL_INCLUDING_HIDDEN, authorizations);
                Vertex sourceVertex = edge.getVertex(Direction.OUT, authorizations);
                Vertex destVertex = edge.getVertex(Direction.IN, authorizations);
                if (SandboxStatusUtil.getSandboxStatus(edge, workspaceId) == SandboxStatus.PUBLIC && !WorkspaceDiffHelper.isPublicDelete(edge, authorizations)) {
                    String error_msg;
                    if (data.getAction() == ClientApiPublishItem.Action.delete) {
                        error_msg = "Cannot delete a public edge";
                    } else {
                        error_msg = "Edge is already public";
                    }
                    LOGGER.warn(error_msg);
                    data.setErrorMessage(error_msg);
                    workspacePublishResponse.addFailure(data);
                    continue;
                }

                if (sourceVertex != null && destVertex != null
                        && SandboxStatusUtil.getSandboxStatus(sourceVertex, workspaceId) != SandboxStatus.PUBLIC
                        && SandboxStatusUtil.getSandboxStatus(destVertex, workspaceId) != SandboxStatus.PUBLIC) {
                    String error_msg = "Cannot publish edge, " + edge.getId() + ", because either source and/or dest vertex are not public";
                    LOGGER.warn(error_msg);
                    data.setErrorMessage(error_msg);
                    workspacePublishResponse.addFailure(data);
                    continue;
                }
                publishEdge(edge, sourceVertex, destVertex, data.getAction(), workspaceId, authorizations);
            } catch (Exception ex) {
                LOGGER.error("Error publishing %s", data.toString(), ex);
                data.setErrorMessage(ex.getMessage());
                workspacePublishResponse.addFailure(data);
            }
        }
        LOGGER.debug("END publishEdges");
        graph.flush();
    }

    private void publishProperties(ClientApiPublishItem[] publishData, ClientApiWorkspacePublishResponse workspacePublishResponse, String workspaceId, Authorizations authorizations) {
        LOGGER.debug("BEGIN publishProperties");
        for (ClientApiPublishItem data : publishData) {
            try {
                if (!(data instanceof ClientApiPropertyPublishItem)) {
                    continue;
                }
                ClientApiPropertyPublishItem propertyPublishItem = (ClientApiPropertyPublishItem) data;
                Element element = getPropertyElement(propertyPublishItem, authorizations);

                String propertyKey = propertyPublishItem.getKey();
                String propertyName = propertyPublishItem.getName();

                OntologyProperty ontologyProperty = ontologyRepository.getPropertyByIRI(propertyName);
                checkNotNull(ontologyProperty, "Could not find ontology property: " + propertyName);
                if (!ontologyProperty.getUserVisible() || propertyName.equals(VisalloProperties.ENTITY_IMAGE_VERTEX_ID.getPropertyName())) {
                    continue;
                }

                if (SandboxStatusUtil.getSandboxStatus(element, workspaceId) != SandboxStatus.PUBLIC) {
                    String errorMessage = "Cannot publish a modification of a property on a private element: " + element.getId();
                    VisibilityJson visibilityJson = VisalloProperties.VISIBILITY_JSON.getPropertyValue(element);
                    LOGGER.warn("%s: visibilityJson: %s, workspaceId: %s", errorMessage, visibilityJson.toString(), workspaceId);
                    data.setErrorMessage(errorMessage);
                    workspacePublishResponse.addFailure(data);
                    continue;
                }

                publishProperty(element, data.getAction(), propertyKey, propertyName, workspaceId, authorizations);
            } catch (Exception ex) {
                LOGGER.error("Error publishing %s", data.toString(), ex);
                data.setErrorMessage(ex.getMessage());
                workspacePublishResponse.addFailure(data);
            }
        }
        LOGGER.debug("END publishProperties");
        graph.flush();
    }

    private Element getPropertyElement(ClientApiPropertyPublishItem data, Authorizations authorizations) {
        Element element = null;

        String elementId = data.getEdgeId();
        if (elementId != null) {
            element = graph.getEdge(elementId, FetchHint.ALL_INCLUDING_HIDDEN, authorizations);
        }

        if (element == null) {
            elementId = data.getVertexId();
            if (elementId != null) {
                element = graph.getVertex(elementId, FetchHint.ALL_INCLUDING_HIDDEN, authorizations);
            }
        }

        if (element == null) {
            elementId = data.getElementId();
            checkNotNull(elementId, "elementId, vertexId, or edgeId is required to publish a property");
            element = graph.getVertex(elementId, FetchHint.ALL_INCLUDING_HIDDEN, authorizations);
            if (element == null) {
                element = graph.getEdge(elementId, FetchHint.ALL_INCLUDING_HIDDEN, authorizations);
            }
        }

        checkNotNull(element, "Could not find edge/vertex with id: " + elementId);
        return element;
    }

    private void publishVertex(Vertex vertex, ClientApiPublishItem.Action action, Authorizations authorizations, String workspaceId) throws IOException {
        if (action == ClientApiPublishItem.Action.delete || WorkspaceDiffHelper.isPublicDelete(vertex, authorizations)) {
            graph.softDeleteVertex(vertex, authorizations);
            graph.flush();
            workQueueRepository.broadcastPublishVertexDelete(vertex);
            return;
        }

        // Need to elevate with videoFrame auth to be able to publish VideoFrame properties
        Authorizations authWithVideoFrame = graph.createAuthorizations(authorizations, VideoFrameInfo.VISIBILITY_STRING);
        vertex = graph.getVertex(vertex.getId(), authWithVideoFrame);

        LOGGER.debug("publishing vertex %s(%s)", vertex.getId(), vertex.getVisibility().toString());
        VisibilityJson visibilityJson = VisalloProperties.VISIBILITY_JSON.getPropertyValue(vertex);

        if (!visibilityJson.getWorkspaces().contains(workspaceId)) {
            throw new VisalloException(String.format("vertex with id '%s' is not local to workspace '%s'", vertex.getId(), workspaceId));
        }

        visibilityJson = VisibilityJson.removeFromAllWorkspace(visibilityJson);
        VisalloVisibility visalloVisibility = visibilityTranslator.toVisibility(visibilityJson);

        ExistingElementMutation<Vertex> vertexElementMutation = vertex.prepareMutation();
        vertexElementMutation.alterElementVisibility(visalloVisibility.getVisibility());

        for (Property property : vertex.getProperties()) {
            OntologyProperty ontologyProperty = ontologyRepository.getPropertyByIRI(property.getName());
            checkNotNull(ontologyProperty, "Could not find ontology property " + property.getName());
            if (!ontologyProperty.getUserVisible() && !property.getName().equals(VisalloProperties.ENTITY_IMAGE_VERTEX_ID.getPropertyName())) {
                publishNewProperty(vertexElementMutation, property, workspaceId);
            }
        }

        Metadata metadata = new Metadata();
        // we need to alter the visibility of the json property, otherwise we'll have two json properties, one with the old visibility and one with the new.
        VisalloProperties.VISIBILITY_JSON.alterVisibility(vertexElementMutation, visalloVisibility.getVisibility());
        VisalloProperties.VISIBILITY_JSON_METADATA.setMetadata(metadata, visibilityJson, visibilityTranslator.getDefaultVisibility());
        VisalloProperties.VISIBILITY_JSON.setProperty(vertexElementMutation, visibilityJson, metadata, visalloVisibility.getVisibility());
        vertexElementMutation.save(authorizations);

        for (Vertex termMention : termMentionRepository.findByVertexIdForVertex(vertex.getId(), authorizations)) {
            termMentionRepository.updateVisibility(termMention, visalloVisibility.getVisibility(), authorizations);
        }

        graph.flush();
        workQueueRepository.broadcastPublishVertex(vertex);
    }

    private void publishProperty(Element element, ClientApiPublishItem.Action action, String key, String name, String workspaceId, Authorizations authorizations) {
        if (action == ClientApiPublishItem.Action.delete) {
            element.softDeleteProperty(key, name, authorizations);
            graph.flush();
            workQueueRepository.broadcastPublishPropertyDelete(element, key, name);
            return;
        }
        ExistingElementMutation elementMutation = element.prepareMutation();
        List<Property> properties = IterableUtils.toList(element.getProperties(key, name));
        SandboxStatus[] sandboxStatuses = SandboxStatusUtil.getPropertySandboxStatuses(properties, workspaceId);
        boolean foundProperty = false;
        Property publicProperty = null;
        for (int propertyIndex = 0; propertyIndex < properties.size(); propertyIndex++) {
            Property property = properties.get(propertyIndex);
            Visibility propertyVisibility = property.getVisibility();
            SandboxStatus sandboxStatus = sandboxStatuses[propertyIndex];

            if (WorkspaceDiffHelper.isPublicDelete(property, authorizations)) {
                if (WorkspaceDiffHelper.isPublicPropertyEdited(properties, sandboxStatuses, property)) {
                    publicProperty = property;
                } else {
                    element.softDeleteProperty(key, name, new Visibility(workspaceId), authorizations);
                    graph.flush();
                    workQueueRepository.broadcastPublishPropertyDelete(element, key, name);
                    foundProperty = true;
                }
            } else if (sandboxStatus == SandboxStatus.PUBLIC_CHANGED) {
                element.softDeleteProperty(key, name, propertyVisibility, authorizations);
                if (publicProperty != null) {
                    element.markPropertyVisible(publicProperty, new Visibility(workspaceId), authorizations);
                    element.addPropertyValue(key, name, property.getValue(), publicProperty.getVisibility(), authorizations);
                }
                graph.flush();
                workQueueRepository.broadcastPublishProperty(element, key, name);
                foundProperty = true;
            } else if (publishNewProperty(elementMutation, property, workspaceId)) {
                elementMutation.save(authorizations);
                graph.flush();
                workQueueRepository.broadcastPublishProperty(element, key, name);
                foundProperty = true;
            }

            if (foundProperty) {
                Iterable<Vertex> termMentions;
                if (element instanceof Vertex) {
                    termMentions = termMentionRepository.findByVertexIdAndProperty(element.getId(), property.getKey(), property.getName(), propertyVisibility, authorizations);
                } else {
                    termMentions = termMentionRepository.findByEdgeIdAndProperty((Edge) element, property.getKey(), property.getName(), propertyVisibility, authorizations);
                }
                for (Vertex termMention : termMentions) {
                    termMentionRepository.updateVisibility(termMention, property.getVisibility(), authorizations);
                }
            }
        }
        if (!foundProperty) {
            throw new VisalloException(String.format("no property with key '%s' and name '%s' found on workspace '%s'", key, name, workspaceId));
        }
    }

    private boolean publishNewProperty(ExistingElementMutation elementMutation, Property property, String workspaceId) {
        VisibilityJson visibilityJson = VisalloProperties.VISIBILITY_JSON_METADATA.getMetadataValue(property.getMetadata());
        if (visibilityJson == null) {
            LOGGER.debug("skipping property %s. no visibility json property", property.toString());
            return false;
        }
        if (!visibilityJson.getWorkspaces().contains(workspaceId)) {
            LOGGER.debug("skipping property %s. doesn't have workspace in json or is not hidden from this workspace.", property.toString());
            return false;
        }

        LOGGER.debug("publishing property %s:%s(%s)", property.getKey(), property.getName(), property.getVisibility().toString());
        visibilityJson = VisibilityJson.removeFromAllWorkspace(visibilityJson);
        VisalloVisibility visalloVisibility = visibilityTranslator.toVisibility(visibilityJson);

        elementMutation
                .alterPropertyVisibility(property, visalloVisibility.getVisibility())
                .setPropertyMetadata(property, VisalloProperties.VISIBILITY_JSON.getPropertyName(), visibilityJson.toString(), visibilityTranslator.getDefaultVisibility());

        return true;
    }

    private void publishEdge(
            Edge edge,
            @SuppressWarnings("UnusedParameters") Vertex sourceVertex,
            Vertex destVertex,
            ClientApiPublishItem.Action action,
            String workspaceId,
            Authorizations authorizations
    ) {
        if (action == ClientApiPublishItem.Action.delete || WorkspaceDiffHelper.isPublicDelete(edge, authorizations)) {
            graph.softDeleteEdge(edge, authorizations);
            graph.flush();
            workQueueRepository.broadcastPublishEdgeDelete(edge);
            return;
        }

        LOGGER.debug("publishing edge %s(%s)", edge.getId(), edge.getVisibility().toString());
        VisibilityJson visibilityJson = VisalloProperties.VISIBILITY_JSON.getPropertyValue(edge);
        if (!visibilityJson.getWorkspaces().contains(workspaceId)) {
            throw new VisalloException(String.format("edge with id '%s' is not local to workspace '%s'", edge.getId(), workspaceId));
        }

        if (edge.getLabel().equals(entityHasImageIri)) {
            publishGlyphIconProperty(edge, workspaceId, authorizations);
        }

        edge.softDeleteProperty(ElementMutation.DEFAULT_KEY, VisalloProperties.VISIBILITY_JSON.getPropertyName(), authorizations);
        visibilityJson = VisibilityJson.removeFromAllWorkspace(visibilityJson);
        VisalloVisibility visalloVisibility = visibilityTranslator.toVisibility(visibilityJson);
        ExistingElementMutation<Edge> edgeExistingElementMutation = edge.prepareMutation();
        edgeExistingElementMutation.alterElementVisibility(visalloVisibility.getVisibility());

        for (Property property : edge.getProperties()) {
            boolean userVisible;
            if (VisalloProperties.JUSTIFICATION.getPropertyName().equals(property.getName())) {
                userVisible = false;
            } else {
                OntologyProperty ontologyProperty = ontologyRepository.getPropertyByIRI(property.getName());
                checkNotNull(ontologyProperty, "Could not find ontology property " + property.getName() + " on property " + property);
                userVisible = ontologyProperty.getUserVisible();
            }
            if (!userVisible && !property.getName().equals(VisalloProperties.ENTITY_IMAGE_VERTEX_ID.getPropertyName())) {
                publishNewProperty(edgeExistingElementMutation, property, workspaceId);
            }
        }

        Metadata metadata = new Metadata();
        VisalloProperties.VISIBILITY_JSON_METADATA.setMetadata(metadata, visibilityJson, visibilityTranslator.getDefaultVisibility());
        VisalloProperties.VISIBILITY_JSON.setProperty(edgeExistingElementMutation, visibilityJson, metadata, visalloVisibility.getVisibility());
        edge = edgeExistingElementMutation.save(authorizations);

        for (Vertex termMention : termMentionRepository.findResolvedTo(destVertex.getId(), authorizations)) {
            termMentionRepository.updateVisibility(termMention, visalloVisibility.getVisibility(), authorizations);
        }

        for (Vertex termMention : termMentionRepository.findByEdgeForEdge(edge, authorizations)) {
            termMentionRepository.updateVisibility(termMention, visalloVisibility.getVisibility(), authorizations);
        }

        graph.flush();
        workQueueRepository.broadcastPublishEdge(edge);
    }

    private void publishGlyphIconProperty(Edge hasImageEdge, String workspaceId, Authorizations authorizations) {
        Vertex entityVertex = hasImageEdge.getVertex(Direction.OUT, authorizations);
        checkNotNull(entityVertex, "Could not find has image source vertex " + hasImageEdge.getVertexId(Direction.OUT));
        ExistingElementMutation elementMutation = entityVertex.prepareMutation();
        Iterable<Property> glyphIconProperties = entityVertex.getProperties(VisalloProperties.ENTITY_IMAGE_VERTEX_ID.getPropertyName());
        for (Property glyphIconProperty : glyphIconProperties) {
            if (publishNewProperty(elementMutation, glyphIconProperty, workspaceId)) {
                elementMutation.save(authorizations);
                return;
            }
        }
        LOGGER.warn("new has image edge without a glyph icon property being set on vertex %s", entityVertex.getId());
    }

    public static class Update {
        private final String vertexId;
        private final Boolean visible;
        private final GraphPosition graphPosition;
        private final String graphLayoutJson;

        public Update(String vertexId, Boolean visible, GraphPosition graphPosition) {
            this.vertexId = vertexId;
            this.visible = visible;
            this.graphPosition = graphPosition;
            graphLayoutJson = null;
        }

        public Update(String vertexId, Boolean visible, GraphPosition graphPosition, String graphLayoutJson) {
            this.vertexId = vertexId;
            this.visible = visible;
            this.graphPosition = graphPosition;
            this.graphLayoutJson = graphLayoutJson;
        }

        public String getVertexId() {
            return vertexId;
        }

        public Boolean getVisible() {
            return visible;
        }

        public GraphPosition getGraphPosition() {
            return graphPosition;
        }

        public String getGraphLayoutJson() {
            return graphLayoutJson;
        }
    }
}

