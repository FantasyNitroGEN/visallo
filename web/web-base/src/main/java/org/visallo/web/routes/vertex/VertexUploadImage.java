package org.visallo.web.routes.vertex;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Required;
import org.apache.commons.io.IOUtils;
import org.vertexium.*;
import org.vertexium.mutation.ElementMutation;
import org.vertexium.property.StreamingPropertyValue;
import org.visallo.core.exception.VisalloResourceNotFoundException;
import org.visallo.core.model.ontology.Concept;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.model.workspace.Workspace;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.security.VisibilityTranslator;
import org.visallo.core.user.User;
import org.visallo.core.util.*;
import org.visallo.web.clientapi.model.ClientApiElement;
import org.visallo.web.clientapi.model.VisibilityJson;
import org.visallo.web.parameterProviders.ActiveWorkspaceId;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.vertexium.util.IterableUtils.toList;

public class VertexUploadImage implements ParameterizedHandler {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(VertexUploadImage.class);
    private static final String SOURCE_UPLOAD = "User Upload";
    private static final String PROCESS = VertexUploadImage.class.getName();
    private static final String MULTI_VALUE_KEY = VertexUploadImage.class.getName();

    private final Graph graph;
    private final OntologyRepository ontologyRepository;
    private final WorkQueueRepository workQueueRepository;
    private final VisibilityTranslator visibilityTranslator;
    private final WorkspaceRepository workspaceRepository;
    private final String clockwiseRotationIri;
    private final String yAxisFlippedIri;
    private final String conceptIri;
    private final String entityHasImageIri;

    @Inject
    public VertexUploadImage(
            final Graph graph,
            final OntologyRepository ontologyRepository,
            final WorkQueueRepository workQueueRepository,
            final VisibilityTranslator visibilityTranslator,
            final WorkspaceRepository workspaceRepository
    ) {
        this.graph = graph;
        this.ontologyRepository = ontologyRepository;
        this.workQueueRepository = workQueueRepository;
        this.visibilityTranslator = visibilityTranslator;
        this.workspaceRepository = workspaceRepository;

        this.conceptIri = ontologyRepository.getRequiredConceptIRIByIntent("entityImage");
        this.entityHasImageIri = ontologyRepository.getRequiredRelationshipIRIByIntent("entityHasImage");
        this.yAxisFlippedIri = ontologyRepository.getRequiredPropertyIRIByIntent("media.yAxisFlipped");
        this.clockwiseRotationIri = ontologyRepository.getRequiredPropertyIRIByIntent("media.clockwiseRotation");
    }

    @Handle
    public ClientApiElement handle(
            HttpServletRequest request,
            @Required(name = "graphVertexId") String graphVertexId,
            @ActiveWorkspaceId String workspaceId,
            User user,
            Authorizations authorizations
    ) throws Exception {
        final List<Part> files = Lists.newArrayList(request.getParts());

        Concept concept = ontologyRepository.getConceptByIRI(conceptIri);
        checkNotNull(concept, "Could not find image concept: " + conceptIri);

        if (files.size() != 1) {
            throw new RuntimeException("Wrong number of uploaded files. Expected 1 got " + files.size());
        }

        final Part file = files.get(0);
        Workspace workspace = this.workspaceRepository.findById(workspaceId, user);

        Vertex entityVertex = graph.getVertex(graphVertexId, authorizations);
        if (entityVertex == null) {
            throw new VisalloResourceNotFoundException(String.format("Could not find associated entity vertex for id: %s", graphVertexId));
        }
        ElementMutation<Vertex> entityVertexMutation = entityVertex.prepareMutation();

        VisibilityJson visibilityJson = getVisalloVisibility(entityVertex, workspaceId);
        Visibility visibility = visibilityTranslator.toVisibility(visibilityJson).getVisibility();

        Metadata metadata = new Metadata();
        VisalloProperties.VISIBILITY_JSON_METADATA.setMetadata(metadata, visibilityJson, visibilityTranslator.getDefaultVisibility());

        String title = imageTitle(entityVertex);
        ElementBuilder<Vertex> artifactVertexBuilder = convertToArtifact(file, title, visibilityJson, metadata, user, visibility);
        Vertex artifactVertex = artifactVertexBuilder.save(authorizations);
        this.graph.flush();

        entityVertexMutation.setProperty(VisalloProperties.ENTITY_IMAGE_VERTEX_ID.getPropertyName(), artifactVertex.getId(), metadata, visibility);
        entityVertex = entityVertexMutation.save(authorizations);
        graph.flush();

        List<Edge> existingEdges = toList(entityVertex.getEdges(artifactVertex, Direction.BOTH, entityHasImageIri, authorizations));
        if (existingEdges.size() == 0) {
            EdgeBuilder edgeBuilder = graph.prepareEdge(entityVertex, artifactVertex, entityHasImageIri, visibility);
            Visibility defaultVisibility = visibilityTranslator.getDefaultVisibility();
            VisalloProperties.VISIBILITY_JSON.setProperty(edgeBuilder, visibilityJson, defaultVisibility);
            VisalloProperties.MODIFIED_DATE.setProperty(edgeBuilder, new Date(), defaultVisibility);
            VisalloProperties.MODIFIED_BY.setProperty(edgeBuilder, user.getUserId(), defaultVisibility);
            edgeBuilder.save(authorizations);
        }

        this.workspaceRepository.updateEntityOnWorkspace(workspace, artifactVertex.getId(), user);
        this.workspaceRepository.updateEntityOnWorkspace(workspace, entityVertex.getId(), user);

        graph.flush();

        workQueueRepository.pushElement(artifactVertex, Priority.HIGH);
        workQueueRepository.pushGraphPropertyQueue(
                artifactVertex,
                null,
                VisalloProperties.RAW.getPropertyName(),
                workspaceId,
                visibilityJson.getSource(),
                Priority.HIGH
        );
        workQueueRepository.pushElementImageQueue(
                entityVertex,
                null,
                VisalloProperties.ENTITY_IMAGE_VERTEX_ID.getPropertyName(),
                Priority.HIGH
        );

        return ClientApiConverter.toClientApi(entityVertex, workspaceId, authorizations);
    }

    private String imageTitle(Vertex entityVertex) {
        Property titleProperty = VisalloProperties.TITLE.getFirstProperty(entityVertex);
        Object title;
        if (titleProperty == null) {
            String conceptTypeProperty = VisalloProperties.CONCEPT_TYPE.getPropertyName();
            String vertexConceptType = (String) entityVertex.getProperty(conceptTypeProperty).getValue();
            Concept concept = ontologyRepository.getConceptByIRI(vertexConceptType);
            title = concept.getDisplayName();
        } else {
            title = titleProperty.getValue();
        }
        return String.format("Image of %s", title.toString());
    }

    private VisibilityJson getVisalloVisibility(Vertex entityVertex, String workspaceId) {
        VisibilityJson visibilityJson = VisalloProperties.VISIBILITY_JSON.getPropertyValue(entityVertex);
        if (visibilityJson == null) {
            visibilityJson = new VisibilityJson();
        }
        String visibilitySource = visibilityJson.getSource();
        if (visibilitySource == null) {
            visibilitySource = "";
        }
        return VisibilityJson.updateVisibilitySourceAndAddWorkspaceId(visibilityJson, visibilitySource, workspaceId);
    }

    protected ElementBuilder<Vertex> convertToArtifact(
            final Part file,
            String title,
            VisibilityJson visibilityJson,
            Metadata metadata,
            User user,
            Visibility visibility
    ) throws IOException {
        Visibility defaultVisibility = visibilityTranslator.getDefaultVisibility();
        final InputStream fileInputStream = file.getInputStream();
        final byte[] rawContent = IOUtils.toByteArray(fileInputStream);
        LOGGER.debug("Uploaded file raw content byte length: %d", rawContent.length);

        final String fileName = file.getName();

        final String fileRowKey = RowKeyHelper.buildSHA256KeyString(rawContent);
        LOGGER.debug("Generated row key: %s", fileRowKey);

        StreamingPropertyValue rawValue = new StreamingPropertyValue(new ByteArrayInputStream(rawContent), byte[].class);
        rawValue.searchIndex(false);
        rawValue.store(true);

        ElementBuilder<Vertex> vertexBuilder = graph.prepareVertex(visibility);
        // Note that VisalloProperties.MIME_TYPE is expected to be set by a GraphPropertyWorker.
        VisalloProperties.CONCEPT_TYPE.setProperty(vertexBuilder, conceptIri, defaultVisibility);
        VisalloProperties.VISIBILITY_JSON.setProperty(vertexBuilder, visibilityJson, defaultVisibility);
        VisalloProperties.MODIFIED_BY.setProperty(vertexBuilder, user.getUserId(), defaultVisibility);
        VisalloProperties.MODIFIED_DATE.setProperty(vertexBuilder, new Date(), defaultVisibility);
        VisalloProperties.TITLE.addPropertyValue(vertexBuilder, MULTI_VALUE_KEY, title, metadata, visibility);
        VisalloProperties.FILE_NAME.addPropertyValue(vertexBuilder, MULTI_VALUE_KEY, fileName, metadata, visibility);
        VisalloProperties.RAW.setProperty(vertexBuilder, rawValue, metadata, visibility);
        VisalloProperties.SOURCE.addPropertyValue(vertexBuilder, MULTI_VALUE_KEY, SOURCE_UPLOAD, metadata, visibility);
        VisalloProperties.PROCESS.addPropertyValue(vertexBuilder, MULTI_VALUE_KEY, PROCESS, metadata, visibility);

        ImageTransform imageTransform = ImageTransformExtractor.getImageTransform(rawContent);
        vertexBuilder.setProperty(yAxisFlippedIri, imageTransform.isYAxisFlipNeeded(), metadata, visibility);
        vertexBuilder.setProperty(clockwiseRotationIri, imageTransform.getCWRotationNeeded(), metadata, visibility);

        return vertexBuilder;
    }
}
