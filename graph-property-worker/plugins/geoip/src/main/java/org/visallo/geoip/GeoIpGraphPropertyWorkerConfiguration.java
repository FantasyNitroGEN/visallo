package org.visallo.geoip;

import org.visallo.core.config.Configurable;

public class GeoIpGraphPropertyWorkerConfiguration {
    @Configurable
    private String pathPrefix = "hdfs://";

    public String getPathPrefix() {
        return pathPrefix;
    }
}
