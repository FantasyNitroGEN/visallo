package org.visallo.core.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class VersionUtil {
    public static void printVersion() {
        try {
            List<VersionData> versionDatas = new ArrayList<>();

            Enumeration<URL> resEnum = Thread.currentThread().getContextClassLoader().getResources(JarFile.MANIFEST_NAME);
            while (resEnum.hasMoreElements()) {
                URL url = resEnum.nextElement();
                try {
                    InputStream is = url.openStream();
                    if (is != null) {
                        Manifest manifest = new Manifest(is);
                        Attributes mainAttributes = manifest.getMainAttributes();
                        String builtOnUnix = mainAttributes.getValue("Built-On-Unix");
                        if (builtOnUnix != null) {
                            try {
                                Date buildOnDate = new Date(Long.parseLong(builtOnUnix));
                                String path = url.toString();
                                path = path.replace("!/META-INF/MANIFEST.MF", "");
                                path = path.replace("/META-INF/MANIFEST.MF", "");
                                path = path.replace("jar:", "");
                                path = path.replace("file:", "");
                                versionDatas.add(new VersionData(path, buildOnDate));
                            } catch (Exception ex) {
                                System.out.println("Could not parse Built-On-Unix: " + builtOnUnix + ": " + ex.getMessage());
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Could not find version information in: " + url);
                }
            }
            if (versionDatas.size() == 0) {
                System.err.println("Could not find version information");
            } else {
                int maxPathWidth = 0;
                for (VersionData versionData : versionDatas) {
                    maxPathWidth = Math.max(maxPathWidth, versionData.getPath().length());
                }
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                for (VersionData versionData : versionDatas) {
                    String buildOnDateString = sdf.format(versionData.getBuildOnDate());
                    System.out.println(String.format("%-" + maxPathWidth + "s: %s", versionData.getPath(), buildOnDateString));
                }
            }
        } catch (IOException ex) {
            System.err.println("could not get version information: " + ex.getMessage());
        }
    }

    private static class VersionData {
        private final String path;
        private final Date buildOnDate;

        public VersionData(String path, Date buildOnDate) {
            this.path = path;
            this.buildOnDate = buildOnDate;
        }

        public String getPath() {
            return path;
        }

        public Date getBuildOnDate() {
            return buildOnDate;
        }
    }
}
