package org.visallo.web.routes.admin;

import com.google.inject.Inject;
import com.v5analytics.webster.HandlerChain;
import org.visallo.core.bootstrap.lib.LibLoader;
import org.visallo.core.config.Configuration;
import org.visallo.core.ingest.FileImportSupportingFileHandler;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorker;
import org.visallo.core.ingest.graphProperty.PostMimeTypeWorker;
import org.visallo.core.ingest.graphProperty.TermMentionFilter;
import org.visallo.core.model.user.UserListener;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.status.StatusServer;
import org.visallo.web.BaseRequestHandler;
import org.visallo.web.WebAppPlugin;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.ServiceLoader;

public class PluginList extends BaseRequestHandler {
    @Inject
    public PluginList(
            UserRepository userRepository,
            WorkspaceRepository workspaceRepository,
            Configuration configuration) {
        super(userRepository, workspaceRepository, configuration);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        JSONObject json = new JSONObject();

        json.put("loadedLibFiles", getLoadedLibFilesJson());
        json.put("graphPropertyWorkers", getGraphPropertyWorkersJson());
        json.put("postMimeTypeWorkers", getPostMimeTypeWorkersJson());
        json.put("userListeners", getUserListenersJson());
        json.put("libLoaders", getLibLoadersJson());
        json.put("fileImportSupportingFileHandlers", getFileImportSupportingFileHandlersJson());
        json.put("termMentionFilters", getTermMentionFiltersJson());
        json.put("webAppPlugins", getWebAppPluginsJson());

        respondWithJson(response, json);
    }

    private JSONArray getUserListenersJson() {
        JSONArray json = new JSONArray();
        for (UserListener userListener : ServiceLoader.load(UserListener.class)) {
            json.put(getUserListenerJson(userListener));
        }
        return json;
    }

    private JSONObject getUserListenerJson(UserListener userListener) {
        JSONObject json = new JSONObject();
        StatusServer.getGeneralInfo(json, userListener.getClass());
        return json;
    }

    private JSONArray getGraphPropertyWorkersJson() {
        JSONArray json = new JSONArray();
        for (GraphPropertyWorker graphPropertyWorker : ServiceLoader.load(GraphPropertyWorker.class)) {
            json.put(getGraphPropertyWorkerJson(graphPropertyWorker));
        }
        return json;
    }

    private JSONObject getGraphPropertyWorkerJson(GraphPropertyWorker graphPropertyWorker) {
        JSONObject json = new JSONObject();
        StatusServer.getGeneralInfo(json, graphPropertyWorker.getClass());
        return json;
    }

    private JSONArray getPostMimeTypeWorkersJson() {
        JSONArray json = new JSONArray();
        for (PostMimeTypeWorker postMimeTypeWorker : ServiceLoader.load(PostMimeTypeWorker.class)) {
            json.put(getPostMimeTypeWorkerJson(postMimeTypeWorker));
        }
        return json;
    }

    private JSONObject getPostMimeTypeWorkerJson(PostMimeTypeWorker postMimeTypeWorker) {
        JSONObject json = new JSONObject();
        StatusServer.getGeneralInfo(json, postMimeTypeWorker.getClass());
        return json;
    }

    private JSONArray getLoadedLibFilesJson() {
        JSONArray json = new JSONArray();
        for (File loadedLibFile : LibLoader.getLoadedLibFiles()) {
            json.put(getLoadedLibFileJson(loadedLibFile));
        }
        return json;
    }

    private JSONObject getLoadedLibFileJson(File loadedLibFile) {
        JSONObject json = new JSONObject();
        json.put("fileName", loadedLibFile.getAbsolutePath());
        return json;
    }

    private JSONArray getLibLoadersJson() {
        JSONArray json = new JSONArray();
        for (LibLoader libLoader : ServiceLoader.load(LibLoader.class)) {
            json.put(getLibLoaderJson(libLoader));
        }
        return json;
    }

    private JSONObject getLibLoaderJson(LibLoader libLoader) {
        JSONObject json = new JSONObject();
        StatusServer.getGeneralInfo(json, libLoader.getClass());
        return json;
    }

    private JSONArray getFileImportSupportingFileHandlersJson() {
        JSONArray json = new JSONArray();
        for (FileImportSupportingFileHandler fileImportSupportingFileHandler : ServiceLoader.load(FileImportSupportingFileHandler.class)) {
            json.put(getFileImportSupportingFileHandlerJson(fileImportSupportingFileHandler));
        }
        return json;
    }

    private JSONObject getFileImportSupportingFileHandlerJson(FileImportSupportingFileHandler fileImportSupportingFileHandler) {
        JSONObject json = new JSONObject();
        StatusServer.getGeneralInfo(json, fileImportSupportingFileHandler.getClass());
        return json;
    }

    private JSONArray getTermMentionFiltersJson() {
        JSONArray json = new JSONArray();
        for (TermMentionFilter termMentionFilter : ServiceLoader.load(TermMentionFilter.class)) {
            json.put(getTermMentionFilterJson(termMentionFilter));
        }
        return json;
    }

    private JSONObject getTermMentionFilterJson(TermMentionFilter termMentionFilter) {
        JSONObject json = new JSONObject();
        StatusServer.getGeneralInfo(json, termMentionFilter.getClass());
        return json;
    }

    private JSONArray getWebAppPluginsJson() {
        JSONArray json = new JSONArray();
        for (WebAppPlugin webAppPlugin : ServiceLoader.load(WebAppPlugin.class)) {
            json.put(getWebAppPluginJson(webAppPlugin));
        }
        return json;
    }

    private JSONObject getWebAppPluginJson(WebAppPlugin webAppPlugin) {
        JSONObject json = new JSONObject();
        StatusServer.getGeneralInfo(json, webAppPlugin.getClass());
        return json;
    }
}
