package org.visallo.web.routes;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.io.ServletContextTemplateLoader;
import com.github.jknack.handlebars.io.TemplateLoader;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import org.apache.commons.io.IOUtils;
import org.visallo.core.config.Configuration;
import org.visallo.web.WebApp;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import static com.google.common.base.Preconditions.checkNotNull;

public class Index implements ParameterizedHandler {
    private static final String PLUGIN_JS_RESOURCES_BEFORE_AUTH_PARAM = "pluginJsResourcesBeforeAuth";
    private static final String PLUGIN_JS_RESOURCES_WEB_WORKER_PARAM = "pluginJsResourcesWebWorker";
    private static final String PLUGIN_JS_RESOURCES_AFTER_AUTH_PARAM = "pluginJsResourcesAfterAuth";
    private static final String PLUGIN_CSS_RESOURCES_PARAM = "pluginCssResources";
    private static final String LOGO_IMAGE_DATA_URI = "logoDataUri";
    private static final String LOGO_PATH_BUNDLE_KEY = "visallo.loading-logo.path";
    private static final Map<String, String> MESSAGE_BUNDLE_PARAMS = ImmutableMap.of(
            "title", "visallo.title",
            "description", "visallo.description"
    );

    private String indexHtml;

    @Handle
    public void handle(
            WebApp webApp,
            ResourceBundle resourceBundle,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws Exception {
        response.setContentType("text/html");
        response.getWriter().write(getIndexHtml(request, webApp, resourceBundle));
    }

    private String getIndexHtml(HttpServletRequest request, WebApp app, ResourceBundle resourceBundle) throws IOException {
        if (indexHtml == null || app.isDevModeEnabled()) {
            Map<String, Object> context = new HashMap<>();
            context.put(PLUGIN_JS_RESOURCES_BEFORE_AUTH_PARAM, app.getPluginsJsResourcesBeforeAuth());
            context.put(PLUGIN_JS_RESOURCES_WEB_WORKER_PARAM, app.getPluginsJsResourcesWebWorker());
            context.put(PLUGIN_JS_RESOURCES_AFTER_AUTH_PARAM, app.getPluginsJsResourcesAfterAuth());
            context.put(PLUGIN_CSS_RESOURCES_PARAM, app.getPluginsCssResources());
            context.put(LOGO_IMAGE_DATA_URI, getLogoImageDataUri(request, resourceBundle));
            for (Map.Entry<String, String> param : MESSAGE_BUNDLE_PARAMS.entrySet()) {
                context.put(param.getKey(), resourceBundle.getString(param.getValue()));
            }
            TemplateLoader templateLoader = new ServletContextTemplateLoader(request.getServletContext(), "/", ".hbs");
            Handlebars handlebars = new Handlebars(templateLoader);
            Template template = handlebars.compile("index");
            indexHtml = template.apply(context);
        }
        return indexHtml;
    }

    private String getLogoImageDataUri(HttpServletRequest request, ResourceBundle resourceBundle) throws IOException {
        String logoPathBundleKey = resourceBundle.getString(LOGO_PATH_BUNDLE_KEY);
        checkNotNull(logoPathBundleKey, LOGO_PATH_BUNDLE_KEY + " configuration not found");
        try (InputStream is = getResourceAsStream(request, logoPathBundleKey)) {
            checkNotNull(is, logoPathBundleKey + " resource not found");
            byte[] bytes = IOUtils.toByteArray(is);
            return "data:image/png;base64," + DatatypeConverter.printBase64Binary(bytes);
        }
    }

    private InputStream getResourceAsStream(HttpServletRequest request, String path) {
        InputStream is = request.getServletContext().getResourceAsStream(path);
        if (is == null) {
            is = getClass().getResourceAsStream(path);
        }
        return is;
    }
}
