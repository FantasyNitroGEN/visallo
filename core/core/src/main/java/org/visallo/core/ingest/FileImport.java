package org.visallo.core.ingest;

import com.google.inject.Inject;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.vertexium.*;
import org.vertexium.property.StreamingPropertyValue;
import org.visallo.core.config.Configuration;
import org.visallo.core.model.WorkQueueNames;
import org.visallo.core.model.ontology.OntologyProperty;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.properties.types.PropertyMetadata;
import org.visallo.core.model.properties.types.VisalloProperty;
import org.visallo.core.model.properties.types.VisalloPropertyUpdate;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.model.workspace.Workspace;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.security.VisalloVisibility;
import org.visallo.core.security.VisibilityTranslator;
import org.visallo.core.user.User;
import org.visallo.core.util.RowKeyHelper;
import org.visallo.core.util.ServiceLoaderUtil;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.clientapi.model.ClientApiImportProperty;
import org.visallo.web.clientapi.model.VisibilityJson;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;

import static org.vertexium.util.IterableUtils.toList;

public class FileImport {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(FileImport.class);
    public static final String MULTI_VALUE_KEY = FileImport.class.getName();
    private final VisibilityTranslator visibilityTranslator;
    private final Graph graph;
    private final WorkQueueRepository workQueueRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkQueueNames workQueueNames;
    private final OntologyRepository ontologyRepository;
    private final Configuration configuration;
    private List<FileImportSupportingFileHandler> fileImportSupportingFileHandlers;
    private List<PostFileImportHandler> postFileImportHandlers;

    @Inject
    public FileImport(
            VisibilityTranslator visibilityTranslator,
            Graph graph,
            WorkQueueRepository workQueueRepository,
            WorkspaceRepository workspaceRepository,
            WorkQueueNames workQueueNames,
            OntologyRepository ontologyRepository,
            Configuration configuration
    ) {
        this.visibilityTranslator = visibilityTranslator;
        this.graph = graph;
        this.workQueueRepository = workQueueRepository;
        this.workspaceRepository = workspaceRepository;
        this.workQueueNames = workQueueNames;
        this.ontologyRepository = ontologyRepository;
        this.configuration = configuration;
    }

    public void importDirectory(
            File dataDir,
            boolean queueDuplicates,
            String conceptTypeIRI,
            String visibilitySource,
            Workspace workspace,
            Priority priority,
            User user,
            Authorizations authorizations
    ) throws IOException {
        ensureInitialized();

        LOGGER.debug("Importing files from %s", dataDir);
        File[] files = dataDir.listFiles();
        if (files == null || files.length == 0) {
            return;
        }

        int totalFileCount = files.length;
        int fileCount = 0;
        int importedFileCount = 0;
        try {
            for (File f : files) {
                if (f.getName().startsWith(".") || f.length() == 0) {
                    continue;
                }
                if (isSupportingFile(f)) {
                    continue;
                }

                LOGGER.debug("Importing file (%d/%d): %s", fileCount + 1, totalFileCount, f.getAbsolutePath());
                try {
                    ClientApiImportProperty[] properties = null;
                    boolean findExistingByFileHash = true;
                    boolean addToWorkspace = false;
                    importFile(
                            f,
                            queueDuplicates,
                            conceptTypeIRI,
                            properties,
                            visibilitySource,
                            workspace,
                            addToWorkspace,
                            findExistingByFileHash,
                            priority,
                            user,
                            authorizations
                    );
                    importedFileCount++;
                } catch (Exception ex) {
                    LOGGER.error("Could not import %s", f.getAbsolutePath(), ex);
                }
                fileCount++;
            }
        } finally {
            graph.flush();
        }

        LOGGER.debug(String.format("Imported %d, skipped %d files from %s", importedFileCount, fileCount - importedFileCount, dataDir));
    }

    private boolean isSupportingFile(File f) {
        for (FileImportSupportingFileHandler fileImportSupportingFileHandler : this.fileImportSupportingFileHandlers) {
            if (fileImportSupportingFileHandler.isSupportingFile(f)) {
                return true;
            }
        }
        return false;
    }

    public Vertex importFile(
            File f,
            boolean queueDuplicates,
            String visibilitySource,
            Workspace workspace,
            Priority priority,
            User user,
            Authorizations authorizations
    ) throws Exception {
        String conceptId = null;
        ClientApiImportProperty[] properties = null;
        boolean findExistingByFileHash = true;
        boolean addToWorkspace = false;
        return importFile(
                f,
                queueDuplicates,
                conceptId,
                properties,
                visibilitySource,
                workspace,
                addToWorkspace,
                findExistingByFileHash,
                priority,
                user,
                authorizations
        );
    }

    public Vertex importFile(
            File f,
            boolean queueDuplicates,
            String conceptId,
            ClientApiImportProperty[] properties,
            String visibilitySource,
            Workspace workspace,
            boolean addToWorkspace,
            boolean findExistingByFileHash,
            Priority priority,
            User user,
            Authorizations authorizations
    ) throws Exception {
        Vertex vertex;
        ensureInitialized();

        String hash = calculateFileHash(f);

        if (findExistingByFileHash) {
            vertex = findExistingVertexWithHash(hash, authorizations);
            if (vertex != null) {
                LOGGER.debug("vertex already exists with hash %s", hash);
                if (queueDuplicates) {
                    LOGGER.debug(
                            "pushing %s on to %s queue",
                            vertex.getId(),
                            workQueueNames.getGraphPropertyQueueName()
                    );
                    if (workspace != null) {
                        workspaceRepository.updateEntityOnWorkspace(
                                workspace,
                                vertex.getId(),
                                user
                        );
                        workQueueRepository.broadcastElement(vertex, workspace.getWorkspaceId());
                        workQueueRepository.pushGraphPropertyQueue(
                                vertex,
                                MULTI_VALUE_KEY,
                                VisalloProperties.RAW.getPropertyName(),
                                workspace.getWorkspaceId(),
                                visibilitySource,
                                priority
                        );
                    } else {
                        workQueueRepository.pushGraphPropertyQueue(
                                vertex,
                                MULTI_VALUE_KEY,
                                VisalloProperties.RAW.getPropertyName(),
                                priority
                        );
                    }
                }
                return vertex;
            }
        }

        List<FileImportSupportingFileHandler.AddSupportingFilesResult> addSupportingFilesResults = new ArrayList<>();

        try (FileInputStream fileInputStream = new FileInputStream(f)) {
            JSONObject metadataJson = loadMetadataJson(f);
            String predefinedId = null;
            if (metadataJson != null) {
                predefinedId = metadataJson.optString("id", null);
                String metadataVisibilitySource = metadataJson.optString("visibilitySource", null);
                if (metadataVisibilitySource != null) {
                    visibilitySource = metadataVisibilitySource;
                }
            }

            StreamingPropertyValue rawValue = new StreamingPropertyValue(fileInputStream, byte[].class);
            rawValue.searchIndex(false);

            VisibilityJson visibilityJson = VisibilityJson.updateVisibilitySourceAndAddWorkspaceId(null, visibilitySource, workspace == null ? null : workspace.getWorkspaceId());
            VisalloVisibility visalloVisibility = this.visibilityTranslator.toVisibility(visibilityJson);
            Visibility visibility = visalloVisibility.getVisibility();
            Visibility defaultVisibility = visibilityTranslator.getDefaultVisibility();
            PropertyMetadata propertyMetadata = new PropertyMetadata(user, visibilityJson, visibility);
            VisalloProperties.CONFIDENCE_METADATA.setMetadata(propertyMetadata, 0.1, visibilityTranslator.getDefaultVisibility());

            VertexBuilder vertexBuilder;
            if (predefinedId == null) {
                vertexBuilder = this.graph.prepareVertex(visibility);
            } else {
                vertexBuilder = this.graph.prepareVertex(predefinedId, visibility);
            }
            List<VisalloPropertyUpdate> changedProperties = new ArrayList<>();
            VisalloProperties.RAW.updateProperty(changedProperties, null, vertexBuilder, rawValue, propertyMetadata);
            VisalloProperties.CONTENT_HASH.updateProperty(changedProperties, null, vertexBuilder, MULTI_VALUE_KEY, hash, propertyMetadata);
            VisalloProperties.FILE_NAME.updateProperty(changedProperties, null, vertexBuilder, MULTI_VALUE_KEY, f.getName(), propertyMetadata);
            VisalloProperties.MODIFIED_DATE.updateProperty(
                    changedProperties,
                    null,
                    vertexBuilder,
                    new Date(f.lastModified()),
                    (Metadata) null,
                    defaultVisibility
            );
            VisalloProperties.MODIFIED_BY.updateProperty(
                    changedProperties,
                    null,
                    vertexBuilder,
                    user.getUserId(),
                    (Metadata) null,
                    defaultVisibility
            );
            VisalloProperties.VISIBILITY_JSON.updateProperty(
                    changedProperties,
                    null,
                    vertexBuilder,
                    visibilityJson,
                    (Metadata) null,
                    defaultVisibility
            );
            if (conceptId != null) {
                VisalloProperties.CONCEPT_TYPE.updateProperty(
                        changedProperties,
                        null,
                        vertexBuilder,
                        conceptId,
                        (Metadata) null,
                        defaultVisibility
                );
            }
            if (properties != null) {
                addProperties(properties, changedProperties, vertexBuilder, visibilityJson, workspace, user);
            }

            for (FileImportSupportingFileHandler fileImportSupportingFileHandler : this.fileImportSupportingFileHandlers) {
                FileImportSupportingFileHandler.AddSupportingFilesResult addSupportingFilesResult = fileImportSupportingFileHandler.addSupportingFiles(vertexBuilder, f, visibility);
                if (addSupportingFilesResult != null) {
                    addSupportingFilesResults.add(addSupportingFilesResult);
                }
            }

            vertex = vertexBuilder.save(authorizations);

            for (PostFileImportHandler postFileImportHandler : this.postFileImportHandlers) {
                postFileImportHandler.handle(graph, vertex, changedProperties, workspace, propertyMetadata, visibility, user, authorizations);
            }

            graph.flush();

            String workspaceId = null;
            if (workspace != null) {
                workspaceRepository.updateEntityOnWorkspace(workspace, vertex.getId(), user);
                workspaceId = workspace.getWorkspaceId();
            }

            LOGGER.debug("File %s imported. vertex id: %s", f.getAbsolutePath(), vertex.getId());
            LOGGER.debug("pushing %s on to %s queue", vertex.getId(), workQueueNames.getGraphPropertyQueueName());
            this.workQueueRepository.broadcastElement(vertex, workspaceId);
            this.workQueueRepository.pushGraphVisalloPropertyQueue(
                    vertex,
                    changedProperties,
                    workspace == null ? null : workspace.getWorkspaceId(),
                    visibilitySource,
                    priority
            );
            return vertex;
        } finally {
            for (FileImportSupportingFileHandler.AddSupportingFilesResult addSupportingFilesResult : addSupportingFilesResults) {
                addSupportingFilesResult.close();
            }
        }
    }

    private void addProperties(ClientApiImportProperty[] properties, List<VisalloPropertyUpdate> changedProperties, VertexBuilder vertexBuilder, VisibilityJson visibilityJson, Workspace workspace, User user) throws ParseException {
        for (ClientApiImportProperty property : properties) {
            OntologyProperty ontologyProperty = ontologyRepository.getPropertyByIRI(property.getName());
            if (ontologyProperty == null) {
                ontologyProperty = ontologyRepository.getRequiredPropertyByIntent(property.getName());
            }
            Object value = ontologyProperty.convertString(property.getValue());
            VisalloProperty prop = ontologyProperty.getVisalloProperty();
            VisibilityJson propertyVisibilityJson = VisibilityJson.updateVisibilitySourceAndAddWorkspaceId(null, property.getVisibilitySource(), workspace == null ? null : workspace.getWorkspaceId());
            VisalloVisibility propertyVisibility = visibilityTranslator.toVisibility(propertyVisibilityJson);
            PropertyMetadata propMetadata = new PropertyMetadata(user, visibilityJson, visibilityTranslator.getDefaultVisibility());
            for (Map.Entry<String, Object> metadataEntry : property.getMetadata().entrySet()) {
                propMetadata.add(metadataEntry.getKey(), metadataEntry.getValue(), visibilityTranslator.getDefaultVisibility());
            }
            //noinspection unchecked
            prop.updateProperty(changedProperties, null, vertexBuilder, property.getKey(), value, propMetadata);
        }
    }

    public List<Vertex> importVertices(
            Workspace workspace,
            List<FileOptions> files,
            Priority priority,
            boolean addToWorkspace,
            boolean findExistingByFileHash,
            User user,
            Authorizations authorizations
    ) throws Exception {
        ensureInitialized();

        List<Vertex> vertices = new ArrayList<>();
        for (FileOptions file : files) {
            if (isSupportingFile(file.getFile())) {
                LOGGER.debug("Skipping file: %s (supporting file)", file.getFile().getAbsolutePath());
                continue;
            }
            LOGGER.debug("Processing file: %s", file.getFile().getAbsolutePath());
            Vertex vertex = importFile(
                    file.getFile(),
                    true,
                    file.getConceptId(),
                    file.getProperties(),
                    file.getVisibilitySource(),
                    workspace,
                    addToWorkspace,
                    findExistingByFileHash,
                    priority,
                    user,
                    authorizations
            );
            vertices.add(vertex);
        }
        return vertices;
    }

    private JSONObject loadMetadataJson(File f) throws IOException {
        File metadataFile = MetadataFileImportSupportingFileHandler.getMetadataFile(f);
        if (metadataFile.exists()) {
            try (FileInputStream in = new FileInputStream(metadataFile)) {
                String fileContents = IOUtils.toString(in);
                return new JSONObject(fileContents);
            }
        }
        return null;
    }

    private void ensureInitialized() {
        if (fileImportSupportingFileHandlers == null) {
            fileImportSupportingFileHandlers = getFileImportSupportingFileHandlers();
        }

        if (postFileImportHandlers == null) {
            postFileImportHandlers = getPostFileImportHandlers();
        }
    }

    protected List<PostFileImportHandler> getPostFileImportHandlers() {
        return toList(ServiceLoaderUtil.load(PostFileImportHandler.class, this.configuration));
    }

    protected List<FileImportSupportingFileHandler> getFileImportSupportingFileHandlers() {
        return toList(ServiceLoaderUtil.load(FileImportSupportingFileHandler.class, this.configuration));
    }

    private Vertex findExistingVertexWithHash(String hash, Authorizations authorizations) {
        Iterator<Vertex> existingVertices = this.graph.query(authorizations)
                .has(VisalloProperties.CONTENT_HASH.getPropertyName(), hash)
                .vertices()
                .iterator();
        if (existingVertices.hasNext()) {
            return existingVertices.next();
        }
        return null;
    }

    private String calculateFileHash(File f) throws IOException {
        try (FileInputStream fileInputStream = new FileInputStream(f)) {
            return RowKeyHelper.buildSHA256KeyString(fileInputStream);
        }
    }

    public static class FileOptions {
        private File file;
        private String visibilitySource;
        private String conceptId;
        private ClientApiImportProperty[] properties;

        public File getFile() {
            return file;
        }

        public void setFile(File file) {
            this.file = file;
        }

        public String getConceptId() {
            return conceptId;
        }

        public void setConceptId(String conceptId) {
            this.conceptId = conceptId;
        }

        public String getVisibilitySource() {
            return visibilitySource;
        }

        public void setVisibilitySource(String visibilitySource) {
            this.visibilitySource = visibilitySource;
        }

        public void setProperties(ClientApiImportProperty[] properties) {
            this.properties = properties;
        }

        public ClientApiImportProperty[] getProperties() {
            return properties;
        }
    }
}
