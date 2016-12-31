package org.visallo.web.structuredingest.core.util;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import org.vertexium.*;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.graph.GraphRepository;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.properties.types.PropertyMetadata;
import org.visallo.core.model.properties.types.SingleValueVisalloProperty;
import org.visallo.core.model.user.PrivilegeRepository;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.model.workspace.Workspace;
import org.visallo.core.model.workspace.WorkspaceHelper;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.security.VisalloVisibility;
import org.visallo.core.security.VisibilityTranslator;
import org.visallo.core.user.User;
import org.visallo.core.util.SandboxStatusUtil;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.clientapi.model.Privilege;
import org.visallo.web.clientapi.model.SandboxStatus;
import org.visallo.web.clientapi.model.VisibilityJson;
import org.visallo.web.structuredingest.core.StructuredIngestOntology;
import org.visallo.web.structuredingest.core.model.ClientApiIngestPreview;
import org.visallo.web.structuredingest.core.model.ClientApiParseErrors;
import org.visallo.web.structuredingest.core.util.mapping.EdgeMapping;
import org.visallo.web.structuredingest.core.util.mapping.ParseMapping;
import org.visallo.web.structuredingest.core.util.mapping.PropertyMapping;
import org.visallo.web.structuredingest.core.util.mapping.VertexMapping;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.visallo.core.model.properties.VisalloProperties.VISIBILITY_JSON_METADATA;

public class GraphBuilderParserHandler extends BaseStructuredFileParserHandler {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(GraphBuilderParserHandler.class);
    public static final Long MAX_DRY_RUN_ROWS = 50000L;
    private static final String MULTI_KEY = "SFIMPORT";
    private static final String SKIPPED_VERTEX_ID = "SKIPPED_VERTEX";

    private final Graph graph;
    private final User user;
    private final VisibilityTranslator visibilityTranslator;
    private final PrivilegeRepository privilegeRepository;
    private final Authorizations authorizations;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceHelper workspaceHelper;
    private final Workspace workspace;
    private final Vertex structuredFileVertex;
    private final PropertyMetadata propertyMetadata;
    private final Visibility visibility;
    private final ParseMapping parseMapping;
    private final ProgressReporter progressReporter;
    private final Authorizations visalloUserAuths;

    private VisibilityJson visibilityJson;
    private boolean publish;
    private int sheetNumber = -1;
    public int maxParseErrors = 10;
    public boolean dryRun = true;
    public ClientApiParseErrors parseErrors = new ClientApiParseErrors();
    public ClientApiIngestPreview clientApiIngestPreview;
    public List<String> createdVertexIds;
    public List<String> createdEdgeIds;

    public GraphBuilderParserHandler(
            Graph graph,
            User user,
            VisibilityTranslator visibilityTranslator,
            PrivilegeRepository privilegeRepository,
            Authorizations authorizations,
            WorkspaceRepository workspaceRepository,
            WorkspaceHelper workspaceHelper,
            String workspaceId,
            boolean publish,
            Vertex structuredFileVertex,
            ParseMapping parseMapping,
            ProgressReporter progressReporter
    ) {
        this.graph = graph;
        this.user = user;
        this.visibilityTranslator = visibilityTranslator;
        this.privilegeRepository = privilegeRepository;
        this.authorizations = authorizations;
        this.workspaceRepository = workspaceRepository;
        this.workspaceHelper = workspaceHelper;
        this.workspace = workspaceRepository.findById(workspaceId, user);
        this.structuredFileVertex = structuredFileVertex;
        this.parseMapping = parseMapping;
        this.progressReporter = progressReporter;
        this.publish = publish;

        visalloUserAuths = graph.createAuthorizations(VisalloVisibility.SUPER_USER_VISIBILITY_STRING);

        if (workspace == null) {
            throw new VisalloException("Unable to find vertex with ID: " + workspaceId);
        }

        clientApiIngestPreview = new ClientApiIngestPreview();
        createdVertexIds = Lists.newArrayList();
        createdEdgeIds = Lists.newArrayList();
        visibilityJson = VisalloProperties.VISIBILITY_JSON.getPropertyValue(structuredFileVertex);
        checkNotNull(visibilityJson);

        if (this.publish) {
            if (!privilegeRepository.hasPrivilege(user, Privilege.PUBLISH)) {
                this.publish = false;
            }
        }

        if (this.publish) {
            visibilityJson = new VisibilityJson("");
        } else {
            visibilityJson.addWorkspace(workspaceId);
        }
        visibility = visibilityTranslator.toVisibility(visibilityJson).getVisibility();

        propertyMetadata = new PropertyMetadata(
                new Date(),
                user,
                GraphRepository.SET_PROPERTY_CONFIDENCE,
                visibilityJson,
                visibilityTranslator.getDefaultVisibility()
        );
    }

    public void reset() {
        parseErrors.errors.clear();
        sheetNumber = -1;
        clientApiIngestPreview = new ClientApiIngestPreview();
        createdVertexIds.clear();
        createdEdgeIds.clear();
    }

    public boolean hasErrors() {
        return !parseErrors.errors.isEmpty();
    }

    @Override
    public void newSheet(String name) {
        // Right now, it will ingest all of the columns in the first sheet since that's
        // what the interface shows. In the future, if they can select a different sheet
        // this code will need to be updated.
        sheetNumber++;
    }

    @Override
    public boolean addRow(Map<String, Object> row, long rowNum) {
        Long rowCount = rowNum + 1;
        if (dryRun && rowCount > MAX_DRY_RUN_ROWS) {
            clientApiIngestPreview.didTruncate = true;
            return false;
        }
        clientApiIngestPreview.processedRows = rowCount;

        Visibility defaultVisibility = visibilityTranslator.getDefaultVisibility();

        // Since we only handle the first sheet currently, bail if this isn't it.
        if (sheetNumber != 0) {
            return false;
        }

        try {
            List<String> newVertexIds = new ArrayList<>();
            List<VertexBuilder> vertexBuilders = new ArrayList<>();
            List<String> workspaceUpdates = new ArrayList<>();
            long vertexNum = 0;
            for (VertexMapping vertexMapping : parseMapping.vertexMappings) {
                VertexBuilder vertexBuilder = createVertex(vertexMapping, row, rowNum, vertexNum);
                if (vertexBuilder != null) {
                    boolean alreadyCreated = createdVertexIds.contains(vertexBuilder.getVertexId());
                    vertexBuilders.add(vertexBuilder);
                    newVertexIds.add(vertexBuilder.getVertexId());
                    createdVertexIds.add(vertexBuilder.getVertexId());
                    workspaceUpdates.add(vertexBuilder.getVertexId());
                    if (!alreadyCreated) {
                        incrementConcept(vertexMapping, !graph.doesVertexExist(vertexBuilder.getVertexId(), authorizations));
                    }
                } else {
                    newVertexIds.add(SKIPPED_VERTEX_ID);
                }
                vertexNum++;
            }

            List<EdgeBuilderByVertexId> edgeBuilders = new ArrayList<>();
            for (EdgeMapping edgeMapping : parseMapping.edgeMappings) {
                EdgeBuilderByVertexId edgeBuilder = createEdge(edgeMapping, newVertexIds);
                if (edgeBuilder != null) {
                    boolean alreadyCreated = createdEdgeIds.contains(edgeBuilder.getEdgeId());
                    createdEdgeIds.add(edgeBuilder.getEdgeId());
                    edgeBuilders.add(edgeBuilder);
                    if (!alreadyCreated) {
                        incrementEdges(edgeMapping, !graph.doesEdgeExist(edgeBuilder.getEdgeId(), authorizations));
                    }
                }
            }

            if (!dryRun) {
                HashFunction hash = Hashing.sha1();
                for (VertexBuilder vertexBuilder : vertexBuilders) {
                    Vertex newVertex = vertexBuilder.save(authorizations);
                    EdgeBuilder hasSourceEdgeBuilder = graph.prepareEdge(
                            hash.newHasher()
                                    .putString(newVertex.getId())
                                    .putString(structuredFileVertex.getId())
                                    .hash()
                                    .toString(),
                            newVertex,
                            structuredFileVertex,
                            StructuredIngestOntology.ELEMENT_HAS_SOURCE_IRI,
                            visibility
                    );
                    VisalloProperties.VISIBILITY_JSON.setProperty(hasSourceEdgeBuilder, visibilityJson, defaultVisibility);
                    VisalloProperties.MODIFIED_BY.setProperty(hasSourceEdgeBuilder, user.getUserId(), defaultVisibility);
                    VisalloProperties.MODIFIED_DATE.setProperty(hasSourceEdgeBuilder, new Date(), defaultVisibility);
                    hasSourceEdgeBuilder.save(authorizations);
                }

                for (EdgeBuilderByVertexId edgeBuilder : edgeBuilders) {
                    edgeBuilder.save(authorizations);
                }

                graph.flush();

                if (!this.publish && workspaceUpdates.size() > 0) {
                    workspaceRepository.updateEntitiesOnWorkspace(workspace, workspaceUpdates, user);
                }
            }
        } catch (SkipRowException sre) {
            // Skip the row and keep going
        }

        if (progressReporter != null) {
            progressReporter.finishedRow(rowNum, getTotalRows());
        }

        return !dryRun || maxParseErrors <= 0 || parseErrors.errors.size() < maxParseErrors;
    }

    private void incrementConcept(VertexMapping vertexMapping, boolean isNew) {
        for (PropertyMapping mapping : vertexMapping.propertyMappings) {
            if (VisalloProperties.CONCEPT_TYPE.getPropertyName().equals(mapping.name)) {
                clientApiIngestPreview.incrementVertices(mapping.value, isNew);
            }
        }
    }

    private void incrementEdges(EdgeMapping mapping, boolean isNew) {
        clientApiIngestPreview.incrementEdges(mapping.label, isNew);
    }

    public boolean cleanUpExistingImport() {
        Iterable<Vertex> vertices = structuredFileVertex.getVertices(
                Direction.IN,
                StructuredIngestOntology.ELEMENT_HAS_SOURCE_IRI,
                authorizations
        );

        for (Vertex vertex : vertices) {
            SandboxStatus sandboxStatus = SandboxStatusUtil.getSandboxStatus(vertex, workspace.getWorkspaceId());
            if (sandboxStatus != SandboxStatus.PUBLIC) {
                workspaceHelper.deleteVertex(
                        vertex,
                        workspace.getWorkspaceId(),
                        false,
                        Priority.HIGH,
                        authorizations,
                        user
                );
            }
        }

        return true;
    }

    private EdgeBuilderByVertexId createEdge(EdgeMapping edgeMapping, List<String> newVertexIds) {
        Visibility defaultVisibility = visibilityTranslator.getDefaultVisibility();
        String inVertexId = newVertexIds.get(edgeMapping.inVertexIndex);
        String outVertexId = newVertexIds.get(edgeMapping.outVertexIndex);

        if (inVertexId.equals(SKIPPED_VERTEX_ID) || outVertexId.equals(SKIPPED_VERTEX_ID)) {
            // TODO: handle edge errors properly?
            return null;
        }

        VisibilityJson edgeVisibilityJson = visibilityJson;
        Visibility edgeVisibility = visibility;
        if (edgeMapping.visibilityJson != null) {
            edgeVisibilityJson = edgeMapping.visibilityJson;
            edgeVisibility = edgeMapping.visibility;
        }

        EdgeBuilderByVertexId m = graph.prepareEdge(outVertexId, inVertexId, edgeMapping.label, edgeVisibility);
        VisalloProperties.VISIBILITY_JSON.setProperty(m, edgeVisibilityJson, edgeVisibility);
        VisalloProperties.MODIFIED_DATE.setProperty(m, propertyMetadata.getModifiedDate(), edgeVisibility);
        VisalloProperties.MODIFIED_BY.setProperty(m, propertyMetadata.getModifiedBy().getUserId(), edgeVisibility);
        return m;
    }

    private VertexBuilder createVertex(VertexMapping vertexMapping, Map<String, Object> row, long rowNum, long vertexNum) {
        VisibilityJson vertexVisibilityJson = visibilityJson;
        Visibility vertexVisibility = visibility;
        if (vertexMapping.visibilityJson != null) {
            vertexVisibilityJson = vertexMapping.visibilityJson;
            vertexVisibility = vertexMapping.visibility;
        }

        String vertexId = generateVertexId(vertexMapping, row, rowNum, vertexNum);

        VertexBuilder m = vertexId == null ? graph.prepareVertex(vertexVisibility) : graph.prepareVertex(vertexId, vertexVisibility);
        setPropertyValue(VisalloProperties.VISIBILITY_JSON, m, vertexVisibilityJson, vertexVisibility);

        for (PropertyMapping propertyMapping : vertexMapping.propertyMappings) {

            if (VisalloProperties.CONCEPT_TYPE.getPropertyName().equals(propertyMapping.name)) {
                setPropertyValue(VisalloProperties.CONCEPT_TYPE, m, propertyMapping.value, vertexVisibility);
                setPropertyValue(VisalloProperties.MODIFIED_DATE, m, propertyMetadata.getModifiedDate(), vertexVisibility);
                setPropertyValue(VisalloProperties.MODIFIED_BY, m, propertyMetadata.getModifiedBy().getUserId(), vertexVisibility);
            } else {
                Metadata metadata = propertyMetadata.createMetadata();
                try {
                    VisalloProperties.SOURCE_FILE_OFFSET_METADATA.setMetadata(metadata, Long.valueOf(rowNum), vertexVisibility);
                    setPropertyValue(m, row, propertyMapping, vertexVisibility, metadata);
                } catch (Exception e) {
                    LOGGER.error("Error parsing property.", e);

                    ClientApiParseErrors.ParseError pe = new ClientApiParseErrors.ParseError();
                    pe.rawPropertyValue = propertyMapping.extractRawValue(row);
                    pe.propertyMapping = propertyMapping;
                    pe.message = e.getMessage();
                    pe.sheetIndex = sheetNumber;
                    pe.rowIndex = rowNum;

                    if (!dryRun) {
                        if (propertyMapping.errorHandlingStrategy == PropertyMapping.ErrorHandlingStrategy.SKIP_ROW) {
                            throw new SkipRowException("Error parsing property.", e);
                        } else if (propertyMapping.errorHandlingStrategy == PropertyMapping.ErrorHandlingStrategy.SKIP_VERTEX) {
                            return null;
                        } else if (propertyMapping.errorHandlingStrategy == PropertyMapping.ErrorHandlingStrategy.SET_CELL_ERROR_PROPERTY) {
                            String multiKey = sheetNumber + "_" + rowNum;
                            StructuredIngestOntology.ERROR_MESSAGE_PROPERTY.addPropertyValue(
                                    m,
                                    multiKey,
                                    pe.message,
                                    metadata,
                                    vertexVisibility
                            );
                            StructuredIngestOntology.RAW_CELL_VALUE_PROPERTY.addPropertyValue(
                                    m,
                                    multiKey,
                                    pe.rawPropertyValue.toString(),
                                    metadata,
                                    vertexVisibility
                            );
                            StructuredIngestOntology.TARGET_PROPERTY.addPropertyValue(
                                    m,
                                    multiKey,
                                    pe.propertyMapping.name,
                                    metadata,
                                    vertexVisibility
                            );
                            StructuredIngestOntology.SHEET_PROPERTY.addPropertyValue(
                                    m,
                                    multiKey,
                                    String.valueOf(sheetNumber),
                                    metadata,
                                    vertexVisibility
                            );
                            StructuredIngestOntology.ROW_PROPERTY.addPropertyValue(
                                    m,
                                    multiKey,
                                    String.valueOf(rowNum),
                                    metadata,
                                    vertexVisibility
                            );
                        } else if (propertyMapping.errorHandlingStrategy != PropertyMapping.ErrorHandlingStrategy.SKIP_CELL) {
                            throw new VisalloException("Unhandled mapping error. Please provide a strategy.");
                        }
                    } else if (propertyMapping.errorHandlingStrategy == null) {
                        parseErrors.errors.add(pe);
                    }
                }
            }
        }

        return m;
    }

    private String generateVertexId(VertexMapping vertexMapping, Map<String, Object> row, long rowNum, long vertexNum) {
        List<String> identifierParts = new ArrayList<>();

        // Find any mappings that designate identifier columns
        for (String key : row.keySet()) {
            for (PropertyMapping mapping : vertexMapping.propertyMappings) {
                if (mapping.key.equals(key) && mapping.identifier) {
                    Object val = row.get(key);
                    if (val != null && !val.toString().isEmpty()) {
                        identifierParts.add(key);
                    }
                }

            }
        }

        HashFunction sha1 = Hashing.sha1();
        Hasher hasher = sha1.newHasher();

        if (identifierParts.isEmpty()) {
            // By default just allow the same file to ingest without creating new entities
            hasher
                .putString(structuredFileVertex.getId()).putString("|")
                .putLong(rowNum).putString("|")
                .putLong(vertexNum);
        } else {
            // Hash all the identifier values and the concept. Use delimiter to minimize collisions
            identifierParts
                    .stream()
                    .sorted(String::compareToIgnoreCase)
                    .forEach(s -> {
                        hasher.putString(row.get(s).toString(), Charsets.UTF_8).putString("|");
                    });

            for (PropertyMapping mapping : vertexMapping.propertyMappings) {
                if (VisalloProperties.CONCEPT_TYPE.getPropertyName().equals(mapping.name)) {
                    hasher.putString(mapping.value);
                }
            }
        }


        HashCode hash = hasher.hash();
        String vertexId = hash.toString();

        // We might need to also hash the workspace if this vertex exists in the system but not visible to user.
        if (shouldAddWorkspaceToId(vertexId)) {
            vertexId = sha1.newHasher()
                    .putString(vertexId)
                    .putString(workspace.getWorkspaceId())
                    .hash()
                    .toString();
        }

        return vertexId;
    }

    /**
     * If the user is creating an entity that is unpublished in different sandbox, this user won't be able to access
     * it since prepareVertex with same id won't change the visibility.
     */
    private boolean shouldAddWorkspaceToId(String vertexId) {
        boolean vertexExistsForUser = graph.doesVertexExist(vertexId, authorizations);
        if (!vertexExistsForUser) {
            boolean vertexExistsInSystem = graph.doesVertexExist(vertexId, visalloUserAuths);
            if (vertexExistsInSystem) {
                return true;
            }
        }
        return false;
    }

    private void setPropertyValue(SingleValueVisalloProperty property, VertexBuilder m, Object value, Visibility vertexVisibility) {
        Metadata metadata = propertyMetadata.createMetadata();
        property.setProperty(m, value, metadata, vertexVisibility);
    }

    private void setPropertyValue(
            VertexBuilder m, Map<String, Object> row, PropertyMapping propertyMapping, Visibility vertexVisibility,
            Metadata metadata
    ) throws Exception {
        Visibility propertyVisibility = vertexVisibility;
        if (propertyMapping.visibility != null) {
            propertyVisibility = propertyMapping.visibility;
            VISIBILITY_JSON_METADATA.setMetadata(
                    metadata, propertyMapping.visibilityJson, visibilityTranslator.getDefaultVisibility());
        }

        Object propertyValue = propertyMapping.decodeValue(row);
        if (propertyValue != null) {
            m.addPropertyValue(MULTI_KEY, propertyMapping.name, propertyValue, metadata, propertyVisibility);
        }
    }
}
