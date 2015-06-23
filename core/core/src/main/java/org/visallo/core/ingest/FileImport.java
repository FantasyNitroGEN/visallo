package org.visallo.core.ingest;

import com.google.inject.Inject;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.vertexium.*;
import org.vertexium.property.StreamingPropertyValue;
import org.visallo.core.bootstrap.InjectHelper;
import org.visallo.core.config.Configuration;
import org.visallo.core.model.WorkQueueNames;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.properties.types.PropertyMetadata;
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
import org.visallo.web.clientapi.model.VisibilityJson;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import static org.vertexium.util.IterableUtils.toList;

public class FileImport {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(FileImport.class);
    public static final String MULTI_VALUE_KEY = FileImport.class.getName();
    private final VisibilityTranslator visibilityTranslator;
    private final Graph graph;
    private final WorkQueueRepository workQueueRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkQueueNames workQueueNames;
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
            Configuration configuration
    ) {
        this.visibilityTranslator = visibilityTranslator;
        this.graph = graph;
        this.workQueueRepository = workQueueRepository;
        this.workspaceRepository = workspaceRepository;
        this.workQueueNames = workQueueNames;
        this.configuration = configuration;
    }

    public void importDirectory(File dataDir, boolean queueDuplicates, String conceptTypeIRI, String visibilitySource, Workspace workspace, Priority priority, User user, Authorizations authorizations) throws IOException {
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
                    importFile(f, queueDuplicates, conceptTypeIRI, visibilitySource, workspace, priority, user, authorizations);
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

    public Vertex importFile(File f, boolean queueDuplicates, String visibilitySource, Workspace workspace, Priority priority, User user, Authorizations authorizations) throws Exception {
        return importFile(f, queueDuplicates, null, visibilitySource, workspace, priority, user, authorizations);
    }

    public Vertex importFile(File f, boolean queueDuplicates, String conceptId, String visibilitySource, Workspace workspace, Priority priority, User user, Authorizations authorizations) throws Exception {
        ensureInitialized();

        String hash = calculateFileHash(f);

        Vertex vertex = findExistingVertexWithHash(hash, authorizations);
        if (vertex != null) {
            LOGGER.warn("vertex already exists with hash %s", hash);
            if (queueDuplicates) {
                LOGGER.debug("pushing %s on to %s queue", vertex.getId(), workQueueNames.getGraphPropertyQueueName());
                this.workQueueRepository.pushElement(vertex);
                if (workspace != null) {
                    this.workQueueRepository.pushGraphPropertyQueue(
                            vertex,
                            MULTI_VALUE_KEY,
                            VisalloProperties.RAW.getPropertyName(),
                            workspace.getWorkspaceId(),
                            visibilitySource,
                            priority
                    );
                } else {
                    this.workQueueRepository.pushGraphPropertyQueue(vertex, MULTI_VALUE_KEY, VisalloProperties.RAW.getPropertyName(), priority);
                }
            }
            return vertex;
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
            PropertyMetadata propertyMetadata = new PropertyMetadata(user, visibilityJson, visibilityTranslator.getDefaultVisibility());
            VisalloProperties.CONFIDENCE_METADATA.setMetadata(propertyMetadata, 0.1, visibilityTranslator.getDefaultVisibility());

            VertexBuilder vertexBuilder;
            if (predefinedId == null) {
                vertexBuilder = this.graph.prepareVertex(visibility);
            } else {
                vertexBuilder = this.graph.prepareVertex(predefinedId, visibility);
            }
            List<VisalloPropertyUpdate> changedProperties = new ArrayList<>();
            VisalloProperties.VISIBILITY_JSON.updateProperty(changedProperties, null, vertexBuilder, visibilityJson, propertyMetadata, visibility);
            VisalloProperties.RAW.updateProperty(changedProperties, null, vertexBuilder, rawValue, propertyMetadata, visibility);
            VisalloProperties.CONTENT_HASH.updateProperty(changedProperties, null, vertexBuilder, MULTI_VALUE_KEY, hash, propertyMetadata, visibility);
            VisalloProperties.FILE_NAME.updateProperty(changedProperties, null, vertexBuilder, MULTI_VALUE_KEY, f.getName(), propertyMetadata, visibility);
            VisalloProperties.MODIFIED_DATE.updateProperty(changedProperties, null, vertexBuilder, new Date(f.lastModified()), propertyMetadata, visibility);
            if (conceptId != null) {
                VisalloProperties.CONCEPT_TYPE.updateProperty(changedProperties, null, vertexBuilder, conceptId, propertyMetadata, visibility);
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

            if (workspace != null) {
                workspaceRepository.updateEntityOnWorkspace(workspace, vertex.getId(), null, null, user);
            }

            LOGGER.debug("File %s imported. vertex id: %s", f.getAbsolutePath(), vertex.getId());
            LOGGER.debug("pushing %s on to %s queue", vertex.getId(), workQueueNames.getGraphPropertyQueueName());
            this.workQueueRepository.pushElement(vertex);
            this.workQueueRepository.pushGraphVisalloPropertyQueue(
                    vertex,
                    changedProperties, workspace == null ? null : workspace.getWorkspaceId(),
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

    public List<Vertex> importVertices(Workspace workspace, List<FileOptions> files, Priority priority, User user, Authorizations authorizations) throws Exception {
        ensureInitialized();

        List<Vertex> vertices = new ArrayList<>();
        for (FileOptions file : files) {
            if (isSupportingFile(file.getFile())) {
                LOGGER.debug("Skipping file: %s (supporting file)", file.getFile().getAbsolutePath());
                continue;
            }
            LOGGER.debug("Processing file: %s", file.getFile().getAbsolutePath());
            vertices.add(importFile(file.getFile(), true, file.getConceptId(), file.getVisibilitySource(), workspace, priority, user, authorizations));
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
            fileImportSupportingFileHandlers = toList(ServiceLoaderUtil.load(FileImportSupportingFileHandler.class, this.configuration));
            for (FileImportSupportingFileHandler fileImportSupportingFileHandler : fileImportSupportingFileHandlers) {
                InjectHelper.inject(fileImportSupportingFileHandler);
            }
        }

        if (postFileImportHandlers == null) {
            postFileImportHandlers = toList(ServiceLoaderUtil.load(PostFileImportHandler.class, this.configuration));
            for (PostFileImportHandler postFileImportHandler : postFileImportHandlers) {
                InjectHelper.inject(postFileImportHandler);
            }
        }
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
    }
}
