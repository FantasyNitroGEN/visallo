package org.visallo.web.routes.workspace;

import com.google.common.base.Joiner;
import com.google.inject.Inject;
import com.v5analytics.simpleorm.SimpleOrmContext;
import com.v5analytics.webster.HandlerChain;
import org.vertexium.*;
import org.vertexium.mutation.ElementMutation;
import org.vertexium.mutation.ExistingElementMutation;
import org.vertexium.util.IterableUtils;
import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.ingest.video.VideoFrameInfo;
import org.visallo.core.model.audit.Audit;
import org.visallo.core.model.audit.AuditAction;
import org.visallo.core.model.audit.AuditRepository;
import org.visallo.core.model.ontology.OntologyProperty;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.termMention.TermMentionRepository;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.model.workspace.diff.WorkspaceDiffHelper;
import org.visallo.core.security.VisalloVisibility;
import org.visallo.core.security.VisibilityTranslator;
import org.visallo.core.user.User;
import org.visallo.core.util.SandboxStatusUtil;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.BaseRequestHandler;
import org.visallo.web.clientapi.model.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class WorkspacePublish extends BaseRequestHandler {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(WorkspacePublish.class);
    private final TermMentionRepository termMentionRepository;
    private final AuditRepository auditRepository;
    private final UserRepository userRepository;
    private final OntologyRepository ontologyRepository;
    private final WorkQueueRepository workQueueRepository;
    private final Graph graph;
    private final VisibilityTranslator visibilityTranslator;
    private String entityHasImageIri;

    @Inject
    public WorkspacePublish(
            final TermMentionRepository termMentionRepository,
            final AuditRepository auditRepository,
            final UserRepository userRepository,
            final Configuration configuration,
            final Graph graph,
            final VisibilityTranslator visibilityTranslator,
            final OntologyRepository ontologyRepository,
            final WorkspaceRepository workspaceRepository,
            final WorkQueueRepository workQueueRepository) {
        super(userRepository, workspaceRepository, configuration);
        this.termMentionRepository = termMentionRepository;
        this.auditRepository = auditRepository;
        this.graph = graph;
        this.visibilityTranslator = visibilityTranslator;
        this.userRepository = userRepository;
        this.ontologyRepository = ontologyRepository;
        this.workQueueRepository = workQueueRepository;

        this.entityHasImageIri = ontologyRepository.getRelationshipIRIByIntent("entityHasImage");
        if (this.entityHasImageIri == null) {
            LOGGER.warn("'entityHasImage' intent has not been defined. Please update your ontology.");
        }
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        if (this.entityHasImageIri == null) {
            this.entityHasImageIri = ontologyRepository.getRequiredRelationshipIRIByIntent("entityHasImage");
        }

        String publishDataString = getRequiredParameter(request, "publishData");
        ClientApiPublishItem[] publishData = getObjectMapper().readValue(publishDataString, ClientApiPublishItem[].class);
        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);
        String workspaceId = getActiveWorkspaceId(request);

        LOGGER.debug("publishing:\n%s", Joiner.on("\n").join(publishData));
        ClientApiWorkspacePublishResponse workspacePublishResponse = new ClientApiWorkspacePublishResponse();
        publishVertices(publishData, workspacePublishResponse, workspaceId, user, authorizations);
        publishEdges(publishData, workspacePublishResponse, workspaceId, user, authorizations);
        publishProperties(publishData, workspacePublishResponse, workspaceId, user, authorizations);

        LOGGER.debug("publishing results: %s", workspacePublishResponse);
        respondWithClientApiObject(response, workspacePublishResponse);
    }

    private void publishVertices(ClientApiPublishItem[] publishData, ClientApiWorkspacePublishResponse workspacePublishResponse, String workspaceId, User user, Authorizations authorizations) {
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
                publishVertex(vertex, data.getAction(), authorizations, workspaceId, user);
            } catch (Exception ex) {
                LOGGER.error("Error publishing %s", data.toString(), ex);
                data.setErrorMessage(ex.getMessage());
                workspacePublishResponse.addFailure(data);
            }
        }
        LOGGER.debug("END publishVertices");
        graph.flush();
    }

    private void publishEdges(ClientApiPublishItem[] publishData, ClientApiWorkspacePublishResponse workspacePublishResponse, String workspaceId, User user, Authorizations authorizations) {
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
                publishEdge(edge, sourceVertex, destVertex, data.getAction(), workspaceId, user, authorizations);
            } catch (Exception ex) {
                LOGGER.error("Error publishing %s", data.toString(), ex);
                data.setErrorMessage(ex.getMessage());
                workspacePublishResponse.addFailure(data);
            }
        }
        LOGGER.debug("END publishEdges");
        graph.flush();
    }

    private void publishProperties(ClientApiPublishItem[] publishData, ClientApiWorkspacePublishResponse workspacePublishResponse, String workspaceId, User user, Authorizations authorizations) {
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

                publishProperty(element, data.getAction(), propertyKey, propertyName, workspaceId, user, authorizations);
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

    private void publishVertex(Vertex vertex, ClientApiPublishItem.Action action, Authorizations authorizations, String workspaceId, User user) throws IOException {
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
        Visibility originalVertexVisibility = vertex.getVisibility();
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
                publishNewProperty(vertexElementMutation, property, workspaceId, user);
            }
        }

        Metadata metadata = new Metadata();
        // we need to alter the visibility of the json property, otherwise we'll have two json properties, one with the old visibility and one with the new.
        VisalloProperties.VISIBILITY_JSON.alterVisibility(vertexElementMutation, visalloVisibility.getVisibility());
        VisalloProperties.VISIBILITY_JSON_METADATA.setMetadata(metadata, visibilityJson, visibilityTranslator.getDefaultVisibility());
        VisalloProperties.VISIBILITY_JSON.setProperty(vertexElementMutation, visibilityJson, metadata, visalloVisibility.getVisibility());
        vertexElementMutation.save(authorizations);

        auditRepository.auditVertex(AuditAction.PUBLISH, vertex.getId(), "", "", user, visalloVisibility.getVisibility());

        SimpleOrmContext simpleOrmContext = userRepository.getSimpleOrmContext(authorizations, VisalloVisibility.SUPER_USER_VISIBILITY_STRING);
        for (Audit row : auditRepository.findByIdStartsWith(vertex.getId(), simpleOrmContext)) {
            auditRepository.updateColumnVisibility(row, originalVertexVisibility, visalloVisibility.getVisibility().getVisibilityString(), simpleOrmContext);
        }

        for (Vertex termMention : termMentionRepository.findByVertexIdForVertex(vertex.getId(), authorizations)) {
            termMentionRepository.updateVisibility(termMention, visalloVisibility.getVisibility(), authorizations);
        }

        graph.flush();
        workQueueRepository.broadcastPublishVertex(vertex);
    }

    private void publishProperty(Element element, ClientApiPublishItem.Action action, String key, String name, String workspaceId, User user, Authorizations authorizations) {
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
            } else if (publishNewProperty(elementMutation, property, workspaceId, user)) {
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

    private boolean publishNewProperty(ExistingElementMutation elementMutation, Property property, String workspaceId, User user) {
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

        auditRepository.auditEntityProperty(AuditAction.PUBLISH, elementMutation.getElement().getId(), property.getKey(),
                property.getName(), property.getValue(), property.getValue(), "", "", property.getMetadata(), user, visalloVisibility.getVisibility());
        return true;
    }

    private void publishEdge(Edge edge, Vertex sourceVertex, Vertex destVertex, ClientApiPublishItem.Action action, String workspaceId, User user, Authorizations authorizations) {
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
            publishGlyphIconProperty(edge, workspaceId, user, authorizations);
        }

        edge.softDeleteProperty(ElementMutation.DEFAULT_KEY, VisalloProperties.VISIBILITY_JSON.getPropertyName(), authorizations);
        visibilityJson = VisibilityJson.removeFromAllWorkspace(visibilityJson);
        VisalloVisibility visalloVisibility = visibilityTranslator.toVisibility(visibilityJson);
        ExistingElementMutation<Edge> edgeExistingElementMutation = edge.prepareMutation();
        Visibility originalEdgeVisibility = edge.getVisibility();
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
                publishNewProperty(edgeExistingElementMutation, property, workspaceId, user);
            }
        }

        auditRepository.auditEdgeElementMutation(AuditAction.PUBLISH, edgeExistingElementMutation, edge, sourceVertex, destVertex, "", user, visalloVisibility.getVisibility());

        Metadata metadata = new Metadata();
        VisalloProperties.VISIBILITY_JSON_METADATA.setMetadata(metadata, visibilityJson, visibilityTranslator.getDefaultVisibility());
        VisalloProperties.VISIBILITY_JSON.setProperty(edgeExistingElementMutation, visibilityJson, metadata, visalloVisibility.getVisibility());
        edge = edgeExistingElementMutation.save(authorizations);

        auditRepository.auditRelationship(AuditAction.PUBLISH, sourceVertex, destVertex, edge, "", "", user, edge.getVisibility());

        SimpleOrmContext simpleOrmContext = userRepository.getSimpleOrmContext(authorizations, VisalloVisibility.SUPER_USER_VISIBILITY_STRING);
        for (Audit row : auditRepository.findByIdStartsWith(edge.getId(), simpleOrmContext)) {
            auditRepository.updateColumnVisibility(row, originalEdgeVisibility, visalloVisibility.getVisibility().getVisibilityString(), simpleOrmContext);
        }

        for (Vertex termMention : termMentionRepository.findResolvedTo(destVertex.getId(), authorizations)) {
            termMentionRepository.updateVisibility(termMention, visalloVisibility.getVisibility(), authorizations);
        }

        for (Vertex termMention : termMentionRepository.findByEdgeForEdge(edge, authorizations)) {
            termMentionRepository.updateVisibility(termMention, visalloVisibility.getVisibility(), authorizations);
        }

        graph.flush();
        workQueueRepository.broadcastPublishEdge(edge);
    }

    private void publishGlyphIconProperty(Edge hasImageEdge, String workspaceId, User user, Authorizations authorizations) {
        Vertex entityVertex = hasImageEdge.getVertex(Direction.OUT, authorizations);
        checkNotNull(entityVertex, "Could not find has image source vertex " + hasImageEdge.getVertexId(Direction.OUT));
        ExistingElementMutation elementMutation = entityVertex.prepareMutation();
        Iterable<Property> glyphIconProperties = entityVertex.getProperties(VisalloProperties.ENTITY_IMAGE_VERTEX_ID.getPropertyName());
        for (Property glyphIconProperty : glyphIconProperties) {
            if (publishNewProperty(elementMutation, glyphIconProperty, workspaceId, user)) {
                elementMutation.save(authorizations);
                return;
            }
        }
        LOGGER.warn("new has image edge without a glyph icon property being set on vertex %s", entityVertex.getId());
    }
}
