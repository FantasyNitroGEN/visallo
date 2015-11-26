package org.visallo.core.model.file;

import com.google.inject.Inject;
import org.apache.commons.io.FileUtils;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class HdfsFileSystemRepository extends FileSystemRepository {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(HdfsFileSystemRepository.class);
    private final FileSystem hdfsFileSystem;

    @Inject
    public HdfsFileSystemRepository(Configuration configuration) {
        hdfsFileSystem = getFileSystem(configuration);
    }

    public static FileSystem getFileSystem(Configuration configuration) {
        String hdfsUserName = configuration.get(Configuration.HDFS_USER_NAME, Configuration.HDFS_USER_NAME_DEFAULT);
        String fsDefaultFS = configuration.get("fs.defaultFS", null);
        try {
            return FileSystem.get(new URI(fsDefaultFS), configuration.getHadoopConfiguration(), hdfsUserName);
        } catch (Exception e) {
            throw new VisalloException("Could not open hdfs filesystem: " + fsDefaultFS + " (user: " + hdfsUserName + ")", e);
        }
    }

    @Override
    public File getLocalFileFor(String path) {
        try {
            Path filePath = getHdfsPath(path);
            String fileName = filePath.getName();
            File tempFile = new File(System.getProperty("java.io.tmpdir"), fileName);
            LOGGER.debug("Copying %s to %s", filePath, tempFile.getAbsolutePath());
            try (InputStream shapeInputStream = hdfsFileSystem.open(filePath)) {
                FileUtils.copyInputStreamToFile(shapeInputStream, tempFile);
            }
            return tempFile;
        } catch (IOException ex) {
            throw new VisalloException("Could not copy file: " + path, ex);
        }
    }

    private Path getHdfsPath(String path) {
        try {
            Path filePath = new Path(path);
            if (!hdfsFileSystem.exists(filePath)) {
                throw new VisalloException("Could not find file: " + filePath);
            }
            return filePath;
        } catch (IOException ex) {
            throw new VisalloException("Could not get path for: " + path, ex);
        }
    }

    @Override
    public InputStream getInputStream(String path) {
        try {
            return hdfsFileSystem.open(getHdfsPath(path));
        } catch (IOException ex) {
            throw new VisalloException("Could not open file: " + path, ex);
        }
    }

    @Override
    public Iterable<String> list(String path) {
        List<String> results = new ArrayList<>();
        try {
            FileStatus[] statuses = hdfsFileSystem.listStatus(new Path(path));
            for (FileStatus status : statuses) {
                results.add(status.getPath().getName());
            }
        } catch (IOException e) {
            throw new VisalloException("Could not get files for: " + path);
        }
        return results;
    }

    public FileSystem getHdfsFileSystem() {
        return hdfsFileSystem;
    }
}
