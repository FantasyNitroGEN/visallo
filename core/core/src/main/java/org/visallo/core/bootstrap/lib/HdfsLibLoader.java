package org.visallo.core.bootstrap.lib;

import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.Description;
import org.visallo.core.model.Name;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.security.NoSuchAlgorithmException;

@Name("HDFS Lib Directory")
@Description("Loads .jar files from a HDFS directory")
public class HdfsLibLoader extends LibLoader {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(HdfsLibLoader.class);

    @Override
    public void loadLibs(Configuration configuration) {
        LOGGER.info("Loading libs using %s", HdfsLibLoader.class.getName());

        String hdfsLibDirectory = configuration.get(Configuration.HDFS_LIB_SOURCE_DIRECTORY, null);
        if (hdfsLibDirectory == null) {
            LOGGER.warn("Skipping HDFS lib. Configuration parameter %s not found", Configuration.HDFS_LIB_SOURCE_DIRECTORY);
            return;
        }

        File libDirectory = getLocalHdfsLibDirectory(configuration);
        String hdfsLibUser = getHdfsLibUser(configuration);
        FileSystem hdfsFileSystem = getFileSystem(configuration, hdfsLibUser);

        try {
            syncLib(hdfsFileSystem, new Path(hdfsLibDirectory), libDirectory);
        } catch (Exception ex) {
            throw new VisalloException(String.format("Could not sync HDFS lib. %s -> %s", hdfsLibDirectory, libDirectory.getAbsolutePath()), ex);
        }
    }

    private File getLocalHdfsLibDirectory(Configuration configuration) {
        String hdfsLibTempDirectoryString = configuration.get(Configuration.HDFS_LIB_TEMP_DIRECTORY, null);
        File libDirectory;
        if (hdfsLibTempDirectoryString == null) {
            File baseDir = new File(System.getProperty("java.io.tmpdir"));
            libDirectory = new File(baseDir, "visallo-hdfslib");
            LOGGER.info("Configuration parameter %s was not set; defaulting local lib dir to %s", Configuration.HDFS_LIB_TEMP_DIRECTORY, libDirectory.getAbsolutePath());
        } else {
            libDirectory = new File(hdfsLibTempDirectoryString);
            LOGGER.info("Using local lib directory: %s", libDirectory.getAbsolutePath());
        }

        if (!libDirectory.exists()) {
            libDirectory.mkdirs();
        }

        return libDirectory;
    }

    private String getHdfsLibUser(Configuration configuration) {
        String hdfsLibUser = configuration.get(Configuration.HDFS_LIB_HDFS_USER, null);
        if (hdfsLibUser == null) {
            hdfsLibUser = "hadoop";
            LOGGER.info("Configuration parameter %s was not set; defaulting to HDFS user '%s'.", Configuration.HDFS_LIB_HDFS_USER, hdfsLibUser);
        } else {
            LOGGER.info("Connecting to HDFS as user '%s'", hdfsLibUser);
        }
        return hdfsLibUser;
    }

    private FileSystem getFileSystem(Configuration configuration, String user) {
        try {
            String hdfsRootDir = configuration.get(Configuration.HADOOP_URL, null);
            if (hdfsRootDir == null) {
                throw new VisalloException("Could not find configuration: " + Configuration.HADOOP_URL);
            }
            return FileSystem.get(new URI(hdfsRootDir), configuration.toHadoopConfiguration(), user);
        } catch (Exception ex) {
            throw new VisalloException("Could not open HDFS file system.", ex);
        }
    }

    private static void syncLib(FileSystem fs, Path source, File destDir) throws IOException, NoSuchAlgorithmException {
        if (!fs.exists(source)) {
            throw new VisalloException(String.format("Could not sync HDFS directory %s. Directory does not exist.", source));
        }

        addFilesFromHdfs(fs, source, destDir);
    }

    private static void addFilesFromHdfs(FileSystem fs, Path source, File destDir) throws IOException, NoSuchAlgorithmException {
        LOGGER.debug("Adding files from HDFS %s -> %s", source.toString(), destDir.getAbsolutePath());
        FileStatus[] hdfsFiles = fs.listStatus(source);
        for (FileStatus hdfsFile : hdfsFiles) {
            if (hdfsFile.isDirectory()) {
                File subDestDir = new File(destDir, hdfsFile.getPath().getName());
                if (!subDestDir.exists()) {
                    subDestDir.mkdirs();
                }
                addFilesFromHdfs(fs, hdfsFile.getPath(), subDestDir);
            } else {
                File locallyCachedFile = getLocalCacheFileName(hdfsFile, destDir);
                if (locallyCachedFile.exists()) {
                    LOGGER.info("HDFS file %s already cached at %s. Skipping sync.", hdfsFile.getPath().toString(), locallyCachedFile.getPath());
                } else {
                    fs.copyToLocalFile(hdfsFile.getPath(), new Path(locallyCachedFile.getAbsolutePath()));
                    locallyCachedFile.setLastModified(hdfsFile.getModificationTime());
                    LOGGER.info("Caching HDFS file %s -> %s", hdfsFile.getPath().toString(), locallyCachedFile.getPath());
                }

                addLibFile(locallyCachedFile);
            }
        }
    }

    private static File getLocalCacheFileName(FileStatus hdfsFile, File destdir) {
        String filename = hdfsFile.getPath().getName();
        String baseFilename = filename.substring(0, filename.lastIndexOf('.'));
        String extension = filename.substring(filename.lastIndexOf('.'));
        String cacheFilename = baseFilename + "-" + hdfsFile.getModificationTime() + extension;
        return new File(destdir, cacheFilename);
    }

}