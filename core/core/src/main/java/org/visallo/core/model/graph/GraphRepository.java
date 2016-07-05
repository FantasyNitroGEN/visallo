package org.visallo.core.model.graph;

import com.google.inject.Inject;
import org.vertexium.*;
import org.vertexium.mutation.ExistingElementMutation;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.exception.VisalloResourceNotFoundException;
import org.visallo.core.model.PropertyJustificationMetadata;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.termMention.TermMentionFor;
import org.visallo.core.model.termMention.TermMentionRepository;
import org.visallo.core.security.VisalloVisibility;
import org.visallo.core.security.VisibilityTranslator;
import org.visallo.core.user.User;
import org.visallo.core.util.VertexiumMetadataUtil;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.clientapi.model.ClientApiSourceInfo;
import org.visallo.web.clientapi.model.SandboxStatus;
import org.visallo.web.clientapi.model.VisibilityJson;

import java.util.Date;

import static com.google.common.base.Preconditions.checkNotNull;

public class GraphRepository {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(GraphRepository.class);
    public static final String VISALLO_VERSION_KEY = "visallo.version";
    public static final int VISALLO_VERSION = 3;
    public static final double SET_PROPERTY_CONFIDENCE = 0.5;
    private final Graph graph;
    private final VisibilityTranslator visibilityTranslator;
    private final TermMentionRepository termMentionRepository;

    @Inject
    public GraphRepository(
            Graph graph,
            VisibilityTranslator visibilityTranslator,
            TermMentionRepository termMentionRepository
    ) {
        this.graph = graph;
        this.visibilityTranslator = visibilityTranslator;
        this.termMentionRepository = termMentionRepository;
    }

    public void verifyVersion() {
        verifyVersion(VISALLO_VERSION);
    }

    public void verifyVersion(int requiredVersion) {
        Object version = graph.getMetadata(VISALLO_VERSION_KEY);
        if (version == null) {
            writeVersion();
            return;
        }
        if (!(version instanceof Integer)) {
            throw new VisalloException("Invalid " + VISALLO_VERSION_KEY + " found. Expected Integer, found " + version.getClass().getName());
        }
        Integer versionInt = (Integer) version;
        if (versionInt != requiredVersion) {
            throw new VisalloException("Invalid " + VISALLO_VERSION_KEY + " found. Expected " + requiredVersion + ", found " + versionInt);
        }
        LOGGER.info("Visallo graph version verified: %d", versionInt);
    }

    public void writeVersion() {
        writeVersion(VISALLO_VERSION);
    }

    public void writeVersion(int version) {
        graph.setMetadata(VISALLO_VERSION_KEY, version);
        LOGGER.info("Wrote %s: %d", VISALLO_VERSION_KEY, version);
    }

    public <T extends Element> VisibilityAndElementMutation<T> updateElementVisibilitySource(
            Element element,
            SandboxStatus sandboxStatus,
            String visibilitySource,
            String workspaceId,
            Authorizations authorizations
    ) {
        Visibility defaultVisibility = visibilityTranslator.getDefaultVisibility();
        VisibilityJson visibilityJson = VisalloProperties.VISIBILITY_JSON.getPropertyValue(element);
        visibilityJson = sandboxStatus != SandboxStatus.PUBLIC
                ? VisibilityJson.updateVisibilitySourceAndAddWorkspaceId(visibilityJson, visibilitySource, workspaceId)
                : VisibilityJson.updateVisibilitySource(visibilityJson, visibilitySource);

        VisalloVisibility visalloVisibility = visibilityTranslator.toVisibility(visibilityJson);
        Visibility visibility = visalloVisibility.getVisibility();

        ExistingElementMutation<T> m = element.<T>prepareMutation().alterElementVisibility(visibility);

        if (VisalloProperties.VISIBILITY_JSON.hasProperty(element)) {
            Property visibilityJsonProperty = VisalloProperties.VISIBILITY_JSON.getProperty(element);
            m.alterPropertyVisibility(
                    visibilityJsonProperty.getKey(), VisalloProperties.VISIBILITY_JSON.getPropertyName(),
                    defaultVisibility
            );
        }
        VisalloProperties.VISIBILITY_JSON.setProperty(m, visibilityJson, defaultVisibility);

        if (VisalloProperties.CONCEPT_TYPE.hasProperty(element)) {
            Property conceptTypeProperty = VisalloProperties.CONCEPT_TYPE.getProperty(element);
            m.alterPropertyVisibility(
                    conceptTypeProperty.getKey(), VisalloProperties.CONCEPT_TYPE.getPropertyName(), visibility);
            Metadata metadata = conceptTypeProperty.getMetadata();
            VisalloProperties.VISIBILITY_JSON_METADATA.setMetadata(metadata, visibilityJson, defaultVisibility);
        }

        m.save(authorizations);
        return new VisibilityAndElementMutation<>(visalloVisibility, m);
    }

    public <T extends Element> Property updatePropertyVisibilitySource(
            Element element,
            String propertyKey,
            String propertyName,
            String oldVisibilitySource,
            String newVisibilitySource,
            User user,
            Authorizations authorizations
    ) {
        Visibility defaultVisibility = visibilityTranslator.getDefaultVisibility();
        Visibility oldVisibility = visibilityTranslator.toVisibility(oldVisibilitySource).getVisibility();
        Property property = element.getProperty(propertyKey, propertyName, oldVisibility);
        if (property == null) {
            throw new VisalloResourceNotFoundException("Could not find property " + propertyKey + ":" + propertyName + " on element " + element.getId());
        }

        VisibilityJson newVisibilityJson = new VisibilityJson(newVisibilitySource);
        Visibility newVisibility = visibilityTranslator.toVisibility(newVisibilityJson).getVisibility();

        LOGGER.info(
                "%s Altering property visibility %s [%s:%s] from [%s] to [%s]",
                user.getUserId(),
                element.getId(),
                propertyKey,
                propertyName,
                oldVisibility.toString(),
                newVisibility.toString()
        );

        ExistingElementMutation<T> m = element.<T>prepareMutation()
                .alterPropertyVisibility(property, newVisibility);
        VisalloProperties.VISIBILITY_JSON_METADATA.setMetadata(m, property, newVisibilityJson, defaultVisibility);
        T newElement = m.save(authorizations);

        Property newProperty = newElement.getProperty(propertyKey, propertyName, newVisibility);
        checkNotNull(
                newProperty,
                "Could not find altered property " + propertyKey + ":" + propertyName + " on element " + element.getId()
        );

        return newProperty;
    }

    public <T extends Element> VisibilityAndElementMutation<T> setProperty(
            T element,
            String propertyName,
            String propertyKey,
            Object value,
            Metadata metadata,
            String oldVisibilitySource,
            String newVisibilitySource,
            String workspaceId,
            String justificationText,
            ClientApiSourceInfo sourceInfo,
            User user,
            Authorizations authorizations
    ) {
        Visibility defaultVisibility = visibilityTranslator.getDefaultVisibility();

        Visibility oldPropertyVisibility = null;
        if (oldVisibilitySource != null) {
            VisibilityJson oldVisibilityJson = new VisibilityJson();
            oldVisibilityJson.setSource(oldVisibilitySource);
            oldVisibilityJson.addWorkspace(workspaceId);
            oldPropertyVisibility = visibilityTranslator.toVisibility(oldVisibilityJson).getVisibility();
        }

        Property oldProperty = element.getProperty(propertyKey, propertyName, oldPropertyVisibility);
        boolean isUpdate = oldProperty != null;

        Metadata propertyMetadata = isUpdate ? oldProperty.getMetadata() : new Metadata();
        propertyMetadata = VertexiumMetadataUtil.mergeMetadata(propertyMetadata, metadata);

        ExistingElementMutation<T> elementMutation = element.prepareMutation();

        VisibilityJson visibilityJson = VisibilityJson.updateVisibilitySourceAndAddWorkspaceId(
                null,
                newVisibilitySource,
                workspaceId
        );
        VisalloProperties.VISIBILITY_JSON_METADATA.setMetadata(propertyMetadata, visibilityJson, defaultVisibility);
        VisalloProperties.MODIFIED_DATE_METADATA.setMetadata(propertyMetadata, new Date(), defaultVisibility);
        VisalloProperties.MODIFIED_BY_METADATA.setMetadata(propertyMetadata, user.getUserId(), defaultVisibility);
        VisalloProperties.CONFIDENCE_METADATA.setMetadata(propertyMetadata, SET_PROPERTY_CONFIDENCE, defaultVisibility);

        VisalloVisibility visalloVisibility = visibilityTranslator.toVisibility(visibilityJson);
        Visibility propertyVisibility = visalloVisibility.getVisibility();

        if (justificationText != null) {
            PropertyJustificationMetadata propertyJustificationMetadata = new PropertyJustificationMetadata(
                    justificationText);
            termMentionRepository.removeSourceInfoEdge(
                    element,
                    propertyKey,
                    propertyName,
                    visalloVisibility,
                    authorizations
            );
            VisalloProperties.JUSTIFICATION_METADATA.setMetadata(
                    propertyMetadata,
                    propertyJustificationMetadata,
                    defaultVisibility
            );
        } else if (sourceInfo != null) {
            Vertex outVertex = graph.getVertex(sourceInfo.vertexId, authorizations);
            VisalloProperties.JUSTIFICATION.removeMetadata(propertyMetadata);
            termMentionRepository.addSourceInfo(
                    element,
                    element.getId(),
                    TermMentionFor.PROPERTY,
                    propertyKey,
                    propertyName,
                    propertyVisibility,
                    sourceInfo.snippet,
                    sourceInfo.textPropertyKey,
                    sourceInfo.textPropertyName,
                    sourceInfo.startOffset,
                    sourceInfo.endOffset,
                    outVertex,
                    propertyVisibility,
                    authorizations
            );
        }

        Property publicProperty = element.getProperty(propertyKey, propertyName);
        // only public properties in a workspace will be sandboxed (hidden from the workspace)
        if (publicProperty != null && workspaceId != null &&
                SandboxStatus.getFromVisibilityString(publicProperty.getVisibility().getVisibilityString(), workspaceId)
                        == SandboxStatus.PUBLIC) {
            // changing a public property, so hide it from the workspace
            element.markPropertyHidden(publicProperty, new Visibility(workspaceId), authorizations);
        } else if (isUpdate && oldVisibilitySource != null && !oldVisibilitySource.equals(newVisibilitySource)) {
            // changing a existing sandboxed property's visibility
            elementMutation.alterPropertyVisibility(oldProperty, propertyVisibility);
        }

        elementMutation.addPropertyValue(propertyKey, propertyName, value, propertyMetadata, propertyVisibility);

        return new VisibilityAndElementMutation<>(visalloVisibility, elementMutation);
    }

    public Vertex addVertex(
            String vertexId,
            String conceptType,
            String visibilitySource,
            String workspaceId,
            String justificationText,
            ClientApiSourceInfo sourceInfo,
            User user,
            Authorizations authorizations
    ) {
        VisibilityJson visibilityJson = VisibilityJson.updateVisibilitySourceAndAddWorkspaceId(
                null,
                visibilitySource,
                workspaceId
        );
        VisalloVisibility visalloVisibility = visibilityTranslator.toVisibility(visibilityJson);

        VertexBuilder vertexBuilder;
        if (vertexId != null) {
            vertexBuilder = graph.prepareVertex(vertexId, visalloVisibility.getVisibility());
        } else {
            vertexBuilder = graph.prepareVertex(visalloVisibility.getVisibility());
        }
        VisalloProperties.VISIBILITY_JSON.setProperty(vertexBuilder, visibilityJson, visalloVisibility.getVisibility());
        Metadata conceptTypeMetadata = new Metadata();
        Visibility metadataVisibility = visibilityTranslator.getDefaultVisibility();
        VisalloProperties.MODIFIED_DATE_METADATA.setMetadata(conceptTypeMetadata, new Date(), metadataVisibility);
        VisalloProperties.MODIFIED_BY_METADATA.setMetadata(conceptTypeMetadata, user.getUserId(), metadataVisibility);
        VisalloProperties.VISIBILITY_JSON_METADATA.setMetadata(conceptTypeMetadata, visibilityJson, metadataVisibility);
        VisalloProperties.CONCEPT_TYPE.setProperty(
                vertexBuilder,
                conceptType,
                conceptTypeMetadata,
                visalloVisibility.getVisibility()
        );

        boolean justificationAdded = addJustification(
                vertexBuilder,
                justificationText,
                visalloVisibility,
                visibilityJson,
                user
        );

        Vertex vertex = vertexBuilder.save(authorizations);
        graph.flush();

        if (justificationAdded) {
            termMentionRepository.removeSourceInfoEdgeFromVertex(
                    vertex.getId(),
                    vertex.getId(),
                    null,
                    null,
                    visalloVisibility,
                    authorizations
            );
        } else if (sourceInfo != null) {
            VisalloProperties.JUSTIFICATION.removeProperty(vertexBuilder, visalloVisibility.getVisibility());

            Vertex sourceDataVertex = graph.getVertex(sourceInfo.vertexId, authorizations);
            termMentionRepository.addSourceInfoToVertex(
                    vertex,
                    vertex.getId(),
                    TermMentionFor.VERTEX,
                    null,
                    null,
                    null,
                    sourceInfo.snippet,
                    sourceInfo.textPropertyKey,
                    sourceInfo.textPropertyName,
                    sourceInfo.startOffset,
                    sourceInfo.endOffset,
                    sourceDataVertex,
                    visalloVisibility.getVisibility(),
                    authorizations
            );
        }

        return vertex;
    }

    public Edge addEdge(
            String edgeId,
            Vertex outVertex,
            Vertex inVertex,
            String predicateLabel,
            String justificationText,
            ClientApiSourceInfo sourceInfo,
            String visibilitySource,
            String workspaceId,
            User user,
            Authorizations authorizations
    ) {
        Date now = new Date();
        VisibilityJson visibilityJson = VisibilityJson.updateVisibilitySourceAndAddWorkspaceId(
                null,
                visibilitySource,
                workspaceId
        );
        VisalloVisibility visalloVisibility = visibilityTranslator.toVisibility(visibilityJson);
        ElementBuilder<Edge> edgeBuilder;
        if (edgeId == null) {
            edgeBuilder = graph.prepareEdge(outVertex, inVertex, predicateLabel, visalloVisibility.getVisibility());
        } else {
            edgeBuilder = graph.prepareEdge(
                    edgeId,
                    outVertex,
                    inVertex,
                    predicateLabel,
                    visalloVisibility.getVisibility()
            );
        }
        VisalloProperties.VISIBILITY_JSON.setProperty(edgeBuilder, visibilityJson, visalloVisibility.getVisibility());
        VisalloProperties.CONCEPT_TYPE.setProperty(
                edgeBuilder,
                OntologyRepository.TYPE_RELATIONSHIP,
                visalloVisibility.getVisibility()
        );
        VisalloProperties.MODIFIED_DATE.setProperty(edgeBuilder, now, visalloVisibility.getVisibility());
        VisalloProperties.MODIFIED_BY.setProperty(edgeBuilder, user.getUserId(), visalloVisibility.getVisibility());

        boolean justificationAdded = addJustification(
                edgeBuilder,
                justificationText,
                visalloVisibility,
                visibilityJson,
                user
        );

        Edge edge = edgeBuilder.save(authorizations);

        if (justificationAdded) {
            termMentionRepository.removeSourceInfoEdgeFromEdge(edge, null, null, visalloVisibility, authorizations);
        } else if (sourceInfo != null) {
            VisalloProperties.JUSTIFICATION.removeProperty(edgeBuilder, visalloVisibility.getVisibility());

            Vertex sourceDataVertex = graph.getVertex(sourceInfo.vertexId, authorizations);
            termMentionRepository.addSourceInfoEdgeToEdge(
                    edge,
                    edge.getId(),
                    TermMentionFor.EDGE,
                    null,
                    null,
                    null,
                    sourceInfo.snippet,
                    sourceInfo.textPropertyKey,
                    sourceInfo.textPropertyName,
                    sourceInfo.startOffset,
                    sourceInfo.endOffset,
                    sourceDataVertex,
                    visalloVisibility.getVisibility(),
                    authorizations
            );
        }

        return edge;
    }

    private boolean addJustification(
            ElementBuilder elementBuilder,
            String justificationText,
            VisalloVisibility visalloVisibility,
            VisibilityJson visibilityJson,
            User user
    ) {
        Visibility visibility = visalloVisibility.getVisibility();
        if (justificationText != null) {
            Metadata metadata = new Metadata();
            Visibility metadataVisibility = visibilityTranslator.getDefaultVisibility();
            VisalloProperties.MODIFIED_DATE_METADATA.setMetadata(metadata, new Date(), metadataVisibility);
            VisalloProperties.MODIFIED_BY_METADATA.setMetadata(metadata, user.getUserId(), metadataVisibility);
            VisalloProperties.VISIBILITY_JSON_METADATA.setMetadata(metadata, visibilityJson, metadataVisibility);

            PropertyJustificationMetadata value = new PropertyJustificationMetadata(justificationText);
            VisalloProperties.JUSTIFICATION.setProperty(elementBuilder, value, metadata, visibility);
            return true;
        }
        return false;
    }
}
