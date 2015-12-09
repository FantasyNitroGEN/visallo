package org.visallo.web.routes.userGuide;

import com.google.common.io.Files;
import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Required;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.util.ProcessRunner;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.VisalloResponse;

import javax.servlet.ServletOutputStream;
import java.io.*;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UserGuide implements ParameterizedHandler {
    public static final String CONTEXT_PATH = "/user-guide";
    public static final String USER_GUIDE_CLASSPATH_LOCATION = "META-INF/user-guide";
    public static final String CONFIGURATION_ENABLED = Configuration.WEB_CONFIGURATION_PREFIX + "userGuide.enabled";
    public static final String INDEX = CONTEXT_PATH + "/index.html";
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(UserGuide.class);
    private File renderedBookDir;
    private boolean enabled;

    @Inject
    public UserGuide(Configuration configuration) {
        enabled = configuration.getBoolean(CONFIGURATION_ENABLED, true);
        if (enabled) {
            try {
                File bookSourceDir = copyMetaInfUserGuideToTempDirectory();
                updateMasterSummary(bookSourceDir);
                renderedBookDir = buildBook(bookSourceDir);
            } catch (Exception ex) {
                LOGGER.warn("failed to render User Guide, " + CONFIGURATION_ENABLED + " will be set to false", ex);
                configuration.set(CONFIGURATION_ENABLED, false);
                enabled = false;
            }
        }
    }

    @Handle
    public void handle(
            @Required(name = "path") String path,
            VisalloResponse response
    ) throws IOException, URISyntaxException, InterruptedException, ExecutionException {
            if (enabled) {
                File resourceFile = new File(renderedBookDir, path);
                if (resourceFile.isDirectory()) {
                    resourceFile = new File(resourceFile, "index.html");
                }
                try (InputStream in = new FileInputStream(resourceFile)) {
                    try (ServletOutputStream out = (ServletOutputStream) response.getOutputStream()) {
                        IOUtils.copy(in, out);
                    }
                }
            } else {
                response.respondWithHtml("The User Guild is not available.");
            }
    }

    private void updateMasterSummary(File bookSourceDir) throws IOException {
        File masterSummaryFile = new File(bookSourceDir, "SUMMARY.md");
        List<String> masterSummaryLines = FileUtils.readLines(masterSummaryFile);
        for (File file : bookSourceDir.listFiles()) {
            if (file.getName().startsWith("SUMMARY-")) {
                mergeSummaryFile(masterSummaryLines, FileUtils.readLines(file));
            }
        }
        FileUtils.writeLines(masterSummaryFile, masterSummaryLines);
    }

    static void mergeSummaryFile(List<String> masterLines, List<String> lines) {
        int nextInsertLocation = 0;
        for (String line : lines) {
            int i = indexOf(masterLines, line);
            if (i >= 0) {
                nextInsertLocation = i + 1;
            } else {
                masterLines.add(nextInsertLocation++, line);
            }
        }
    }

    private static int indexOf(List<String> lines, String toBeFoundString) {
        toBeFoundString = stripExtra(toBeFoundString);
        for (int i = 0; i < lines.size(); i++) {
            String line = stripExtra(lines.get(i));
            if (line.equals(toBeFoundString)) {
                return i;
            }
        }
        return -1;
    }

    private static String stripExtra(String line) {
        Pattern p = Pattern.compile("(\\s*)\\* \\[(.*?)\\]\\(.*\\)");
        Matcher m = p.matcher(line);
        if (m.matches()) {
            return m.group(1) + "* " + m.group(2);
        }
        return line;
    }

    private File buildBook(File bookSourceDir) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        File renderedBookDir = new File(bookSourceDir, "_book");
        try {
            new ProcessRunner().execute(bookSourceDir, "node", new String[]{"./node_modules/gitbook-cli/bin/gitbook.js", "build"}, out, "gitbook");
            markFileDeleteOnExit(renderedBookDir);
        } catch (Exception e) {
            throw new VisalloException("error building User Guide", e);
        } finally {
            LOGGER.debug("build output: %s", new String(out.toByteArray()));
        }
        return renderedBookDir;
    }

    private void markFileDeleteOnExit(File file) {
        file.deleteOnExit();
        if (file.isDirectory()) {
            for (File f : file.listFiles()) {
                markFileDeleteOnExit(f);
            }
        }
    }

    private File copyMetaInfUserGuideToTempDirectory() throws IOException, URISyntaxException {
        File tempDir = Files.createTempDir();
        tempDir.deleteOnExit();
        LOGGER.info("copying User Guide source files from %s to %s", USER_GUIDE_CLASSPATH_LOCATION, tempDir.getAbsolutePath());

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Set<String> userGuideResources = ClasspathResourceUtils.findClasspathResources(classLoader, USER_GUIDE_CLASSPATH_LOCATION);
        for (String resource : userGuideResources) {
            String outFileName = resource.substring((USER_GUIDE_CLASSPATH_LOCATION + "/").length());
            File outFile = new File(tempDir, outFileName);
            outFile.getParentFile().mkdirs();
            try (InputStream in = classLoader.getResourceAsStream(resource)) {
                try (OutputStream out = new FileOutputStream(outFile)) {
                    IOUtils.copy(in, out);
                }
            }
        }

        return tempDir;
    }
}
