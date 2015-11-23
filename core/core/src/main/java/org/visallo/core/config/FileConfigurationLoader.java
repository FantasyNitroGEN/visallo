package org.visallo.core.config;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.exception.VisalloResourceNotFoundException;
import org.visallo.core.util.ProcessUtil;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

/**
 * Searches for visallo configuration directories in this order:
 * - ${ENV_VISALLO_DIR}
 * - ${user.home}/.visallo
 * - ${appdata}/Visallo
 * - DEFAULT_UNIX_LOCATION or DEFAULT_WINDOWS_LOCATION
 */
public class FileConfigurationLoader extends ConfigurationLoader {
    /**
     * !!! DO NOT DEFINE A LOGGER here. This class get loaded very early in the process and we don't want to the logger to be initialized yet **
     */
    public static final String ENV_VISALLO_DIR = "VISALLO_DIR";
    public static final String DEFAULT_UNIX_LOCATION = "/opt/visallo/";
    public static final String DEFAULT_WINDOWS_LOCATION = "c:/opt/visallo/";

    public FileConfigurationLoader(Map initParameters) {
        super(initParameters);
    }

    public Configuration createConfiguration() {
        final Map<String, String> properties = new HashMap<>();
        List<File> configDirectories = getVisalloDirectoriesFromLeastPriority("config");
        if (configDirectories.size() == 0) {
            throw new VisalloException("Could not find any valid config directories.");
        }
        List<String> loadedFiles = new ArrayList<>();
        for (File directory : configDirectories) {
            Map<String, String> directoryProperties = loadDirectory(directory, loadedFiles);
            properties.putAll(directoryProperties);
        }
        setConfigurationInfo("loadedFiles", loadedFiles);
        return new Configuration(this, properties);
    }

    public static List<File> getVisalloDirectoriesFromMostPriority(String subDirectory) {
        return Lists.reverse(getVisalloDirectoriesFromLeastPriority(subDirectory));
    }

    public static List<File> getVisalloDirectoriesFromLeastPriority(String subDirectory) {
        List<File> results = new ArrayList<>();

        String currentDir = Paths.get(".").toAbsolutePath().normalize().toString();
        addVisalloSubDirectory(results, currentDir, subDirectory);

        if (ProcessUtil.isWindows()) {
            addVisalloSubDirectory(results, DEFAULT_WINDOWS_LOCATION, subDirectory);
        } else {
            addVisalloSubDirectory(results, DEFAULT_UNIX_LOCATION, subDirectory);
        }

        String appData = System.getProperty("appdata");
        if (appData != null && appData.length() > 0) {
            addVisalloSubDirectory(results, new File(new File(appData), "Visallo").getAbsolutePath(), subDirectory);
        }

        String userHome = System.getProperty("user.home");
        if (userHome != null && userHome.length() > 0) {
            addVisalloSubDirectory(results, new File(new File(userHome), ".visallo").getAbsolutePath(), subDirectory);
        }

        addVisalloSubDirectory(results, System.getenv(ENV_VISALLO_DIR), subDirectory);

        return ImmutableList.copyOf(results);
    }

    private static void addVisalloSubDirectory(List<File> results, String location, String subDirectory) {
        if (location == null || location.trim().length() == 0) {
            return;
        }

        location = location.trim();
        if (location.startsWith("file://")) {
            location = location.substring("file://".length());
        }

        File dir = new File(new File(location), subDirectory);
        if (!dir.exists()) {
            return;
        }

        results.add(dir);
    }

    private static Map<String, String> loadDirectory(File configDirectory, List<String> loadedFiles) {
        VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(FileConfigurationLoader.class);

        LOGGER.debug("Attempting to load configuration from directory: %s", configDirectory);
        if (!configDirectory.exists()) {
            throw new VisalloException("Could not find config directory: " + configDirectory);
        }

        File[] files = configDirectory.listFiles();
        if (files == null) {
            throw new VisalloException("Could not parse directory name: " + configDirectory);
        }
        Arrays.sort(files, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        Map<String, String> properties = new HashMap<>();
        for (File f : files) {
            if (!f.getAbsolutePath().endsWith(".properties")) {
                continue;
            }
            try {
                Map<String, String> fileProperties = loadFile(f.getAbsolutePath(), loadedFiles);
                for (Map.Entry<String, String> filePropertyEntry : fileProperties.entrySet()) {
                    properties.put(filePropertyEntry.getKey(), filePropertyEntry.getValue());
                }
            } catch (IOException ex) {
                throw new VisalloException("Could not load config file: " + f.getAbsolutePath(), ex);
            }
        }

        return properties;
    }

    private static Map<String, String> loadFile(final String fileName, List<String> loadedFiles) throws IOException {
        VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(FileConfigurationLoader.class);

        Map<String, String> results = new HashMap<>();
        LOGGER.info("Loading config file: %s", fileName);
        try (FileInputStream in = new FileInputStream(fileName)) {
            Properties properties = new Properties();
            properties.load(in);
            for (Map.Entry<Object, Object> prop : properties.entrySet()) {
                String key = prop.getKey().toString();
                String value = prop.getValue().toString();
                results.put(key, value);
            }
            loadedFiles.add(fileName);
        } catch (Exception e) {
            LOGGER.info("Could not load configuration file: %s", fileName);
        }
        return results;
    }

    @Override
    public File resolveFileName(String fileName) {
        return resolveLocalFileName(fileName);
    }

    public static File resolveLocalFileName(String fileName) {
        List<File> configDirectories = getVisalloDirectoriesFromMostPriority("config");
        if (configDirectories.size() == 0) {
            throw new VisalloResourceNotFoundException("Could not find any valid config directories.");
        }
        for (File directory : configDirectories) {
            File f = new File(directory, fileName);
            if (f.exists()) {
                return f;
            }
        }
        throw new VisalloException("Could not find file: " + fileName);
    }
}
