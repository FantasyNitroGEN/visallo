package org.visallo.web.routes.config;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Required;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.visallo.core.exception.VisalloResourceNotFoundException;
import org.visallo.web.VisalloResponse;

import javax.servlet.http.HttpServletRequest;
import java.io.File;

public class Plugin implements ParameterizedHandler {
    private static final String WEB_PLUGINS_PREFIX = "web.plugins.";
    private static final String DEFAULT_PLUGINS_DIR = "/jsc/configuration/plugins";
    private final org.visallo.core.config.Configuration configuration;

    @Inject
    public Plugin(org.visallo.core.config.Configuration configuration) {
        this.configuration = configuration;
    }

    @Handle
    public void handle(
            HttpServletRequest request,
            @Required(name = "pluginName") String pluginName,
            VisalloResponse response
    ) throws Exception {
        final String configurationKey = WEB_PLUGINS_PREFIX + pluginName;
        String pluginPath = configuration.get(configurationKey, null);

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
            throw new VisalloResourceNotFoundException("Only js,ejs,css,html files served from plugin");
        }

        String filePath = FilenameUtils.concat(pluginPath, pluginResourcePath);
        File file = new File(filePath);

        if (!file.exists()) {
            throw new VisalloResourceNotFoundException("Could not find file: " + filePath);
        }

        response.setCharacterEncoding("UTF-8");
        FileUtils.copyFile(file, response.getOutputStream());
    }
}
