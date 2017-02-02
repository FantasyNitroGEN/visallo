package org.visallo.core.config;

import com.google.common.base.Throwables;
import org.apache.log4j.xml.DOMConfigurator;
import org.json.JSONObject;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.exception.VisalloResourceNotFoundException;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

import java.io.File;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public abstract class ConfigurationLoader {
    public static final String ENV_CONFIGURATION_LOADER = "VISALLO_CONFIGURATION_LOADER";
    private static Configuration configuration;
    private final Map initParameters;
    private JSONObject configurationInfo = new JSONObject();

    protected ConfigurationLoader(Map initParameters) {
        this.initParameters = initParameters;
    }

    public static void configureLog4j() {
        ConfigurationLoader configurationLoader = createConfigurationLoader();
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
        ConfigurationLoader configurationLoader = createConfigurationLoader(configLoader, initParameters);
        if (configuration == null) {
            configuration = configurationLoader.createConfiguration();
        }

        // This load method overload is at the bottom of the call hierarchy and is the only place guaranteed
        // to get called while loading configuration. It is also early enough in the startup process (ie before
        // SSL connection to databases or data stores are made) to set system properties and have them take effect.
        setSystemProperties(configuration);

        return configuration;
    }

    private static void setSystemProperties(Configuration configuration) {
        Map<String, String> systemProperties = configuration.getSubset("systemProperty");
        for (Map.Entry<String, String> systemProperty : systemProperties.entrySet()) {
            System.setProperty(systemProperty.getKey(), systemProperty.getValue());
        }
    }

    private static ConfigurationLoader createConfigurationLoader() {
        return createConfigurationLoader(null, null);
    }

    private static ConfigurationLoader createConfigurationLoader(Class configLoaderClass, Map initParameters) {
        if (configLoaderClass == null) {
            configLoaderClass = getConfigurationLoaderClass();
        }
        if (initParameters == null) {
            initParameters = new HashMap<>();
        }

        try {
            @SuppressWarnings("unchecked") Constructor constructor = configLoaderClass.getConstructor(Map.class);
            return (ConfigurationLoader) constructor.newInstance(initParameters);
        } catch (Exception e) {
            throw new VisalloException("Could not load configuration class: " + configLoaderClass.getName(), e);
        }
    }

    public abstract Configuration createConfiguration();

    protected void doConfigureLog4j() {
        String fileName = System.getProperty("logQuiet") == null ? "log4j.xml" : "log4j-quiet.xml";
        File log4jFile = null;
        String log4jLocation = null;
        try {
            log4jFile = resolveFileName(fileName);
        } catch (VisalloResourceNotFoundException e) {
            // OK, try classpath
        }
        if (log4jFile == null || !log4jFile.exists()) {
            try {
                URL log4jResource = getClass().getResource(fileName);
                System.err.println("Could not resolve log4j.xml, using the fallback: " + log4jResource);
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
