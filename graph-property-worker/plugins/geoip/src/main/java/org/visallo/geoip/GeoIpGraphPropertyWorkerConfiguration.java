package org.visallo.geoip;

import org.visallo.core.config.Configurable;

public class GeoIpGraphPropertyWorkerConfiguration {
    @Configurable
    private String pathPrefix = "/visallo/config/org.visallo.geoip.GeoIpGraphPropertyWorker";

    public String getPathPrefix() {
        return pathPrefix;
    }
}
