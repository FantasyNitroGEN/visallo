package org.visallo.quickStart;

import com.google.common.collect.Multimap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.vfs.Vfs;
import org.visallo.core.exception.VisalloException;

import java.io.*;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class QuickStartWorkingDirectory {
    private final File workingDirectory;

    public QuickStartWorkingDirectory(File workingDirectoryOverride) throws IOException {
        if (workingDirectoryOverride == null) {
            this.workingDirectory = new File(new File(System.getProperty("java.io.tmpdir")), "visallo-quick-start");
        } else {
            this.workingDirectory = workingDirectoryOverride;
        }
        initialize();
    }

    private void initialize() throws IOException {
        File visalloInitializedFile = new File(workingDirectory, ".visallo-initialized");
        if (!visalloInitializedFile.exists()) {
            System.out.println("Initializing " + workingDirectory.getAbsolutePath() + "...");
            workingDirectory.mkdirs();

            copyResourcesToWorkingDirectory();
            updateLogDirInLog4jXml();
            expandWebApp(new File(workingDirectory, "webapp"));

            IOUtils.write("complete", new FileOutputStream(visalloInitializedFile));
            System.out.println("Initialized " + workingDirectory.getAbsolutePath());
        } else {
            System.out.println("Skipping initialization of " + workingDirectory.getAbsolutePath() + ", already initialized");
        }
    }

    private void updateLogDirInLog4jXml() {
        File log4jXmlFile = getFile("config/log4j.xml");
        try {
            File logsDir = getFile("logs");
            System.out.println("Logging to: " + logsDir.getAbsolutePath());
            logsDir.mkdirs();

            String contents = FileUtils.readFileToString(log4jXmlFile);
            contents = contents.replaceAll("%LOG_DIR%", logsDir.getAbsolutePath());
            FileUtils.writeStringToFile(log4jXmlFile, contents);
        } catch (Exception ex) {
            throw new VisalloException("Could not update: " + log4jXmlFile.getAbsolutePath(), ex);
        }
    }

    protected void copyResourcesToWorkingDirectory() {
        Reflections reflections = new Reflections(
                new ConfigurationBuilder()
                        .setScanners(new ResourcesScanner())
                        .setUrls(ClasspathHelper.forClassLoader(QuickStartJettyWebServer.class.getClassLoader()))
        );
        Multimap<String, String> resources = reflections.getStore().get(ResourcesScanner.class.getSimpleName());
        for (Map.Entry<String, String> entry : resources.entries()) {
            copyResourceTo(entry.getKey(), workingDirectory);
        }
    }

    private static File copyResourceTo(String resourceName, File directory) {
        String prefix = QuickStartJettyWebServer.class.getPackage().getName().replace('.', '/') + "/";
        String destRelativePath = resourceName.substring(prefix.length());
        resourceName = "/" + resourceName;

        File destFile = new File(directory, destRelativePath);
        if (destFile.exists()) {
            return destFile;
        }
        destFile.getParentFile().mkdirs();
        try {
            try (InputStream in = QuickStartJettyWebServer.class.getResourceAsStream(resourceName)) {
                if (in == null) {
                    throw new VisalloException("Could not find resource: " + resourceName);
                }
                try (OutputStream out = new FileOutputStream(destFile)) {
                    IOUtils.copy(in, out);
                }
            }
        } catch (IOException ex) {
            throw new VisalloException("Could not read resource: " + resourceName, ex);
        }
        return destFile;
    }

    private static void expandWebApp(File destDir) {
        destDir.mkdirs();
        String prefix = "/" + QuickStartJettyWebServer.class.getPackage().getName().replace('.', '/') + "/";
        String path = prefix + "webapp.zip";
        try {
            try (InputStream in = QuickStartJettyWebServer.class.getResourceAsStream(path)) {
                if (in == null) {
                    throw new VisalloException("Could not find resource: " + path);
                }
                try (ZipInputStream zin = new ZipInputStream(in)) {
                    ZipEntry ze;
                    while ((ze = zin.getNextEntry()) != null) {
                        if (!ze.isDirectory()) {
                            File outputFile = new File(destDir, ze.getName());
                            outputFile.getParentFile().mkdirs();
                            try (FileOutputStream outFile = new FileOutputStream(outputFile)) {
                                IOUtils.copy(zin, outFile);
                                zin.closeEntry();
                            }
                        }
                    }
                }
            }
        } catch (IOException ex) {
            throw new VisalloException("Could not expand webapp.zip: " + path, ex);
        }
    }

    public File getFile(String relativePath) {
        return new File(workingDirectory, relativePath);
    }

    public File getDirectory() {
        return workingDirectory;
    }

    private static class ResourcesScanner extends org.reflections.scanners.ResourcesScanner {
        String match;

        public ResourcesScanner() {
            this.match = QuickStartJettyWebServer.class.getPackage().getName().replace('.', '/');
        }

        @Override
        public boolean acceptsInput(String file) {
            if (file.endsWith(".class") || file.endsWith(".zip")) {
                return false;
            }
            return file.indexOf(match) == 0;
        }

        @Override
        public Object scan(Vfs.File file, Object classObject) {
            String relativePath = file.getRelativePath();
            if (acceptsInput(relativePath)) {
                getStore().put(relativePath, relativePath);
            }
            return classObject;
        }
    }
}
