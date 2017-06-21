package org.visallo.core.model.workspace;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.vertexium.*;
import org.vertexium.mutation.ElementMutation;
import org.vertexium.mutation.ExistingElementMutation;
import org.vertexium.util.CloseableUtils;
import org.vertexium.util.ConvertingIterable;
import org.vertexium.util.IterableUtils;
import org.vertexium.util.StreamUtils;
import org.visallo.core.bootstrap.InjectHelper;
import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloAccessDeniedException;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.formula.FormulaEvaluator;
import org.visallo.core.ingest.graphProperty.ElementOrPropertyStatus;
import org.visallo.core.ingest.video.VideoFrameInfo;
import org.visallo.core.model.ontology.Concept;
import org.visallo.core.model.ontology.OntologyProperty;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.ontology.Relationship;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.termMention.TermMentionRepository;
import org.visallo.core.model.user.AuthorizationRepository;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.model.workspace.product.Product;
import org.visallo.core.security.VisalloVisibility;
import org.visallo.core.security.VisibilityTranslator;
import org.visallo.core.trace.Traced;
import org.visallo.core.user.User;
import org.visallo.core.util.SandboxStatusUtil;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.clientapi.model.*;

import java.io.InputStream;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.vertexium.util.IterableUtils.toList;
import static org.visallo.core.util.StreamUtil.stream;

public abstract class WorkspaceRepository {
    public static final String TO_ENTITY_ID_SEPARATOR = "_TO_ENTITY_";
    public static final String VISIBILITY_STRING = "workspace";
    public static final VisalloVisibility VISIBILITY = new VisalloVisibility(VISIBILITY_STRING);
    public static final String WORKSPACE_CONCEPT_IRI = WorkspaceProperties.WORKSPACE_CONCEPT_IRI;
    public static final String WORKSPACE_TO_ENTITY_RELATIONSHIP_IRI = WorkspaceProperties.WORKSPACE_TO_ENTITY_RELATIONSHIP_IRI;
    public static final String WORKSPACE_TO_USER_RELATIONSHIP_IRI = WorkspaceProperties.WORKSPACE_TO_USER_RELATIONSHIP_IRI;
    public static final String WORKSPACE_ID_PREFIX = "WORKSPACE_";
    public static final String OWL_IRI = "http://visallo.org/workspace";
    private static final String DEFAULT_WORKSPACE_TITLE = "Default";
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(WorkspaceRepository.class);
    private final Graph graph;
    private final Configuration configuration;
    private final VisibilityTranslator visibilityTranslator;
    private final TermMentionRepository termMentionRepository;
    private final OntologyRepository ontologyRepository;
    private final WorkQueueRepository workQueueRepository;
    private String entityHasImageIri;
    private final AuthorizationRepository authorizationRepository;
    private Collection<WorkspaceListener> workspaceListeners;

    protected WorkspaceRepository(
            Graph graph,
            Configuration configuration,
            VisibilityTranslator visibilityTranslator,
            TermMentionRepository termMentionRepository,
            OntologyRepository ontologyRepository,
            WorkQueueRepository workQueueRepository,
            AuthorizationRepository authorizationRepository
    ) {
        this.graph = graph;
        this.configuration = configuration;
        this.visibilityTranslator = visibilityTranslator;
        this.termMentionRepository = termMentionRepository;
        this.ontologyRepository = ontologyRepository;
        this.workQueueRepository = workQueueRepository;

        this.entityHasImageIri = ontologyRepository.getRelationshipIRIByIntent("entityHasImage", null);
        this.authorizationRepository = authorizationRepository;
        if (this.entityHasImageIri == null) {
            LOGGER.warn("'entityHasImage' intent has not been defined. Please update your ontology.");
        }
    }

    public static String getWorkspaceToEntityEdgeId(String workspaceVertexId, String entityVertexId) {
        return workspaceVertexId + TO_ENTITY_ID_SEPARATOR + entityVertexId;
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

    public Workspace add(User user) {
        return add(null, user);
    }


    public Workspace add(String title, User user) {
        if (title == null) {
            title = DEFAULT_WORKSPACE_TITLE + " - " + user.getDisplayName();
        }
        return add(null, title, user);
    }

    /**
     * Finds all workspaces the given user has access to. Including workspaces shared to that user.
     */
    public abstract Iterable<Workspace> findAllForUser(User user);

    /**
     * Finds all workspaces regardless of access.
     *
     * @param user a user with access to all workspaces such as system user.
     */
    public abstract Iterable<Workspace> findAll(User user);

    public abstract void setTitle(Workspace workspace, String title, User user);

    public abstract List<WorkspaceUser> findUsersWithAccess(String workspaceId, User user);

    public List<WorkspaceEntity> findEntities(Workspace workspace, User user) {
        return findEntities(workspace, false, user);
    }

    public abstract List<WorkspaceEntity> findEntities(Workspace workspace, boolean fetchVertices, User user);

    public Workspace copy(Workspace workspace, User user) {
        return copyTo(workspace, user, user);
    }

    public Workspace copyTo(Workspace workspace, User destinationUser, User user) {
        return add("Copy of " + workspace.getDisplayTitle(), destinationUser);
    }

    public abstract void softDeleteEntitiesFromWorkspace(
            Workspace workspace,
            List<String> entityIdsToDelete,
            User authUser
    );

    public abstract void deleteUserFromWorkspace(Workspace workspace, String userId, User user);

    public abstract UpdateUserOnWorkspaceResult updateUserOnWorkspace(
            Workspace workspace,
            String userId,
            WorkspaceAccess workspaceAccess,
            User user
    );

    public enum UpdateUserOnWorkspaceResult {
        ADD, UPDATE
    }

    public abstract ClientApiWorkspaceDiff getDiff(Workspace workspace, User user, FormulaEvaluator.UserContext userContext);

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

    public JSONArray toJson(Iterable<Workspace> workspaces, User user) {
        JSONArray resultJson = new JSONArray();
        for (Workspace workspace : workspaces) {
            resultJson.put(toJson(workspace, user));
        }
        return resultJson;
    }

    public JSONObject toJson(Workspace workspace, User user) {
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

            return workspaceJson;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public ClientApiWorkspace toClientApi(
            Workspace workspace,
            User user,
            Authorizations authorizations
    ) {
        checkNotNull(workspace, "workspace cannot be null");
        checkNotNull(user, "user cannot be null");

        try {
            ClientApiWorkspace workspaceClientApi = new ClientApiWorkspace();
            workspaceClientApi.setWorkspaceId(workspace.getWorkspaceId());
            workspaceClientApi.setTitle(workspace.getDisplayTitle());

            String creatorUserId = getCreatorUserId(workspace.getWorkspaceId(), user);
            if (creatorUserId == null) {
                workspaceClientApi.setSharedToUser(true);
            } else {
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

            return workspaceClientApi;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    protected Graph getGraph() {
        return graph;
    }

    public abstract void updateEntitiesOnWorkspace(Workspace workspace, Collection<String> vertexIds, User user);

    public void updateEntityOnWorkspace(
            Workspace workspace,
            String vertexId,
            User user
    ) {
        Collection<String> vertexIds = new ArrayList<>();
        vertexIds.add(vertexId);
        updateEntitiesOnWorkspace(workspace, vertexIds, user);
    }

    public void updateEntityOnWorkspace(
            String workspaceId,
            String vertexId,
            User user
    ) {
        Workspace workspace = findById(workspaceId, user);
        updateEntityOnWorkspace(workspace, vertexId, user);
    }

    public ClientApiWorkspacePublishResponse publish(
            ClientApiPublishItem[] publishData,
            User user,
            String workspaceId,
            Authorizations authorizations
    ) {
        if (this.entityHasImageIri == null) {
            this.entityHasImageIri = ontologyRepository.getRequiredRelationshipIRIByIntent("entityHasImage", workspaceId);
        }

        Map<ClientApiPublishItem.Action, List<ClientApiPublishItem>> publishDataByAction = Arrays.stream(publishData)
                .filter(ClientApiPublishItem::validate)
                .collect(Collectors.groupingBy(ClientApiPublishItem::getAction));

        List<ClientApiPublishItem> addUpdateData = publishDataByAction.get(ClientApiPublishItem.Action.ADD_OR_UPDATE);
        if (addUpdateData != null && !addUpdateData.isEmpty()) {
            publishRequiredConcepts(addUpdateData, user, workspaceId, authorizations);
            publishRequiredRelationships(addUpdateData, user, workspaceId, authorizations);
            publishRequiredPropertyTypes(addUpdateData, user, workspaceId);

            // Don't publish any data for which we couldn't also publish the required ontology
            addUpdateData = addUpdateData.stream().filter(data -> data.getErrorMessage() == null).collect(Collectors.toList());

            publishVertices(addUpdateData, workspaceId, authorizations);
            publishEdges(addUpdateData, workspaceId, authorizations);
        }

        publishProperties(publishData, workspaceId, authorizations);

        List<ClientApiPublishItem> deletionData = publishDataByAction.get(ClientApiPublishItem.Action.DELETE);
        if (deletionData != null && !deletionData.isEmpty()) {
            publishEdges(deletionData, workspaceId, authorizations);
            publishVertices(deletionData, workspaceId, authorizations);
        }

        ClientApiWorkspacePublishResponse workspacePublishResponse = new ClientApiWorkspacePublishResponse();
        for (ClientApiPublishItem data : publishData) {
            if (data.getErrorMessage() != null) {
                workspacePublishResponse.addFailure(data);
            }
        }
        return workspacePublishResponse;
    }

    private void publishVertices(List<ClientApiPublishItem> publishData, String workspaceId, Authorizations authorizations) {
        LOGGER.debug("BEGIN publishVertices");

        Map<String, ClientApiVertexPublishItem> vertexIdToPublishData = publishData.stream()
                .filter(data -> data instanceof ClientApiVertexPublishItem)
                .map(data -> ((ClientApiVertexPublishItem) data))
                .collect(Collectors.toMap(ClientApiVertexPublishItem::getVertexId, Function.identity()));

        // Need to elevate with videoFrame auth to be able to load and publish VideoFrame properties
        Authorizations authWithVideoFrame = graph.createAuthorizations(
                authorizations,
                VideoFrameInfo.VISIBILITY_STRING);
        Iterable<Vertex> verticesToPublish = graph.getVertices(
                vertexIdToPublishData.keySet(),
                FetchHint.ALL_INCLUDING_HIDDEN,
                authWithVideoFrame);

        for (Vertex vertex : verticesToPublish) {
            String vertexId = vertex.getId();
            ClientApiPublishItem data = vertexIdToPublishData.get(vertexId);
            vertexIdToPublishData.remove(vertexId); // remove to indicate that it's been handled

            try {
                if (SandboxStatusUtil.getSandboxStatus(vertex, workspaceId) == SandboxStatus.PUBLIC
                        && !WorkspaceDiffHelper.isPublicDelete(vertex, authorizations)) {
                    String msg;
                    if (data.getAction() == ClientApiPublishItem.Action.DELETE) {
                        msg = "Cannot delete public vertex " + vertexId;
                    } else {
                        msg = "Vertex " + vertexId + " is already public";
                    }
                    data.setErrorMessage(msg);
                    continue;
                }
                publishVertex(vertex, data.getAction(), authWithVideoFrame, workspaceId);
            } catch (Exception ex) {
                data.setErrorMessage(ex.getMessage());
            }
        }

        CloseableUtils.closeQuietly(verticesToPublish);

        vertexIdToPublishData.forEach((vertexId, data) ->
                data.setErrorMessage("Unable to load vertex with id " + vertexId));

        LOGGER.debug("END publishVertices");
        graph.flush();
    }

    private void publishRequiredConcepts(
            List<ClientApiPublishItem> publishData,
            User user,
            String workspaceId,
            Authorizations authorizations
    ) {
        Map<String, ClientApiVertexPublishItem> publishDataByVertexId = publishData.stream()
                .filter(data -> data instanceof ClientApiVertexPublishItem)
                .map(data -> ((ClientApiVertexPublishItem) data))
                .collect(Collectors.toMap(ClientApiVertexPublishItem::getVertexId, Function.identity()));

        Iterable<Vertex> verticesToPublish = graph.getVertices(
                publishDataByVertexId.keySet(),
                EnumSet.of(FetchHint.PROPERTIES),
                authorizations);

        Map<String, List<String>> vertexIdsByConcept = StreamUtils.stream(verticesToPublish)
                .collect(Collectors.groupingBy(VisalloProperties.CONCEPT_TYPE::getPropertyValue, Collectors.mapping(Vertex::getId, Collectors.toList())));

        List<String> publishedConceptIds = vertexIdsByConcept.keySet().stream()
                .map(iri -> {
                    Concept concept = ontologyRepository.getConceptByIRI(iri, workspaceId);
                    if (concept == null) {
                        vertexIdsByConcept.get(iri).forEach(vertexId -> {
                            ClientApiVertexPublishItem data = publishDataByVertexId.get(vertexId);
                            data.setErrorMessage("Unable to locate concept with IRI " + iri);
                        });
                    }
                    return concept;
                })
                .filter(concept -> concept != null && concept.getSandboxStatus() != SandboxStatus.PUBLIC )
                .flatMap(concept -> {
                    try {
                        return ontologyRepository.getConceptAndAncestors(concept, workspaceId).stream()
                                .filter(conceptOrAncestor -> conceptOrAncestor.getSandboxStatus() != SandboxStatus.PUBLIC)
                                .map(conceptOrAncestor -> {
                                    try {
                                        ontologyRepository.publishConcept(conceptOrAncestor, user, workspaceId);
                                    } catch (Exception ex) {
                                        LOGGER.error("Error publishing concept %s", concept.getIRI(), ex);
                                        vertexIdsByConcept.get(concept.getIRI()).forEach(vertexId -> {
                                            ClientApiVertexPublishItem data = publishDataByVertexId.get(vertexId);
                                            data.setErrorMessage("Unable to publish concept " + concept.getDisplayName());
                                        });
                                    }
                                    return conceptOrAncestor.getId();
                                });
                    } catch (Exception ex) {
                        LOGGER.error("Error publishing concept %s", concept.getIRI(), ex);
                        vertexIdsByConcept.get(concept.getIRI()).forEach(vertexId -> {
                            ClientApiVertexPublishItem data = publishDataByVertexId.get(vertexId);
                            data.setErrorMessage("Unable to publish concept " + concept.getDisplayName());
                        });
                    }
                    return Stream.empty();
                }).collect(Collectors.toList());

        if (!publishedConceptIds.isEmpty()) {
            ontologyRepository.clearCache();
            workQueueRepository.pushOntologyConceptsChange(null, publishedConceptIds);
        }

        CloseableUtils.closeQuietly(verticesToPublish);
    }

    private void publishRequiredRelationships(
            List<ClientApiPublishItem> publishData,
            User user,
            String workspaceId,
            Authorizations authorizations
    ) {
        Map<String, ClientApiRelationshipPublishItem> publishDataByEdgeId = publishData.stream()
                .filter(data -> data instanceof ClientApiRelationshipPublishItem)
                .map(data -> ((ClientApiRelationshipPublishItem) data))
                .collect(Collectors.toMap(ClientApiRelationshipPublishItem::getEdgeId, Function.identity()));

        Iterable<Edge> edgesToPublish = graph.getEdges(
                publishDataByEdgeId.keySet(),
                EnumSet.of(FetchHint.PROPERTIES),
                authorizations);

        Map<String, List<String>> edgeIdsByLabel = StreamUtils.stream(edgesToPublish)
                .collect(Collectors.groupingBy(Edge::getLabel, Collectors.mapping(Edge::getId, Collectors.toList())));

        List<String> publishedRelationshipIds = edgeIdsByLabel.keySet().stream()
                .map(iri -> {
                    Relationship relationship = ontologyRepository.getRelationshipByIRI(iri, workspaceId);
                    if (relationship == null) {
                        edgeIdsByLabel.get(iri).forEach(edgeId -> {
                            ClientApiRelationshipPublishItem data = publishDataByEdgeId.get(edgeId);
                            data.setErrorMessage("Unable to locate relationship with IRI " + iri);
                        });
                    }
                    return relationship;
                })
                .filter(relationship -> relationship != null && relationship.getSandboxStatus() != SandboxStatus.PUBLIC )
                .flatMap(relationship -> {
                    try {
                        return ontologyRepository.getRelationshipAndAncestors(relationship, workspaceId).stream()
                                .filter(relationshipOrAncestor -> relationshipOrAncestor.getSandboxStatus() != SandboxStatus.PUBLIC)
                                .map(relationshipOrAncestor -> {
                                    try {
                                        ontologyRepository.publishRelationship(relationshipOrAncestor, user, workspaceId);
                                    } catch (Exception ex) {
                                        LOGGER.error("Error publishing relationship %s", relationship.getIRI(), ex);
                                        edgeIdsByLabel.get(relationship.getIRI()).forEach(edgeId -> {
                                            ClientApiRelationshipPublishItem data = publishDataByEdgeId.get(edgeId);
                                            data.setErrorMessage("Unable to publish relationship " + relationship.getDisplayName());
                                        });
                                    }
                                    return relationshipOrAncestor.getId();
                                });
                    } catch (Exception ex) {
                        LOGGER.error("Error publishing relationship %s", relationship.getIRI(), ex);
                        edgeIdsByLabel.get(relationship.getIRI()).forEach(edgeId -> {
                            ClientApiRelationshipPublishItem data = publishDataByEdgeId.get(edgeId);
                            data.setErrorMessage("Unable to publish relationship " + relationship.getDisplayName());
                        });
                    }
                    return Stream.empty();
                }).collect(Collectors.toList());

        if (!publishedRelationshipIds.isEmpty()) {
            ontologyRepository.clearCache();
            workQueueRepository.pushOntologyRelationshipsChange(null, publishedRelationshipIds);
        }

        CloseableUtils.closeQuietly(edgesToPublish);
    }

    private void publishRequiredPropertyTypes(
            List<ClientApiPublishItem> publishData,
            User user,
            String workspaceId
    ) {
        Map<String, List<ClientApiPropertyPublishItem>> publishDataByPropertyIri = publishData.stream()
                .filter(data -> data instanceof ClientApiPropertyPublishItem)
                .map(data -> ((ClientApiPropertyPublishItem) data))
                .filter(data -> data.getAction() == ClientApiPublishItem.Action.ADD_OR_UPDATE)
                .collect(Collectors.groupingBy(ClientApiPropertyPublishItem::getName, Collectors.toList()));

        List<String> publishedPropertyIds = publishDataByPropertyIri.keySet().stream()
                .map(iri -> {
                    OntologyProperty property = ontologyRepository.getPropertyByIRI(iri, workspaceId);
                    if (property == null) {
                        publishDataByPropertyIri.get(iri).forEach(data ->
                            data.setErrorMessage("Unable to locate property with IRI " + iri)
                        );
                    }
                    return property;
                })
                .filter(property -> property != null && property.getSandboxStatus() != SandboxStatus.PUBLIC)
                .map(property -> {
                    try {
                        ontologyRepository.publishProperty(property, user, workspaceId);
                        return null;
                    } catch (Exception ex) {
                        LOGGER.error("Error publishing property %s", property.getIri(), ex);
                        publishDataByPropertyIri.get(property.getIri()).forEach(data ->
                            data.setErrorMessage("Unable to publish relationship " + property.getDisplayName())
                        );
                    }
                    return property.getId();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (!publishedPropertyIds.isEmpty()) {
            ontologyRepository.clearCache();
            workQueueRepository.pushOntologyPropertiesChange(null, publishedPropertyIds);
        }
    }

    private void publishEdges(
            List<ClientApiPublishItem> publishData,
            String workspaceId,
            Authorizations authorizations
    ) {
        LOGGER.debug("BEGIN publishEdges");
        for (ClientApiPublishItem data : publishData) {
            try {
                if (!(data instanceof ClientApiRelationshipPublishItem)) {
                    continue;
                }
                ClientApiRelationshipPublishItem relationshipPublishItem = (ClientApiRelationshipPublishItem) data;
                Edge edge = graph.getEdge(
                        relationshipPublishItem.getEdgeId(),
                        FetchHint.ALL_INCLUDING_HIDDEN,
                        authorizations
                );
                Vertex outVertex = edge.getVertex(Direction.OUT, authorizations);
                Vertex inVertex = edge.getVertex(Direction.IN, authorizations);
                if (SandboxStatusUtil.getSandboxStatus(edge, workspaceId) == SandboxStatus.PUBLIC
                        && !WorkspaceDiffHelper.isPublicDelete(edge, authorizations)) {
                    String error_msg;
                    if (data.getAction() == ClientApiPublishItem.Action.DELETE) {
                        error_msg = "Cannot delete a public edge";
                    } else {
                        error_msg = "Edge is already public";
                    }
                    data.setErrorMessage(error_msg);
                    continue;
                }

                if (outVertex != null && inVertex != null
                        && SandboxStatusUtil.getSandboxStatus(outVertex, workspaceId) != SandboxStatus.PUBLIC
                        && SandboxStatusUtil.getSandboxStatus(inVertex, workspaceId) != SandboxStatus.PUBLIC) {
                    data.setErrorMessage("Cannot publish edge, " + edge.getId() + ", because either source and/or dest vertex are not public");
                    continue;
                }
                publishEdge(edge, outVertex, inVertex, data.getAction(), workspaceId, authorizations);
            } catch (Exception ex) {
                data.setErrorMessage(ex.getMessage());
            }
        }
        LOGGER.debug("END publishEdges");
        graph.flush();
    }

    private void publishProperties(
            ClientApiPublishItem[] publishData,
            String workspaceId,
            Authorizations authorizations
    ) {
        LOGGER.debug("BEGIN publishProperties");
        for (ClientApiPublishItem data : publishData) {
            try {
                if (!(data instanceof ClientApiPropertyPublishItem) || data.getErrorMessage() != null) {
                    continue;
                }
                ClientApiPropertyPublishItem propertyPublishItem = (ClientApiPropertyPublishItem) data;
                Element element = getPropertyElement(propertyPublishItem, authorizations);

                String propertyKey = propertyPublishItem.getKey();
                String propertyName = propertyPublishItem.getName();

                OntologyProperty ontologyProperty = ontologyRepository.getPropertyByIRI(propertyName, workspaceId);
                checkNotNull(ontologyProperty, "Could not find ontology property: " + propertyName);
                if (!ontologyProperty.getUserVisible() || propertyName.equals(VisalloProperties.ENTITY_IMAGE_VERTEX_ID.getPropertyName())) {
                    continue;
                }

                if (SandboxStatusUtil.getSandboxStatus(element, workspaceId) != SandboxStatus.PUBLIC) {
                    data.setErrorMessage("Cannot publish a modification of a property on a private element: " + element.getId());
                    continue;
                }

                publishProperty(element, data.getAction(), propertyKey, propertyName, workspaceId, authorizations);
            } catch (Exception ex) {
                data.setErrorMessage(ex.getMessage());
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

    private void publishVertex(
            Vertex vertex,
            ClientApiPublishItem.Action action,
            Authorizations authorizations,
            String workspaceId
    ) {
        if (action == ClientApiPublishItem.Action.DELETE || WorkspaceDiffHelper.isPublicDelete(vertex, authorizations)) {
            long beforeDeletionTimestamp = System.currentTimeMillis() - 1;
            graph.softDeleteVertex(vertex, authorizations);
            graph.flush();
            workQueueRepository.pushPublishedVertexDeletion(vertex, beforeDeletionTimestamp, Priority.HIGH);
            return;
        }

        LOGGER.debug("publishing vertex %s(%s)", vertex.getId(), vertex.getVisibility().toString());
        VisibilityJson visibilityJson = VisalloProperties.VISIBILITY_JSON.getPropertyValue(vertex);

        if (visibilityJson == null || !visibilityJson.getWorkspaces().contains(workspaceId)) {
            throw new VisalloException(String.format(
                    "vertex with id '%s' is not local to workspace '%s'",
                    vertex.getId(),
                    workspaceId
            ));
        }

        visibilityJson = VisibilityJson.removeFromAllWorkspace(visibilityJson);
        VisalloVisibility visalloVisibility = visibilityTranslator.toVisibility(visibilityJson);

        ExistingElementMutation<Vertex> vertexElementMutation = vertex.prepareMutation();
        vertexElementMutation.alterElementVisibility(visalloVisibility.getVisibility());

        for (Property property : vertex.getProperties()) {
            OntologyProperty ontologyProperty = ontologyRepository.getPropertyByIRI(property.getName(), workspaceId);
            checkNotNull(ontologyProperty, "Could not find ontology property " + property.getName());
            boolean userVisible = ontologyProperty.getUserVisible();
            if (shouldAutoPublishElementProperty(property, userVisible)) {
                publishNewProperty(vertexElementMutation, property, workspaceId);
            }
        }

        VisalloProperties.VISIBILITY_JSON.setProperty(
                vertexElementMutation,
                visibilityJson,
                visibilityTranslator.getDefaultVisibility()
        );
        vertexElementMutation.save(authorizations);

        for (Vertex termMention : termMentionRepository.findByVertexId(vertex.getId(), authorizations)) {
            termMentionRepository.updateVisibility(termMention, visalloVisibility.getVisibility(), authorizations);
        }

        graph.flush();
        workQueueRepository.broadcastPublishVertex(vertex);
    }

    private void publishProperty(
            Element element,
            ClientApiPublishItem.Action action,
            String key,
            String name,
            String workspaceId,
            Authorizations authorizations
    ) {
        long beforeActionTimestamp = System.currentTimeMillis() - 1;
        if (action == ClientApiPublishItem.Action.DELETE) {
            element.softDeleteProperty(key, name, authorizations);
            graph.flush();
            workQueueRepository.pushPublishedPropertyDeletion(element, key, name, beforeActionTimestamp, Priority.HIGH);
            return;
        }
        ExistingElementMutation elementMutation = element.prepareMutation();
        List<Property> properties = IterableUtils.toList(element.getProperties(key, name));
        SandboxStatus[] sandboxStatuses = SandboxStatusUtil.getPropertySandboxStatuses(properties, workspaceId);
        boolean foundProperty = false;
        Property publicProperty = null;

        for (Property property : properties) {
            if (WorkspaceDiffHelper.isPublicDelete(property, authorizations) &&
                    WorkspaceDiffHelper.isPublicPropertyEdited(properties, sandboxStatuses, property)) {
                publicProperty = property;
                break;
            }
        }

        for (int i = 0; i < properties.size(); i++) {
            Property property = properties.get(i);
            Visibility propertyVisibility = property.getVisibility();
            SandboxStatus sandboxStatus = sandboxStatuses[i];

            if (WorkspaceDiffHelper.isPublicDelete(property, authorizations)) {
                if (publicProperty == null) {
                    element.softDeleteProperty(key, name, new Visibility(workspaceId), authorizations);
                    graph.flush();
                    workQueueRepository.pushPublishedPropertyDeletion(
                            element,
                            key,
                            name,
                            beforeActionTimestamp,
                            Priority.HIGH
                    );
                    foundProperty = true;
                }
            } else if (sandboxStatus == SandboxStatus.PUBLIC_CHANGED) {
                element.softDeleteProperty(key, name, propertyVisibility, authorizations);
                workQueueRepository.pushPublishedPropertyDeletion(
                        element,
                        key,
                        name,
                        beforeActionTimestamp,
                        Priority.HIGH
                );
                if (publicProperty != null) {
                    element.markPropertyVisible(publicProperty, new Visibility(workspaceId), authorizations);

                    Visibility publicVisibility = publicProperty.getVisibility();

                    Metadata metadata = property.getMetadata();
                    VisibilityJson visibilityJson = VisalloProperties.VISIBILITY_JSON_METADATA.getMetadataValue(metadata);
                    VisibilityJson.removeFromWorkspace(visibilityJson, workspaceId);
                    VisalloProperties.VISIBILITY_JSON_METADATA.setMetadata(
                            metadata,
                            visibilityJson,
                            visibilityTranslator.getDefaultVisibility()
                    );
                    Visibility newVisibility = visibilityTranslator.toVisibility(visibilityJson).getVisibility();

                    if (!publicVisibility.equals(newVisibility)) {
                        element.softDeleteProperty(key, name, publicVisibility, authorizations);
                    } else {
                        newVisibility = publicVisibility;
                    }
                    element.addPropertyValue(key, name, property.getValue(), metadata, newVisibility, authorizations);
                    workQueueRepository.pushGraphPropertyQueue(
                            element,
                            key,
                            name,
                            ElementOrPropertyStatus.UNHIDDEN,
                            beforeActionTimestamp,
                            Priority.HIGH
                    );
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
                    termMentions = termMentionRepository.findByVertexIdAndProperty(
                            element.getId(),
                            property.getKey(),
                            property.getName(),
                            propertyVisibility,
                            authorizations
                    );
                } else {
                    termMentions = Collections.emptyList();
                }

                for (Vertex termMention : termMentions) {
                    termMentionRepository.updateVisibility(termMention, property.getVisibility(), authorizations);
                }
            }
        }
        if (!foundProperty) {
            throw new VisalloException(String.format(
                    "no property with key '%s' and name '%s' found on workspace '%s'",
                    key,
                    name,
                    workspaceId
            ));
        }
    }

    private boolean publishNewProperty(ExistingElementMutation elementMutation, Property property, String workspaceId) {
        VisibilityJson visibilityJson = VisalloProperties.VISIBILITY_JSON_METADATA.getMetadataValue(property.getMetadata());
        if (visibilityJson == null) {
            LOGGER.warn("skipping property %s. no visibility json property", property.toString());
            return false;
        }
        if (!visibilityJson.getWorkspaces().contains(workspaceId)) {
            LOGGER.warn(
                    "skipping property %s. doesn't have workspace in json or is not hidden from this workspace.",
                    property.toString()
            );
            return false;
        }

        LOGGER.debug(
                "publishing property %s:%s(%s)",
                property.getKey(),
                property.getName(),
                property.getVisibility().toString()
        );
        visibilityJson = VisibilityJson.removeFromAllWorkspace(visibilityJson);
        VisalloVisibility visalloVisibility = visibilityTranslator.toVisibility(visibilityJson);

        elementMutation
                .alterPropertyVisibility(property, visalloVisibility.getVisibility())
                .setPropertyMetadata(
                        property,
                        VisalloProperties.VISIBILITY_JSON.getPropertyName(),
                        visibilityJson.toString(),
                        visibilityTranslator.getDefaultVisibility()
                );

        return true;
    }

    private void publishEdge(
            Edge edge,
            @SuppressWarnings("UnusedParameters") Vertex outVertex,
            Vertex inVertex,
            ClientApiPublishItem.Action action,
            String workspaceId,
            Authorizations authorizations
    ) {
        if (action == ClientApiPublishItem.Action.DELETE || WorkspaceDiffHelper.isPublicDelete(edge, authorizations)) {
            long beforeDeletionTimestamp = System.currentTimeMillis() - 1;
            graph.softDeleteEdge(edge, authorizations);
            graph.flush();
            workQueueRepository.pushPublishedEdgeDeletion(edge, beforeDeletionTimestamp, Priority.HIGH);
            return;
        }

        LOGGER.debug("publishing edge %s(%s)", edge.getId(), edge.getVisibility().toString());
        VisibilityJson visibilityJson = VisalloProperties.VISIBILITY_JSON.getPropertyValue(edge);
        if (visibilityJson == null || !visibilityJson.getWorkspaces().contains(workspaceId)) {
            throw new VisalloException(String.format(
                    "edge with id '%s' is not local to workspace '%s'",
                    edge.getId(),
                    workspaceId
            ));
        }

        if (edge.getLabel().equals(entityHasImageIri)) {
            publishGlyphIconProperties(edge, workspaceId, authorizations);
        }

        edge.softDeleteProperty(
                ElementMutation.DEFAULT_KEY,
                VisalloProperties.VISIBILITY_JSON.getPropertyName(),
                authorizations
        );
        visibilityJson = VisibilityJson.removeFromAllWorkspace(visibilityJson);
        VisalloVisibility visalloVisibility = visibilityTranslator.toVisibility(visibilityJson);
        ExistingElementMutation<Edge> edgeExistingElementMutation = edge.prepareMutation();
        edgeExistingElementMutation.alterElementVisibility(visalloVisibility.getVisibility());

        for (Property property : edge.getProperties()) {
            boolean userVisible;
            if (VisalloProperties.JUSTIFICATION.getPropertyName().equals(property.getName())) {
                userVisible = false;
            } else {
                OntologyProperty ontologyProperty = ontologyRepository.getPropertyByIRI(property.getName(), workspaceId);
                checkNotNull(
                        ontologyProperty,
                        "Could not find ontology property " + property.getName() + " on property " + property
                );
                userVisible = ontologyProperty.getUserVisible();
            }
            if (shouldAutoPublishElementProperty(property, userVisible)) {
                publishNewProperty(edgeExistingElementMutation, property, workspaceId);
            }
        }

        Metadata metadata = new Metadata();
        VisalloProperties.VISIBILITY_JSON_METADATA.setMetadata(
                metadata,
                visibilityJson,
                visibilityTranslator.getDefaultVisibility()
        );
        VisalloProperties.VISIBILITY_JSON.setProperty(
                edgeExistingElementMutation,
                visibilityJson,
                visibilityTranslator.getDefaultVisibility()
        );
        edge = edgeExistingElementMutation.save(authorizations);

        for (Vertex termMention : termMentionRepository.findResolvedTo(inVertex.getId(), authorizations)) {
            termMentionRepository.updateVisibility(termMention, visalloVisibility.getVisibility(), authorizations);
        }

        for (Vertex termMention : termMentionRepository.findByEdgeForEdge(edge, authorizations)) {
            termMentionRepository.updateVisibility(termMention, visalloVisibility.getVisibility(), authorizations);
        }

        graph.flush();
        workQueueRepository.broadcastPublishEdge(edge);
    }

    private boolean shouldAutoPublishElementProperty(Property property, boolean userVisible) {
        if (userVisible) {
            return false;
        }

        String propertyName = property.getName();
        if (propertyName.equals(VisalloProperties.ENTITY_IMAGE_VERTEX_ID.getPropertyName())) {
            return false;
        }

        if (propertyName.equals(VisalloProperties.CONCEPT_TYPE.getPropertyName())
                || propertyName.equals(VisalloProperties.MODIFIED_BY.getPropertyName())
                || propertyName.equals(VisalloProperties.MODIFIED_DATE.getPropertyName())
                || propertyName.equals(VisalloProperties.VISIBILITY_JSON.getPropertyName())) {
            VisibilityJson visibilityJson = VisalloProperties.VISIBILITY_JSON_METADATA.getMetadataValue(property.getMetadata());
            if (visibilityJson != null) {
                LOGGER.warn("Property %s should not have visibility JSON metadata set", property.toString());
                return true;
            }

            if (!property.getVisibility().equals(visibilityTranslator.getDefaultVisibility())) {
                LOGGER.warn("Property %s should have default visibility", property.toString());
                return true;
            }

            return false;
        }

        return true;
    }

    private void publishGlyphIconProperties(Edge hasImageEdge, String workspaceId, Authorizations authorizations) {
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

    @Deprecated
    public List<String> findEntityVertexIds(Workspace workspace, User user) {
        List<WorkspaceEntity> workspaceEntities = findEntities(workspace, user);
        return toList(new ConvertingIterable<WorkspaceEntity, String>(workspaceEntities) {
            @Override
            protected String convert(WorkspaceEntity workspaceEntity) {
                return workspaceEntity.getEntityVertexId();
            }
        });
    }

    @Traced
    protected Iterable<Edge> findModifiedEdges(
            final Workspace workspace,
            List<WorkspaceEntity> workspaceEntities,
            boolean includeHidden,
            User user
    ) {

        Authorizations systemAuthorizations = getAuthorizationRepository().getGraphAuthorizations(
                user,
                VisalloVisibility.SUPER_USER_VISIBILITY_STRING,
                workspace.getWorkspaceId()
        );
        Iterable<Vertex> vertices = stream(WorkspaceEntity.toVertices(workspaceEntities, getGraph(), systemAuthorizations))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        Authorizations authorizations = getAuthorizationRepository().getGraphAuthorizations(
                user,
                VISIBILITY_STRING,
                workspace.getWorkspaceId()
        );
        Iterable<String> edgeIds = getGraph().findRelatedEdgeIdsForVertices(vertices, authorizations);

        return getGraph().getEdges(
                edgeIds,
                includeHidden ? FetchHint.ALL_INCLUDING_HIDDEN : FetchHint.ALL,
                authorizations
        );
    }

    public abstract Dashboard findDashboardById(String workspaceId, String dashboardId, User user);

    public abstract void deleteDashboard(String workspaceId, String dashboardId, User user);

    public abstract Collection<Dashboard> findAllDashboardsForWorkspace(String workspaceId, User user);

    public abstract DashboardItem findDashboardItemById(String workspaceId, String dashboardItemId, User user);

    public abstract void deleteDashboardItem(String workspaceId, String dashboardItemId, User user);

    public abstract Collection<Product> findAllProductsForWorkspace(String workspaceId, User user);

    public abstract Product addOrUpdateProduct(String workspaceId, String productId, String title, String kind, JSONObject params, User user);

    public abstract Product updateProductPreview(String workspaceId, String productId, String previewDataUrl, User user);

    public abstract void deleteProduct(String workspaceId, String productId, User user);

    public abstract Product findProductById(String workspaceId, String productId, JSONObject params, boolean includeExtended, User user);

    public abstract InputStream getProductPreviewById(String workspaceId, String productId, User user);

    protected VisibilityTranslator getVisibilityTranslator() {
        return visibilityTranslator;
    }

    protected TermMentionRepository getTermMentionRepository() {
        return termMentionRepository;
    }

    protected OntologyRepository getOntologyRepository() {
        return ontologyRepository;
    }

    protected WorkQueueRepository getWorkQueueRepository() {
        return workQueueRepository;
    }

    public abstract String addOrUpdateDashboardItem(
            String workspaceId,
            String dashboardId,
            String dashboardItemId,
            String title,
            String configuration,
            String extensionId,
            User user
    );

    public abstract String addOrUpdateDashboard(String workspaceId, String dashboardId, String title, User user);

    protected AuthorizationRepository getAuthorizationRepository() {
        return authorizationRepository;
    }

    protected void fireWorkspaceBeforeDelete(Workspace workspace, User user) {
        for (WorkspaceListener workspaceListener : getWorkspaceListeners()) {
            workspaceListener.workspaceBeforeDelete(workspace, user);
        }
    }

    protected void fireWorkspaceProductUpdated(Product product, JSONObject params, User user) {
        for (WorkspaceListener workspaceListener : getWorkspaceListeners()) {
            workspaceListener.workspaceProductUpdated(product, params, user);
        }
    }

    protected void fireWorkspaceAddProduct(Product product, User user) {
        for (WorkspaceListener workspaceListener : getWorkspaceListeners()) {
            workspaceListener.workspaceAddProduct(product, user);
        }
    }

    protected void fireWorkspaceUpdateEntities(Workspace workspace, Collection<String> vertexIds, User user) {
        for (WorkspaceListener workspaceListener : getWorkspaceListeners()) {
            workspaceListener.workspaceUpdateEntities(workspace, vertexIds, user);
        }
    }

    protected void fireWorkspaceUpdateUser(Workspace workspace, String userId, WorkspaceAccess workspaceAccess, User user) {
        for (WorkspaceListener workspaceListener : getWorkspaceListeners()) {
            workspaceListener.workspaceUpdateUser(workspace, userId, workspaceAccess, user);
        }
    }

    protected void fireWorkspaceAdded(Workspace workspace, User user) {
        for (WorkspaceListener workspaceListener : getWorkspaceListeners()) {
            workspaceListener.workspaceAdded(workspace, user);
        }
    }

    protected void fireWorkspaceBeforeDeleteProduct(String workspaceId, String productId, User user) {
        for (WorkspaceListener workspaceListener : getWorkspaceListeners()) {
            workspaceListener.workspaceBeforeDeleteProduct(workspaceId, productId, user);
        }
    }

    protected void fireWorkspaceDeleteUser(Workspace workspace, String userId, User user) {
        for (WorkspaceListener workspaceListener : getWorkspaceListeners()) {
            workspaceListener.workspaceDeleteUser(workspace, userId, user);
        }
    }

    protected Collection<WorkspaceListener> getWorkspaceListeners() {
        if (workspaceListeners == null) {
            workspaceListeners = InjectHelper.getInjectedServices(WorkspaceListener.class, configuration);
        }
        return workspaceListeners;
    }
}

