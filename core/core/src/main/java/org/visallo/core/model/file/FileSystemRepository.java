package org.visallo.core.model.file;

import java.io.File;
import java.io.InputStream;

public abstract class FileSystemRepository {
    public abstract File getLocalFileFor(String path);

    public abstract InputStream getInputStream(String path);

    public abstract Iterable<String> list(String path);
}
