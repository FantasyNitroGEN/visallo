package org.visallo.web.routes.vertex;

import com.google.common.io.Files;
import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Optional;
import com.v5analytics.webster.annotations.Required;
import org.apache.commons.fileupload.FileUploadBase;
import org.apache.commons.fileupload.ParameterParser;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.Vertex;
import org.vertexium.Visibility;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.ingest.FileImport;
import org.visallo.core.ingest.cloud.CloudImportLongRunningProcessQueueItem;
import org.visallo.core.model.longRunningProcess.FindPathLongRunningProcessQueueItem;
import org.visallo.core.model.longRunningProcess.LongRunningProcessRepository;
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
import org.visallo.web.clientapi.model.ClientApiLongRunningProcessSubmitResponse;
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

public class VertexCloudImport implements ParameterizedHandler {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(VertexCloudImport.class);

    private final Graph graph;
    private final FileImport fileImport;
    private final WorkspaceRepository workspaceRepository;
    private final LongRunningProcessRepository longRunningProcessRepository;
    private final VisibilityTranslator visibilityTranslator;
    private final WorkspaceHelper workspaceHelper;
    private Authorizations authorizations;

    @Inject
    public VertexCloudImport(
            Graph graph,
            FileImport fileImport,
            WorkspaceRepository workspaceRepository,
            VisibilityTranslator visibilityTranslator,
            LongRunningProcessRepository longRunningProcessRepository,
            WorkspaceHelper workspaceHelper
    ) {
        this.graph = graph;
        this.fileImport = fileImport;
        this.workspaceRepository = workspaceRepository;
        this.longRunningProcessRepository = longRunningProcessRepository;
        this.visibilityTranslator = visibilityTranslator;
        this.workspaceHelper = workspaceHelper;
    }

    @Handle
    public ClientApiLongRunningProcessSubmitResponse handle(
            @Required(name = "cloudResource") String cloudResource,
            @Required(name = "cloudConfiguration") String cloudConfiguration,
            @Optional(name = "publish", defaultValue = "false") boolean shouldPublish,
            @Optional(name = "findExistingByFileHash", defaultValue = "true") boolean findExistingByFileHash,
            @ActiveWorkspaceId String workspaceId,
            Authorizations authorizations,
            User user
    ) throws Exception {
        workspaceId = workspaceHelper.getWorkspaceIdOrNullIfPublish(workspaceId, shouldPublish, user);

        this.authorizations = authorizations;

        CloudImportLongRunningProcessQueueItem item = new CloudImportLongRunningProcessQueueItem(
            cloudResource,
            cloudConfiguration,
            user.getUserId(),
            workspaceId,
            authorizations
        );
        String id = this.longRunningProcessRepository.enqueue(item.toJson(), user, authorizations);

        return new ClientApiLongRunningProcessSubmitResponse(id);
    }

    public Graph getGraph() {
        return graph;
    }

    protected Authorizations getAuthorizations() {
        return authorizations;
    }
}
