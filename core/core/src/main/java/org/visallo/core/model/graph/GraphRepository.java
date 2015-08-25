package org.visallo.core.model.graph;

import com.google.inject.Inject;
import org.vertexium.*;
import org.vertexium.mutation.ExistingElementMutation;
import org.visallo.core.exception.VisalloException;
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

        ExistingElementMutation<T> m = element.<T>prepareMutation().alterElementVisibility(visalloVisibility.getVisibility());
        if (VisalloProperties.VISIBILITY_JSON.getPropertyValue(element) != null) {
            Property visibilityJsonProperty = VisalloProperties.VISIBILITY_JSON.getProperty(element);
            m.alterPropertyVisibility(visibilityJsonProperty.getKey(), VisalloProperties.VISIBILITY_JSON.getPropertyName(), defaultVisibility);
        }
        Metadata metadata = new Metadata();
        metadata.add(VisalloProperties.VISIBILITY_JSON.getPropertyName(), visibilityJson.toString(), defaultVisibility);

        VisalloProperties.VISIBILITY_JSON.setProperty(m, visibilityJson, metadata, defaultVisibility);
        m.save(authorizations);
        return new VisibilityAndElementMutation<>(visalloVisibility, m);
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

        VisibilityJson visibilityJson = VisibilityJson.updateVisibilitySourceAndAddWorkspaceId(null, newVisibilitySource, workspaceId);
        VisalloProperties.VISIBILITY_JSON_METADATA.setMetadata(propertyMetadata, visibilityJson, defaultVisibility);
        VisalloProperties.MODIFIED_DATE_METADATA.setMetadata(propertyMetadata, new Date(), defaultVisibility);
        VisalloProperties.MODIFIED_BY_METADATA.setMetadata(propertyMetadata, user.getUserId(), defaultVisibility);
        VisalloProperties.CONFIDENCE_METADATA.setMetadata(propertyMetadata, SET_PROPERTY_CONFIDENCE, defaultVisibility);

        VisalloVisibility visalloVisibility = visibilityTranslator.toVisibility(visibilityJson);
        Visibility propertyVisibility = visalloVisibility.getVisibility();

        if (justificationText != null) {
            PropertyJustificationMetadata propertyJustificationMetadata = new PropertyJustificationMetadata(justificationText);
            termMentionRepository.removeSourceInfoEdge(element, propertyKey, propertyName, visalloVisibility, authorizations);
            VisalloProperties.JUSTIFICATION_METADATA.setMetadata(propertyMetadata, propertyJustificationMetadata, defaultVisibility);
        } else if (sourceInfo != null) {
            Vertex sourceVertex = graph.getVertex(sourceInfo.vertexId, authorizations);
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
                    sourceInfo.startOffset,
                    sourceInfo.endOffset,
                    sourceVertex,
                    propertyVisibility,
                    authorizations
            );
        }

        Property publicProperty = element.getProperty(propertyKey, propertyName);
        if (publicProperty != null && SandboxStatus.getFromVisibilityString(publicProperty.getVisibility().getVisibilityString(), workspaceId) == SandboxStatus.PUBLIC) {
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
            Authorizations authorizations
    ) {
        VisibilityJson visibilityJson = VisibilityJson.updateVisibilitySourceAndAddWorkspaceId(null, visibilitySource, workspaceId);
        VisalloVisibility visalloVisibility = visibilityTranslator.toVisibility(visibilityJson);

        VertexBuilder vertexBuilder;
        if (vertexId != null) {
            vertexBuilder = graph.prepareVertex(vertexId, visalloVisibility.getVisibility());
        } else {
            vertexBuilder = graph.prepareVertex(visalloVisibility.getVisibility());
        }
        VisalloProperties.VISIBILITY_JSON.setProperty(vertexBuilder, visibilityJson, visalloVisibility.getVisibility());
        Metadata propertyMetadata = new Metadata();
        VisalloProperties.VISIBILITY_JSON_METADATA.setMetadata(propertyMetadata, visibilityJson, visibilityTranslator.getDefaultVisibility());
        VisalloProperties.CONCEPT_TYPE.setProperty(vertexBuilder, conceptType, propertyMetadata, visalloVisibility.getVisibility());

        if (justificationText != null) {
            PropertyJustificationMetadata propertyJustificationMetadata = new PropertyJustificationMetadata(justificationText);
            VisalloProperties.JUSTIFICATION.setProperty(vertexBuilder, propertyJustificationMetadata, visalloVisibility.getVisibility());
        } else if (sourceInfo != null) {
            VisalloProperties.JUSTIFICATION.removeProperty(vertexBuilder, visalloVisibility.getVisibility());
        }

        Vertex vertex = vertexBuilder.save(authorizations);
        graph.flush();

        if (justificationText != null) {
            termMentionRepository.removeSourceInfoEdgeFromVertex(vertex.getId(), vertex.getId(), null, null, visalloVisibility, authorizations);
        } else if (sourceInfo != null) {
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
            Vertex sourceVertex,
            Vertex destVertex,
            String predicateLabel,
            String justificationText,
            ClientApiSourceInfo sourceInfo,
            String visibilitySource,
            String workspaceId,
            User user,
            Authorizations authorizations
    ) {
        Date now = new Date();
        VisibilityJson visibilityJson = VisibilityJson.updateVisibilitySourceAndAddWorkspaceId(null, visibilitySource, workspaceId);
        VisalloVisibility visalloVisibility = visibilityTranslator.toVisibility(visibilityJson);
        ElementBuilder<Edge> edgeBuilder = graph.prepareEdge(sourceVertex, destVertex, predicateLabel, visalloVisibility.getVisibility());
        VisalloProperties.VISIBILITY_JSON.setProperty(edgeBuilder, visibilityJson, visalloVisibility.getVisibility());
        VisalloProperties.CONCEPT_TYPE.setProperty(edgeBuilder, OntologyRepository.TYPE_RELATIONSHIP, visalloVisibility.getVisibility());
        VisalloProperties.MODIFIED_DATE.setProperty(edgeBuilder, now, visalloVisibility.getVisibility());
        VisalloProperties.MODIFIED_BY.setProperty(edgeBuilder, user.getUserId(), visalloVisibility.getVisibility());

        if (justificationText != null) {
            PropertyJustificationMetadata propertyJustificationMetadata = new PropertyJustificationMetadata(justificationText);
            VisalloProperties.JUSTIFICATION.setProperty(edgeBuilder, propertyJustificationMetadata, visalloVisibility.getVisibility());
        } else if (sourceInfo != null) {
            VisalloProperties.JUSTIFICATION.removeProperty(edgeBuilder, visalloVisibility.getVisibility());
        }

        Edge edge = edgeBuilder.save(authorizations);

        if (justificationText != null) {
            termMentionRepository.removeSourceInfoEdgeFromEdge(edge, null, null, visalloVisibility, authorizations);
        } else if (sourceInfo != null) {
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
                    sourceInfo.startOffset,
                    sourceInfo.endOffset,
                    sourceDataVertex,
                    visalloVisibility.getVisibility(),
                    authorizations
            );
        }

        return edge;
    }
}
