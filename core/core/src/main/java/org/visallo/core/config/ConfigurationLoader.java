package org.visallo.core.config;

import com.google.common.base.Throwables;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.exception.VisalloResourceNotFoundException;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.apache.log4j.xml.DOMConfigurator;
import org.json.JSONObject;

import java.io.File;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public abstract class ConfigurationLoader {
    public static final String ENV_CONFIGURATION_LOADER = "VISALLO_CONFIGURATION_LOADER";
    protected static ConfigurationLoader configurationLoader;
    private static Configuration configuration;
    private final Map initParameters;
    private JSONObject configurationInfo = new JSONObject();

    protected ConfigurationLoader(Map initParameters) {
        this.initParameters = initParameters;
    }

    public static void configureLog4j() {
        ConfigurationLoader configurationLoader = getOrCreateConfigurationLoader();
        configurationLoader.doConfigureLog4j();
    }

    public static Configuration load() {
        return load(new HashMap());
    }

    public static Configuration load(Map p) {
        return load(getConfigurationLoaderClass(), p);
    }

    public static Class getConfigurationLoaderClass() {
        String configLoaderName = System.getenv(ENV_CONFIGURATION_LOADER);
        if (configLoaderName == null) {
            configLoaderName = System.getProperty(ENV_CONFIGURATION_LOADER);
        }
        if (configLoaderName != null) {
            return getConfigurationLoaderByName(configLoaderName);
        }

        return FileConfigurationLoader.class;
    }

    public static Configuration load(String configLoaderName, Map<String, String> initParameters) {
        Class configLoader;
        if (configLoaderName == null) {
            configLoader = getConfigurationLoaderClass();
        } else {
            configLoader = getConfigurationLoaderByName(configLoaderName);
        }
        return load(configLoader, initParameters);
    }

    public static Class getConfigurationLoaderByName(String configLoaderName) {
        Class configLoader;
        try {
            configLoader = Class.forName(configLoaderName);
        } catch (ClassNotFoundException e) {
            throw new VisalloException("Could not load class " + configLoaderName, e);
        }
        return configLoader;
    }

    public static Configuration load(Class configLoader, Map initParameters) {
        ConfigurationLoader configurationLoader = getOrCreateConfigurationLoader(configLoader, initParameters);
        if (configuration == null) {
            configuration = configurationLoader.createConfiguration();
        }
        return configuration;
    }

    private static ConfigurationLoader getOrCreateConfigurationLoader() {
        return getOrCreateConfigurationLoader(null, null);
    }

    private static ConfigurationLoader getOrCreateConfigurationLoader(Class configLoaderClass, Map initParameters) {
        if (configurationLoader != null) {
            return configurationLoader;
        }

        if (configLoaderClass == null) {
            configLoaderClass = getConfigurationLoaderClass();
        }
        if (initParameters == null) {
            initParameters = new HashMap<>();
        }

        try {
            @SuppressWarnings("unchecked") Constructor constructor = configLoaderClass.getConstructor(Map.class);
            configurationLoader = (ConfigurationLoader) constructor.newInstance(initParameters);
        } catch (Exception e) {
            throw new VisalloException("Could not load configuration class: " + configLoaderClass.getName(), e);
        }
        return configurationLoader;
    }

    public abstract Configuration createConfiguration();

    protected void doConfigureLog4j() {
        File log4jFile = null;
        String log4jLocation = null;
        try {
            log4jFile = resolveFileName("log4j.xml");
        } catch (VisalloResourceNotFoundException e) {
            // OK, try classpath
        }
        if (log4jFile == null || !log4jFile.exists()) {
            try {
                String fileName = System.getProperty("logQuiet") == null ? "log4j.xml" : "log4j-quiet.xml";
                URL log4jResource = getClass().getResource(fileName);
                if (log4jResource != null) {
                    DOMConfigurator.configure(log4jResource);
                    log4jLocation = log4jResource.toExternalForm();
                } else {
                    throw new VisalloResourceNotFoundException("Could not find log4j.xml on the classpath");
                }
            } catch (RuntimeException e) {
                Throwables.propagate(e);
            }
        } else {
            log4jLocation = log4jFile.getAbsolutePath();
            DOMConfigurator.configure(log4jFile.getAbsolutePath());
        }
        VisalloLogger logger = VisalloLoggerFactory.getLogger(VisalloLoggerFactory.class);
        logger.info("Using ConfigurationLoader: %s", this.getClass().getName());
        logger.info("Using log4j.xml: %s", log4jLocation);
    }

    public abstract File resolveFileName(String fileName);

    protected Map getInitParameters() {
        return initParameters;
    }

    protected void setConfigurationInfo(String key, Object object) {
        configurationInfo.put(key, object);
    }

    public JSONObject getConfigurationInfo() {
        return configurationInfo;
    }
}
