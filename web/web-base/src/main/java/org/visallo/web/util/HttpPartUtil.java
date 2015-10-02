package org.visallo.web.util;

import org.apache.commons.io.IOUtils;

import javax.servlet.http.Part;
import java.io.*;

public class HttpPartUtil {
    public static void copyPartToOutputStream(Part part, OutputStream out) throws IOException {
        try (InputStream in = part.getInputStream()) {
            IOUtils.copy(in, out);
        }
    }

    public static void copyPartToFile(Part part, File outFile) throws IOException {
        try (FileOutputStream out = new FileOutputStream(outFile)) {
            copyPartToOutputStream(part, out);
        }
    }
}
