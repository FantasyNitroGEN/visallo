package org.visallo.core.util;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloException;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.*;

/**
 * This class exists to provide much deeper and extensive debugging and logging as
 * opposed to (@see java.util.ServiceLoader)
 */
public class ServiceLoaderUtil {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(ServiceLoaderUtil.class);
    private static final String PREFIX = "META-INF/services/";
    public static final String CONFIG_DISABLE_PREFIX = "disable.";

    public static <T> Iterable<T> load(Class<T> clazz, Configuration configuration) {
        Iterable<Class<? extends T>> classes = loadClasses(clazz, configuration);
        return Iterables.transform(classes, new Function<Class<? extends T>, T>() {
            @Nullable
            @Override
            public T apply(Class<? extends T> serviceClass) {
                try {
                    Constructor<? extends T> constructor = serviceClass.getConstructor();
                    return constructor.newInstance();
                } catch (Exception ex) {
                    String errorMessage = String.format("Failed to load %s", serviceClass.getName());
                    LOGGER.error("%s", errorMessage, ex);
                    throw new VisalloException(errorMessage, ex);
                }
            }
        });
    }

    public static <T> Iterable<Class<? extends T>> loadClasses(Class<T> clazz, Configuration configuration) {
        Set<Class<? extends T>> services = new HashSet<>();
        String fullName = PREFIX + clazz.getName();
        LOGGER.debug("loading services for class %s", fullName);
        try {
            Enumeration<URL> serviceFiles = Thread.currentThread().getContextClassLoader().getResources(fullName);
            if (!serviceFiles.hasMoreElements()) {
                LOGGER.debug("Could not find any services for %s", fullName);
            } else {
                Set<URL> serviceFilesSet = new HashSet<>();
                while (serviceFiles.hasMoreElements()) {
                    URL serviceFile = serviceFiles.nextElement();
                    serviceFilesSet.add(serviceFile);
                }

                Map<String, URL> loadedClassNames = new HashMap<>();
                for (URL serviceFile : serviceFilesSet) {
                    List<String> fileClassNames = loadFile(serviceFile);
                    for (String className : fileClassNames) {
                        if (configuration.getBoolean(CONFIG_DISABLE_PREFIX + className, false)) {
                            LOGGER.info("ignoring class %s because it is disabled in configuration", className);
                            continue;
                        }
                        if (loadedClassNames.containsKey(className)) {
                            LOGGER.warn("ignoring class '%s' because it is already loaded from '%s'", className, loadedClassNames.get(className));
                            continue;
                        }
                        services.add(ServiceLoaderUtil.<T>loadClass(serviceFile, className));
                        loadedClassNames.put(className, serviceFile);
                    }
                }
            }

            return services;
        } catch (IOException e) {
            throw new VisalloException("Could not load services for class: " + clazz.getName(), e);
        }
    }

    private static List<String> loadFile(URL serviceFile) throws IOException {
        List<String> results = new ArrayList<>();
        LOGGER.debug("loadFile(%s)", serviceFile);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(serviceFile.openStream()))) {
            String className;
            while ((className = reader.readLine()) != null) {
                className = className.trim();
                if (className.length() == 0) {
                    continue;
                }
                results.add(className);
            }
        }
        return results;
    }

    private static <T> Class<? extends T> loadClass(URL config, String className) {
        try {
            LOGGER.info("Loading %s from %s", className, config.toString());
            return ClassUtil.forName(className);
        } catch (Throwable t) {
            String errorMessage = String.format("Failed to load %s from %s", className, config.toString());
            LOGGER.error("%s", errorMessage, t);
            throw new VisalloException(errorMessage, t);
        }
    }
}
