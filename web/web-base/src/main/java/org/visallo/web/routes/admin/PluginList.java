package org.visallo.web.routes.admin;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import org.json.JSONArray;
import org.json.JSONObject;
import org.visallo.core.bootstrap.lib.LibLoader;
import org.visallo.core.config.Configuration;
import org.visallo.core.ingest.FileImportSupportingFileHandler;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorker;
import org.visallo.core.ingest.graphProperty.PostMimeTypeWorker;
import org.visallo.core.ingest.graphProperty.TermMentionFilter;
import org.visallo.core.model.user.UserListener;
import org.visallo.core.status.StatusServer;
import org.visallo.core.util.ServiceLoaderUtil;
import org.visallo.web.WebAppPlugin;

import java.io.File;

public class PluginList implements ParameterizedHandler {
    private final Configuration configuration;

    @Inject
    public PluginList(Configuration configuration) {
        this.configuration = configuration;
    }

    @Handle
    public JSONObject handle() throws Exception {
        JSONObject json = new JSONObject();

        json.put("loadedLibFiles", getLoadedLibFilesJson());
        json.put("graphPropertyWorkers", getGraphPropertyWorkersJson());
        json.put("postMimeTypeWorkers", getPostMimeTypeWorkersJson());
        json.put("userListeners", getUserListenersJson());
        json.put("libLoaders", getLibLoadersJson());
        json.put("fileImportSupportingFileHandlers", getFileImportSupportingFileHandlersJson());
        json.put("termMentionFilters", getTermMentionFiltersJson());
        json.put("webAppPlugins", getWebAppPluginsJson());

        return json;
    }

    private JSONArray getUserListenersJson() {
        JSONArray json = new JSONArray();
        for (Class<? extends UserListener> userListenerClass : ServiceLoaderUtil.loadClasses(UserListener.class, configuration)) {
            json.put(getUserListenerJson(userListenerClass));
        }
        return json;
    }

    private JSONObject getUserListenerJson(Class<? extends UserListener> userListenerClass) {
        JSONObject json = new JSONObject();
        StatusServer.getGeneralInfo(json, userListenerClass);
        return json;
    }

    private JSONArray getGraphPropertyWorkersJson() {
        JSONArray json = new JSONArray();
        for (Class<? extends GraphPropertyWorker> graphPropertyWorkerClass : ServiceLoaderUtil.loadClasses(GraphPropertyWorker.class, configuration)) {
            json.put(getGraphPropertyWorkerJson(graphPropertyWorkerClass));
        }
        return json;
    }

    private JSONObject getGraphPropertyWorkerJson(Class<? extends GraphPropertyWorker> graphPropertyWorkerClass) {
        JSONObject json = new JSONObject();
        StatusServer.getGeneralInfo(json, graphPropertyWorkerClass);
        return json;
    }

    private JSONArray getPostMimeTypeWorkersJson() {
        JSONArray json = new JSONArray();
        for (Class<? extends PostMimeTypeWorker> postMimeTypeWorkerClass : ServiceLoaderUtil.loadClasses(PostMimeTypeWorker.class, configuration)) {
            json.put(getPostMimeTypeWorkerJson(postMimeTypeWorkerClass));
        }
        return json;
    }

    private JSONObject getPostMimeTypeWorkerJson(Class<? extends PostMimeTypeWorker> postMimeTypeWorkerClass) {
        JSONObject json = new JSONObject();
        StatusServer.getGeneralInfo(json, postMimeTypeWorkerClass);
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
        for (Class<? extends LibLoader> libLoaderClass : ServiceLoaderUtil.loadClasses(LibLoader.class, configuration)) {
            json.put(getLibLoaderJson(libLoaderClass));
        }
        return json;
    }

    private JSONObject getLibLoaderJson(Class<? extends LibLoader> libLoaderClass) {
        JSONObject json = new JSONObject();
        StatusServer.getGeneralInfo(json, libLoaderClass);
        return json;
    }

    private JSONArray getFileImportSupportingFileHandlersJson() {
        JSONArray json = new JSONArray();
        for (Class<? extends FileImportSupportingFileHandler> fileImportSupportingFileHandlerClass : ServiceLoaderUtil.loadClasses(FileImportSupportingFileHandler.class, configuration)) {
            json.put(getFileImportSupportingFileHandlerJson(fileImportSupportingFileHandlerClass));
        }
        return json;
    }

    private JSONObject getFileImportSupportingFileHandlerJson(Class<? extends FileImportSupportingFileHandler> fileImportSupportingFileHandlerClass) {
        JSONObject json = new JSONObject();
        StatusServer.getGeneralInfo(json, fileImportSupportingFileHandlerClass);
        return json;
    }

    private JSONArray getTermMentionFiltersJson() {
        JSONArray json = new JSONArray();
        for (Class<? extends TermMentionFilter> termMentionFilterClass : ServiceLoaderUtil.loadClasses(TermMentionFilter.class, configuration)) {
            json.put(getTermMentionFilterJson(termMentionFilterClass));
        }
        return json;
    }

    private JSONObject getTermMentionFilterJson(Class<? extends TermMentionFilter> termMentionFilterClass) {
        JSONObject json = new JSONObject();
        StatusServer.getGeneralInfo(json, termMentionFilterClass);
        return json;
    }

    private JSONArray getWebAppPluginsJson() {
        JSONArray json = new JSONArray();
        for (Class<? extends WebAppPlugin> webAppPluginClass : ServiceLoaderUtil.loadClasses(WebAppPlugin.class, configuration)) {
            json.put(getWebAppPluginJson(webAppPluginClass));
        }
        return json;
    }

    private JSONObject getWebAppPluginJson(Class<? extends WebAppPlugin> webAppPluginClass) {
        JSONObject json = new JSONObject();
        StatusServer.getGeneralInfo(json, webAppPluginClass);
        return json;
    }
}
