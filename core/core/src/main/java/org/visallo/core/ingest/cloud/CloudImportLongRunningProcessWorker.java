package org.visallo.core.ingest.cloud;

import com.google.common.io.ByteProcessor;
import com.google.common.io.ByteStreams;
import com.google.common.io.Closeables;
import com.google.common.io.Files;
import com.google.inject.Inject;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.Vertex;
import org.visallo.core.bootstrap.InjectHelper;
import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.ingest.FileImport;
import org.visallo.core.model.longRunningProcess.LongRunningProcessRepository;
import org.visallo.core.model.longRunningProcess.LongRunningProcessWorker;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.model.workspace.Workspace;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.user.User;
import org.visallo.core.util.ClientApiConverter;
import org.visallo.core.util.JSONUtil;
import org.visallo.web.clientapi.model.ClientApiImportProperty;

import java.io.*;
import java.util.Collection;

public class CloudImportLongRunningProcessWorker extends LongRunningProcessWorker {
    private final Configuration configuration;
    private final FileImport fileImport;
    private final Graph graph;
    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final LongRunningProcessRepository longRunningProcessRepository;

    @Inject
    public CloudImportLongRunningProcessWorker(
            Graph graph,
            Configuration configuration,
            FileImport fileImport,
            UserRepository userRepository,
            WorkspaceRepository workspaceRepository,
            LongRunningProcessRepository longRunningProcessRepository
    ) {
        this.graph = graph;
        this.configuration = configuration;
        this.fileImport = fileImport;
        this.userRepository = userRepository;
        this.workspaceRepository = workspaceRepository;
        this.longRunningProcessRepository = longRunningProcessRepository;
    }

    @Override
    public boolean isHandled(JSONObject longRunningProcessQueueItem) {
        return longRunningProcessQueueItem.getString("type").equals("org-visallo-ingest-cloud");
    }

    @Override
    protected void processInternal(JSONObject longRunningProcessQueueItem) {
        CloudImportLongRunningProcessQueueItem item = ClientApiConverter.toClientApi(longRunningProcessQueueItem, CloudImportLongRunningProcessQueueItem.class);
        CloudResourceSource destination = getDestination(item.getDestination());

        if (destination == null) {
            longRunningProcessQueueItem.put("error", "No cloud destination configured for :" + item.getDestination());
        } else {
            try {
                download(destination, item, longRunningProcessQueueItem);
            } catch (Exception e) {
                throw new VisalloException("Unable to download from cloud destination", e);
            }
        }
    }

    private CloudResourceSource getDestination(String className) {
        Collection<CloudResourceSource> destinations = InjectHelper.getInjectedServices(CloudResourceSource.class, configuration);
        for (CloudResourceSource destination : destinations) {
            if (destination.getClass().getName().equals(className)) {
                return destination;
            }
        }
        return null;
    }

    private void download(CloudResourceSource destination, CloudImportLongRunningProcessQueueItem item, JSONObject itemJson) throws Exception {
        String id = itemJson.getString("id");
        Authorizations authorizations = graph.createAuthorizations(item.getAuthorizations());
        String visibilitySource = "";
        User user = userRepository.findById(item.getUserId());
        String conceptId = null;
        Priority priority = Priority.NORMAL;
        Workspace workspace = workspaceRepository.findById(item.getWorkspaceId(), user);
        ClientApiImportProperty[] properties = null;
        boolean queueDefaults = false;
        boolean findExistingByFileHash = true;

        File tempDir = Files.createTempDir();
        try {
            Collection<CloudResourceSourceItem> items = destination.getItems(new JSONObject(item.getConfiguration()));
            Long allItemsSize = 0L;
            for (CloudResourceSourceItem cloudResourceSourceItem : items) {
                Long size = cloudResourceSourceItem.getSize();
                if (size != null) {
                    allItemsSize += size;
                }
            }


            long noSizeProgress = 0;
            long cumulativeSize = 0;
            for (CloudResourceSourceItem cloudResourceSourceItem : items) {
                String fileName = cloudResourceSourceItem.getName();
                if (fileName == null) throw new VisalloException("Cloud destination item name must not be null");
                File file = new File(tempDir, cloudResourceSourceItem.getName());

                try (InputStream inputStream = cloudResourceSourceItem.getInputStream()) {
                    if (inputStream == null) {
                        throw new VisalloException("Cloud destination input stream must not be null");
                    }

                    noSizeProgress += (double) 1 / items.size();
                    if (downloadFile(id, inputStream, file, cumulativeSize, allItemsSize, noSizeProgress)) {
                        Vertex vertex = fileImport.importFile(
                                file,
                                queueDefaults,
                                conceptId,
                                properties,
                                visibilitySource,
                                workspace,
                                findExistingByFileHash,
                                priority,
                                user,
                                authorizations
                        );

                        JSONArray vertexIds = JSONUtil.getOrCreateJSONArray(itemJson, "vertexIds");
                        vertexIds.put(vertex.getId());
                    }
                    if (allItemsSize > 0) {
                        cumulativeSize += cloudResourceSourceItem.getSize();
                    }
                }
            }
        } finally {
            longRunningProcessRepository.reportProgress(id, 1.0, "Finishing");
            FileUtils.deleteDirectory(tempDir);
        }
    }

    private boolean downloadFile(String longRunningProcessId, InputStream inputStream, File file, Long cumulativeSize, Long allItemSize, double noSizeProgress) throws Exception {
        final OutputStream out = new FileOutputStream(file);
        boolean success = false;
        try {
            ByteStreams.readBytes(inputStream,
                    new ByteProcessor<Object>() {
                        private long progress = cumulativeSize;
                        private long flushProgress = 0;
                        public boolean processBytes(byte[] buffer, int offset, int length) throws IOException {
                            out.write(buffer, offset, length);
                            if (allItemSize > 0) {
                                progress += length;
                                flushProgress += length;
                                if (((double)flushProgress / allItemSize) > 0.01) {
                                    longRunningProcessRepository.reportProgress(longRunningProcessId, (double) progress / allItemSize, String.format("Downloading %s", file.getName()));
                                    flushProgress = 0;
                                }
                            }
                            return true;
                        }
                        public Void getResult() {
                            return null;
                        }
                    });
            success = true;
        } finally {
            if (allItemSize.equals(0)) {
                longRunningProcessRepository.reportProgress(longRunningProcessId, noSizeProgress, "Downloading");
            }
            Closeables.close(out, !success);
        }

        return success;
    }

}
