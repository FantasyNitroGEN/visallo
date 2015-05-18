package org.visallo.web.routes.config;

import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.BaseRequestHandler;
import com.v5analytics.webster.HandlerChain;
import com.google.inject.Inject;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;

public class Plugin extends BaseRequestHandler {
    private static final String WEB_PLUGINS_PREFIX = "web.plugins.";
    private static final String DEFAULT_PLUGINS_DIR = "/jsc/configuration/plugins";

    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(Plugin.class);

    @Inject
    public Plugin(
            final UserRepository userRepository,
            final WorkspaceRepository workspaceRepository,
            final org.visallo.core.config.Configuration configuration) {
        super(userRepository, workspaceRepository, configuration);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final String pluginName = getAttributeString(request, "pluginName");
        final String configurationKey = WEB_PLUGINS_PREFIX + pluginName;
        String pluginPath = getConfiguration().get(configurationKey, null);

        // Default behavior if not customized
        if (pluginPath == null) {
            pluginPath = request.getServletContext().getResource(DEFAULT_PLUGINS_DIR + "/" + pluginName).getPath();
        }

        String uri = request.getRequestURI();
        String searchString = "/" + pluginName + "/";
        String pluginResourcePath = uri.substring(uri.indexOf(searchString) + searchString.length());

        if (pluginResourcePath.endsWith(".js")) {
            response.setContentType("application/x-javascript");
        } else if (pluginResourcePath.endsWith(".ejs")) {
            response.setContentType("text/plain");
        } else if (pluginResourcePath.endsWith(".css")) {
            response.setContentType("text/css");
        } else if (pluginResourcePath.endsWith(".html")) {
            response.setContentType("text/html");
        } else {
            LOGGER.error("Only js,ejs,css,html files served from plugin");
            respondWithNotFound(response);
            return;
        }

        String filePath = FilenameUtils.concat(pluginPath, pluginResourcePath);
        File file = new File(filePath);

        if (file.exists()) {
            response.setCharacterEncoding("UTF-8");
            FileUtils.copyFile(file, response.getOutputStream());
        } else {
            respondWithNotFound(response);
        }
    }
}
