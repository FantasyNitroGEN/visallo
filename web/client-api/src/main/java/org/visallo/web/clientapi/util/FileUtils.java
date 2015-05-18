package org.visallo.web.clientapi.util;

import java.io.File;

public class FileUtils {
    public static String getTempDirectoryPath() {
        return System.getProperty("java.io.tmpdir");
    }

    public static File getTempDirectory() {
        return new File(getTempDirectoryPath());
    }
}
