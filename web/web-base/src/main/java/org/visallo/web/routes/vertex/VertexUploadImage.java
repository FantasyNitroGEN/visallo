package org.visallo.web.routes.vertex;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.v5analytics.webster.HandlerChain;
import org.visallo.core.config.Configuration;
import org.visallo.core.model.audit.AuditAction;
import org.visallo.core.model.audit.AuditRepository;
import org.visallo.core.model.ontology.Concept;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.model.workspace.Workspace;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.security.VisalloVisibility;
import org.visallo.core.security.VisibilityTranslator;
import org.visallo.core.user.User;
import org.visallo.core.util.ClientApiConverter;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.core.util.RowKeyHelper;
import org.visallo.web.BaseRequestHandler;
import org.visallo.web.clientapi.model.VisibilityJson;
import org.apache.commons.io.IOUtils;
import org.vertexium.*;
import org.vertexium.mutation.ElementMutation;
import org.vertexium.property.StreamingPropertyValue;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.vertexium.util.IterableUtils.toList;

public class VertexUploadImage extends BaseRequestHandler {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(VertexUploadImage.class);

    private static final String ATTR_GRAPH_VERTEX_ID = "graphVertexId";
    private static final String DEFAULT_MIME_TYPE = "image";
    private static final String SOURCE_UPLOAD = "User Upload";
    private static final String PROCESS = VertexUploadImage.class.getName();
    private static final String MULTI_VALUE_KEY = VertexUploadImage.class.getName();

    private final Graph graph;
    private final AuditRepository auditRepository;
    private final OntologyRepository ontologyRepository;
    private final WorkQueueRepository workQueueRepository;
    private final VisibilityTranslator visibilityTranslator;
    private final WorkspaceRepository workspaceRepository;
    private String conceptIri;
    private String entityHasImageIri;

    @Inject
    public VertexUploadImage(
            final Graph graph,
            final AuditRepository auditRepository,
            final OntologyRepository ontologyRepository,
            final WorkQueueRepository workQueueRepository,
            final UserRepository userRepository,
            final Configuration configuration,
            final VisibilityTranslator visibilityTranslator,
            final WorkspaceRepository workspaceRepository) {
        super(userRepository, workspaceRepository, configuration);
        this.graph = graph;
        this.auditRepository = auditRepository;
        this.ontologyRepository = ontologyRepository;
        this.workQueueRepository = workQueueRepository;
        this.visibilityTranslator = visibilityTranslator;
        this.workspaceRepository = workspaceRepository;

        this.conceptIri = ontologyRepository.getConceptIRIByIntent("entityImage");
        if (this.conceptIri == null) {
            LOGGER.warn("'entityImage' intent has not been defined. Please update your ontology.");
        }

        this.entityHasImageIri = ontologyRepository.getRelationshipIRIByIntent("entityHasImage");
        if (this.entityHasImageIri == null) {
            LOGGER.warn("'entityHasImage' intent has not been defined. Please update your ontology.");
        }
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        if (this.conceptIri == null) {
            this.conceptIri = ontologyRepository.getRequiredConceptIRIByIntent("entityImage");
        }
        if (this.entityHasImageIri == null) {
            this.entityHasImageIri = ontologyRepository.getRequiredConceptIRIByIntent("entityHasImage");
        }

        final String graphVertexId = getAttributeString(request, ATTR_GRAPH_VERTEX_ID);
        final List<Part> files = Lists.newArrayList(request.getParts());

        Concept concept = ontologyRepository.getConceptByIRI(conceptIri);
        checkNotNull(concept, "Could not find image concept: " + conceptIri);

        if (files.size() != 1) {
            throw new RuntimeException("Wrong number of uploaded files. Expected 1 got " + files.size());
        }

        final User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);
        final Part file = files.get(0);
        String workspaceId = getActiveWorkspaceId(request);
        Workspace workspace = this.workspaceRepository.findById(workspaceId, user);

        Vertex entityVertex = graph.getVertex(graphVertexId, authorizations);
        if (entityVertex == null) {
            LOGGER.warn("Could not find associated entity vertex for id: %s", graphVertexId);
            respondWithNotFound(response);
            return;
        }
        ElementMutation<Vertex> entityVertexMutation = entityVertex.prepareMutation();

        VisibilityJson visibilityJson = getVisalloVisibility(entityVertex, workspaceId);
        VisalloVisibility visalloVisibility = visibilityTranslator.toVisibility(visibilityJson);

        Metadata metadata = new Metadata();
        VisalloProperties.VISIBILITY_JSON_METADATA.setMetadata(metadata, visibilityJson, visibilityTranslator.getDefaultVisibility());

        String title = String.format("Image of %s", VisalloProperties.TITLE.getOnlyPropertyValue(entityVertex));
        ElementBuilder<Vertex> artifactVertexBuilder = convertToArtifact(file, title, visibilityJson, metadata, visalloVisibility, authorizations);
        Vertex artifactVertex = artifactVertexBuilder.save(authorizations);
        this.graph.flush();

        auditRepository.auditVertexElementMutation(AuditAction.UPDATE, artifactVertexBuilder, artifactVertex, "", user, visalloVisibility.getVisibility());

        entityVertexMutation.setProperty(VisalloProperties.ENTITY_IMAGE_VERTEX_ID.getPropertyName(), artifactVertex.getId(), metadata, visalloVisibility.getVisibility());
        auditRepository.auditVertexElementMutation(AuditAction.UPDATE, entityVertexMutation, entityVertex, "", user, visalloVisibility.getVisibility());
        entityVertex = entityVertexMutation.save(authorizations);
        graph.flush();

        List<Edge> existingEdges = toList(entityVertex.getEdges(artifactVertex, Direction.BOTH, entityHasImageIri, authorizations));
        if (existingEdges.size() == 0) {
            EdgeBuilder edgeBuilder = graph.prepareEdge(entityVertex, artifactVertex, entityHasImageIri, visalloVisibility.getVisibility());
            VisalloProperties.VISIBILITY_JSON.setProperty(edgeBuilder, visibilityJson, visalloVisibility.getVisibility());
            Edge edge = edgeBuilder.save(authorizations);
            auditRepository.auditRelationship(AuditAction.CREATE, entityVertex, artifactVertex, edge, "", "", user, visalloVisibility.getVisibility());
        }

        this.workspaceRepository.updateEntityOnWorkspace(workspace, artifactVertex.getId(), null, null, user);
        this.workspaceRepository.updateEntityOnWorkspace(workspace, entityVertex.getId(), null, null, user);

        graph.flush();
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

        respondWithClientApiObject(response, ClientApiConverter.toClientApi(entityVertex, workspaceId, authorizations));
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

    private ElementBuilder<Vertex> convertToArtifact(
            final Part file,
            String title,
            VisibilityJson visibilityJson,
            Metadata metadata,
            VisalloVisibility visalloVisibility,
            Authorizations authorizations
    ) throws IOException {
        final InputStream fileInputStream = file.getInputStream();
        final byte[] rawContent = IOUtils.toByteArray(fileInputStream);
        LOGGER.debug("Uploaded file raw content byte length: %d", rawContent.length);

        final String fileName = file.getName();

        String mimeType = DEFAULT_MIME_TYPE;
        if (file.getContentType() != null) {
            mimeType = file.getContentType();
        }

        final String fileRowKey = RowKeyHelper.buildSHA256KeyString(rawContent);
        LOGGER.debug("Generated row key: %s", fileRowKey);

        StreamingPropertyValue rawValue = new StreamingPropertyValue(new ByteArrayInputStream(rawContent), byte[].class);
        rawValue.searchIndex(false);
        rawValue.store(true);

        ElementBuilder<Vertex> vertexBuilder = graph.prepareVertex(visalloVisibility.getVisibility());
        VisalloProperties.VISIBILITY_JSON.setProperty(vertexBuilder, visibilityJson, visalloVisibility.getVisibility());
        VisalloProperties.TITLE.addPropertyValue(vertexBuilder, MULTI_VALUE_KEY, title, metadata, visalloVisibility.getVisibility());
        VisalloProperties.MODIFIED_DATE.setProperty(vertexBuilder, new Date(), metadata, visalloVisibility.getVisibility());
        VisalloProperties.FILE_NAME.addPropertyValue(vertexBuilder, MULTI_VALUE_KEY, fileName, metadata, visalloVisibility.getVisibility());
        VisalloProperties.MIME_TYPE.addPropertyValue(vertexBuilder, MULTI_VALUE_KEY, mimeType, metadata, visalloVisibility.getVisibility());
        VisalloProperties.RAW.setProperty(vertexBuilder, rawValue, metadata, visalloVisibility.getVisibility());
        VisalloProperties.CONCEPT_TYPE.setProperty(vertexBuilder, conceptIri, metadata, visalloVisibility.getVisibility());
        VisalloProperties.SOURCE.addPropertyValue(vertexBuilder, MULTI_VALUE_KEY, SOURCE_UPLOAD, metadata, visalloVisibility.getVisibility());
        VisalloProperties.PROCESS.addPropertyValue(vertexBuilder, MULTI_VALUE_KEY, PROCESS, metadata, visalloVisibility.getVisibility());
        return vertexBuilder;
    }
}
