package org.visallo.web.routes.userGuide;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Required;
import org.visallo.core.config.Configuration;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.VisalloResponse;
import org.visallo.web.parameterProviders.BaseUrl;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class UserGuideShortUrl implements ParameterizedHandler {
    public static final String CONTEXT_PATH = "/ug";
    public static final String SHORT_URL_PROPERTIES_FILENAME = UserGuide.USER_GUIDE_CLASSPATH_LOCATION + "/short-url.properties";
    public static final String CONFIGURATION_PREFIX_FOR_SHORT_URLS = "userGuide.short-url";
    public static final String CONFIGURATION_KEY_FOR_DEFAULT_VALUE = "userGuide.short-url-default";
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(UserGuideShortUrl.class);
    private Map<String, String> map;
    private String defaultValue;

    @Inject
    public UserGuideShortUrl(Configuration configuration) {
        map = loadShortUrlsFromClasspath();
        map.putAll(configuration.getSubset(CONFIGURATION_PREFIX_FOR_SHORT_URLS));
        defaultValue = configuration.get(CONFIGURATION_KEY_FOR_DEFAULT_VALUE, UserGuide.INDEX);
    }

    @Handle
    public void handle(
            @BaseUrl String baseUrl,
            @Required(name = "key") String key,
            VisalloResponse response
    ) throws IOException {
        String value = map.get(key);
        if (value == null) {
            value = defaultValue;
            LOGGER.warn("no value found for short url key: %s, redirecting to default: %s", key, value);
        }
        if (value.startsWith("/")) {
            value = baseUrl + value;
        }
        response.getHttpServletResponse().sendRedirect(value);
    }

    private Map<String, String> loadShortUrlsFromClasspath() {
        Map<String, String> map = new HashMap<>();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Set<String> propertiesFileResources = ClasspathResourceUtils.findClasspathResources(classLoader, SHORT_URL_PROPERTIES_FILENAME);
        for (String propertiesFileResource : propertiesFileResources) {
            try (InputStream in = classLoader.getResourceAsStream(propertiesFileResource)) {
                Properties properties = new Properties();
                properties.load(in);
                for (Map.Entry<Object, Object> property : properties.entrySet()) {
                    String key = property.getKey().toString();
                    String value = property.getValue().toString();
                    if (value.startsWith("/")) {
                        value = UserGuide.CONTEXT_PATH + value;
                    }
                    map.put(key, value);
                }
            } catch (IOException ex) {
                LOGGER.warn("error loading properties file: %s", propertiesFileResource, ex);
            }
        }
        return map;
    }
}
