package org.visallo.web.routes.vertex;

import com.google.common.io.Files;
import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Optional;
import org.apache.commons.fileupload.FileUploadBase;
import org.apache.commons.fileupload.ParameterParser;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.Vertex;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.ingest.FileImport;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.model.workspace.Workspace;
import org.visallo.core.model.workspace.WorkspaceHelper;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.security.VisibilityTranslator;
import org.visallo.core.user.User;
import org.visallo.core.util.ClientApiConverter;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.BadRequestException;
import org.visallo.web.clientapi.model.ClientApiArtifactImportResponse;
import org.visallo.web.clientapi.model.ClientApiImportProperty;
import org.visallo.web.parameterProviders.ActiveWorkspaceId;
import org.visallo.web.util.HttpPartUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicInteger;

public class VertexImport implements ParameterizedHandler {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(VertexImport.class);
    private static final String PARAMS_FILENAME = "filename";
    private static final String UNKNOWN_FILENAME = "unknown_filename";
    private final Graph graph;
    private final FileImport fileImport;
    private final WorkspaceRepository workspaceRepository;
    private final VisibilityTranslator visibilityTranslator;
    private final WorkspaceHelper workspaceHelper;
    private Authorizations authorizations;

    @Inject
    public VertexImport(
            Graph graph,
            FileImport fileImport,
            WorkspaceRepository workspaceRepository,
            VisibilityTranslator visibilityTranslator,
            WorkspaceHelper workspaceHelper
    ) {
        this.graph = graph;
        this.fileImport = fileImport;
        this.workspaceRepository = workspaceRepository;
        this.visibilityTranslator = visibilityTranslator;
        this.workspaceHelper = workspaceHelper;
    }

    protected static String getFilename(Part part) {
        String fileName = UNKNOWN_FILENAME;

        final ParameterParser parser = new ParameterParser();
        parser.setLowerCaseNames(true);

        final Map params = parser.parse(part.getHeader(FileUploadBase.CONTENT_DISPOSITION), ';');
        if (params.containsKey(PARAMS_FILENAME)) {
            final String name = (String) params.get(PARAMS_FILENAME);
            if (name != null) {
                try {
                    fileName = URLDecoder.decode(name, "utf8").trim();
                } catch (UnsupportedEncodingException ex) {
                    LOGGER.error("Failed to url decode: " + name, ex);
                    fileName = name.trim();
                }
            }
        }

        return fileName;
    }

    @Handle
    public ClientApiArtifactImportResponse handle(
            @Optional(name = "publish", defaultValue = "false") boolean shouldPublish,
            @Optional(name = "addToWorkspace", defaultValue = "false") boolean addToWorkspace,
            @Optional(name = "findExistingByFileHash", defaultValue = "true") boolean findExistingByFileHash,
            @ActiveWorkspaceId String workspaceId,
            Authorizations authorizations,
            User user,
            ResourceBundle resourceBundle,
            HttpServletRequest request
    ) throws Exception {
        if (!ServletFileUpload.isMultipartContent(request)) {
            throw new BadRequestException("file", "Could not process request without multi-part content");
        }

        workspaceId = workspaceHelper.getWorkspaceIdOrNullIfPublish(workspaceId, shouldPublish, user);

        this.authorizations = authorizations;

        File tempDir = Files.createTempDir();
        try {
            List<FileImport.FileOptions> files = getFiles(request, tempDir, resourceBundle, authorizations, user);
            if (files == null) {
                throw new BadRequestException("file", "Could not process request without files");
            }

            Workspace workspace = workspaceRepository.findById(workspaceId, user);

            List<Vertex> vertices = fileImport.importVertices(
                    workspace,
                    files,
                    Priority.HIGH,
                    addToWorkspace,
                    findExistingByFileHash,
                    user,
                    authorizations
            );

            return toArtifactImportResponse(vertices);
        } finally {
            FileUtils.deleteDirectory(tempDir);
        }
    }

    protected ClientApiArtifactImportResponse toArtifactImportResponse(List<Vertex> vertices) {
        ClientApiArtifactImportResponse response = new ClientApiArtifactImportResponse();
        for (Vertex vertex : vertices) {
            response.getVertexIds().add(vertex.getId());
        }
        return response;
    }

    protected List<FileImport.FileOptions> getFiles(
            HttpServletRequest request,
            File tempDir,
            ResourceBundle resourceBundle,
            Authorizations authorizations,
            User user
    ) throws Exception {
        List<String> invalidVisibilities = new ArrayList<>();
        List<FileImport.FileOptions> files = new ArrayList<>();
        AtomicInteger visibilitySourceIndex = new AtomicInteger(0);
        AtomicInteger conceptIndex = new AtomicInteger(0);
        AtomicInteger fileIndex = new AtomicInteger(0);
        AtomicInteger propertiesIndex = new AtomicInteger(0);
        for (Part part : request.getParts()) {
            if (part.getName().equals("file")) {
                String fileName = getFilename(part);
                File outFile = new File(tempDir, fileName);
                HttpPartUtil.copyPartToFile(part, outFile);
                addFileToFilesList(files, fileIndex.getAndIncrement(), outFile);
            } else if (part.getName().equals("conceptId")) {
                String conceptId = IOUtils.toString(part.getInputStream(), "UTF8");
                addConceptIdToFilesList(files, conceptIndex.getAndIncrement(), conceptId);
            } else if (part.getName().equals("properties")) {
                String propertiesString = IOUtils.toString(part.getInputStream(), "UTF8");
                ClientApiImportProperty[] properties = convertPropertiesStringToClientApiImportProperties(
                        propertiesString);
                addPropertiesToFilesList(files, propertiesIndex.getAndIncrement(), properties);
            } else if (part.getName().equals("visibilitySource")) {
                String visibilitySource = IOUtils.toString(part.getInputStream(), "UTF8");
                if (!graph.isVisibilityValid(
                        visibilityTranslator.toVisibility(visibilitySource).getVisibility(),
                        authorizations
                )) {
                    invalidVisibilities.add(visibilitySource);
                }
                addVisibilityToFilesList(files, visibilitySourceIndex.getAndIncrement(), visibilitySource);
            }
        }

        if (invalidVisibilities.size() > 0) {
            LOGGER.warn(
                    "%s is not a valid visibility for %s user",
                    invalidVisibilities.toString(),
                    user.getDisplayName()
            );
            throw new BadRequestException(
                    "visibilitySource",
                    resourceBundle.getString("visibility.invalid"),
                    invalidVisibilities
            );
        }

        return files;
    }

    protected ClientApiImportProperty[] convertPropertiesStringToClientApiImportProperties(String propertiesString) throws Exception {
        JSONArray properties = new JSONArray(propertiesString);
        ClientApiImportProperty[] clientApiProperties = new ClientApiImportProperty[properties.length()];
        for (int i = 0; i < properties.length(); i++) {
            String propertyString;
            try {
                propertyString = properties.getJSONObject(i).toString();
            } catch (JSONException e) {
                throw new VisalloException("Could not parse properties json", e);
            }
            clientApiProperties[i] = ClientApiConverter.toClientApi(propertyString, ClientApiImportProperty.class);
        }
        return clientApiProperties;
    }

    protected void addPropertiesToFilesList(
            List<FileImport.FileOptions> files,
            int index,
            ClientApiImportProperty[] properties
    ) {
        ensureFilesSize(files, index);
        if (properties != null && properties.length > 0) {
            files.get(index).setProperties(properties);
        }
    }

    protected void addConceptIdToFilesList(List<FileImport.FileOptions> files, int index, String conceptId) {
        ensureFilesSize(files, index);
        if (conceptId != null && conceptId.length() > 0) {
            files.get(index).setConceptId(conceptId);
        }
    }

    protected void addVisibilityToFilesList(List<FileImport.FileOptions> files, int index, String visibilitySource) {
        ensureFilesSize(files, index);
        files.get(index).setVisibilitySource(visibilitySource);
    }

    protected void addFileToFilesList(List<FileImport.FileOptions> files, int index, File file) {
        ensureFilesSize(files, index);
        files.get(index).setFile(file);
    }

    private void ensureFilesSize(List<FileImport.FileOptions> files, int index) {
        while (files.size() <= index) {
            files.add(new FileImport.FileOptions());
        }
    }

    public Graph getGraph() {
        return graph;
    }

    protected Authorizations getAuthorizations() {
        return authorizations;
    }
}
