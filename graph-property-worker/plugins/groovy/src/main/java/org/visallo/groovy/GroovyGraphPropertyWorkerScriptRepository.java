package org.visallo.groovy;

import com.google.inject.Inject;
import groovy.lang.GroovyShell;
import groovy.lang.MetaMethod;
import groovy.lang.Script;
import org.vertexium.Element;
import org.vertexium.Property;
import org.visallo.core.config.Configuration;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorkData;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class GroovyGraphPropertyWorkerScriptRepository {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(GroovyGraphPropertyWorkerScriptRepository.class);
    public static final String CONFIG_SCRIPT_DIR = GroovyGraphPropertyWorker.class.getName() + ".scriptDir";
    public static final String CONFIG_REFRESH_INTERVAL_MS = GroovyGraphPropertyWorker.class.getName() + ".refreshIntervalMs";
    public static final int CONFIG_REFRESH_INTERVAL_MS_DEFAULT = 1000;
    private final long refreshInterval;
    private final File scriptDir;
    private long lastRefreshTime;
    private Map<File, ScriptData> scripts = new HashMap<>();

    @Inject
    public GroovyGraphPropertyWorkerScriptRepository(
            Configuration configuration
    ) {
        refreshInterval = configuration.getInt(CONFIG_REFRESH_INTERVAL_MS, CONFIG_REFRESH_INTERVAL_MS_DEFAULT);
        String scriptDirStr = configuration.get(CONFIG_SCRIPT_DIR, null);
        checkNotNull(scriptDirStr, CONFIG_SCRIPT_DIR + " is required configuration parameter");
        scriptDir = new File(scriptDirStr);
    }

    public void refreshScripts() {
        if (!isReadyToRefresh()) {
            return;
        }
        List<File> filesToRemove = new ArrayList<>(scripts.keySet());
        refreshScripts(filesToRemove, scriptDir);
        removeScriptsThatAreNoLongerPresent(filesToRemove);
        lastRefreshTime = System.currentTimeMillis();
    }

    private void removeScriptsThatAreNoLongerPresent(List<File> filesToRemove) {
        for (File f : filesToRemove) {
            LOGGER.info("removing script: %s", f.getAbsolutePath());
            scripts.remove(f);
        }
    }

    private boolean isReadyToRefresh() {
        return lastRefreshTime + refreshInterval <= System.currentTimeMillis();
    }

    private void refreshScripts(List<File> filesToRemove, File file) {
        if (file.isFile()) {
            if (!file.getAbsolutePath().endsWith(".groovy")) {
                return;
            }
            try {
                refreshScript(file);
                filesToRemove.remove(file);
            } catch (Exception e) {
                LOGGER.error("Could not load script: " + file.getAbsolutePath(), e);
            }
        } else if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children == null) {
                return;
            }
            for (File child : children) {
                refreshScripts(filesToRemove, child);
            }
        }
    }

    private void refreshScript(File file) throws IOException {
        ScriptData scriptData = scripts.get(file);
        if (!isNew(file, scriptData)) {
            return;
        }
        scriptData = loadScript(file);
        scripts.put(file, scriptData);
    }

    private ScriptData loadScript(File file) throws IOException {
        ScriptData scriptData;
        LOGGER.info("Loading %s", file.getAbsolutePath());
        GroovyShell shell = new GroovyShell();
        Script script = shell.parse(file);
        validateScript(script);
        scriptData = new ScriptData(file, script);
        return scriptData;
    }

    private boolean isNew(File file, ScriptData scriptData) {
        if (scriptData == null) {
            return true;
        }
        return scriptData.getModifiedTime() < file.lastModified();
    }

    private void validateScript(Script script) {
        MetaMethod isHandledMethod = script.getMetaClass().getMetaMethod("isHandled", new Object[]{Element.class, Property.class});
        checkNotNull(isHandledMethod, "Could not find isHandled(Element, Property) method in script");
        MetaMethod executeMethod = script.getMetaClass().getMetaMethod("execute", new Object[]{InputStream.class, GraphPropertyWorkData.class});
        checkNotNull(executeMethod, "Could not find execute(InputStream, GraphPropertyWorkData) method in script");
    }

    public Iterable<ScriptData> getScriptDatas() {
        return this.scripts.values();
    }
}
