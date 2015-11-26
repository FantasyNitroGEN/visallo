package org.visallo.geoip;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.inject.Inject;
import org.supercsv.io.CsvListReader;
import org.supercsv.prefs.CsvPreference;
import org.vertexium.type.GeoPoint;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.file.FileSystemRepository;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GeoIpRepository {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(GeoIpRepository.class);
    private Map<Long, List<GeoIp>> geoIpLookupTable = new HashMap<>();
    private Map<Long, String> locationLookupTable = new HashMap<>();

    @Inject
    public GeoIpRepository(FileSystemRepository fileSystemRepository) {
        String geoLite2CityBlocksIpv4HdfsPath = "/visallo/config/org.visallo.geoip.GeoIpGraphPropertyWorker/GeoLite2-City-Blocks-IPv4.csv";
        LOGGER.debug("Loading %s", geoLite2CityBlocksIpv4HdfsPath);
        try (InputStream in = fileSystemRepository.getInputStream(geoLite2CityBlocksIpv4HdfsPath)) {
            loadGeoIp(in);
        } catch (IOException e) {
            throw new VisalloException("Could not close file", e);
        }

        String geoLite2CityLocationsEnHdfsPath = "/visallo/config/org.visallo.geoip.GeoIpGraphPropertyWorker/GeoLite2-City-Locations-en.csv";
        LOGGER.debug("Loading %s", geoLite2CityLocationsEnHdfsPath);
        try (InputStream in = fileSystemRepository.getInputStream(geoLite2CityLocationsEnHdfsPath)) {
            loadGeoLocations(in);
        } catch (IOException e) {
            throw new VisalloException("Could not close file", e);
        }
    }

    @VisibleForTesting
    void loadGeoLocations(InputStream in) {
        try {
            CsvListReader csvReader = new CsvListReader(new InputStreamReader(in), CsvPreference.STANDARD_PREFERENCE);
            csvReader.read(); // skip title line

            int lineNumber = 1;
            List<String> line;
            while ((line = csvReader.read()) != null) {
                try {
                    loadGeoLocationLine(line);
                } catch (Exception ex) {
                    LOGGER.warn("Invalid Geo location line: %d: %s", lineNumber, line, ex);
                }
                lineNumber++;
            }
        } catch (IOException ex) {
            throw new VisalloException("Could not read geo locations file", ex);
        }
    }

    private void loadGeoLocationLine(List<String> parts) {
        if (parts.size() != 13) {
            throw new VisalloException("Invalid Geo location line. Expected 13 parts, found " + parts.size());
        }
        long id = Long.parseLong(parts.get(0));
        String continent = emptyStringToNull(parts.get(3));
        String country = emptyStringToNull(parts.get(5));
        String subdivision1 = emptyStringToNull(parts.get(7));
        String subdivision2 = emptyStringToNull(parts.get(9));
        String city = emptyStringToNull(parts.get(10));
        addLocation(id, Joiner.on(", ").skipNulls().join(continent, country, subdivision1, subdivision2, city));
    }

    @VisibleForTesting
    void addLocation(long id, String location) {
        this.locationLookupTable.put(id, location);
    }

    private String emptyStringToNull(String s) {
        if (s == null) {
            return null;
        }
        s = s.trim();
        if (s.length() == 0) {
            return null;
        }
        return s;
    }

    @VisibleForTesting
    void loadGeoIp(InputStream in) {
        try {
            CsvListReader csvReader = new CsvListReader(new InputStreamReader(in), CsvPreference.STANDARD_PREFERENCE);
            csvReader.read(); // skip title line

            int lineNumber = 1;
            List<String> line;
            while ((line = csvReader.read()) != null) {
                try {
                    addGeoIpLine(line);
                } catch (Exception ex) {
                    LOGGER.warn("Invalid GeoIP line: %d: %s", lineNumber, line, ex);
                }
                lineNumber++;
            }
        } catch (IOException e) {
            throw new VisalloException("Could not read file", e);
        }
    }

    @VisibleForTesting
    void addGeoIpLine(List<String> parts) {
        if (parts.size() != 9) {
            throw new VisalloException("Invalid GeoIP line. Expected 9 parts, found " + parts.size());
        }
        String ipAndBitMask = parts.get(0);
        if (ipAndBitMask.equals("network")) {
            return;
        }

        // no geo-location information
        if (parts.get(7) == null || parts.get(7).length() == 0 || parts.get(8) == null || parts.get(8).length() == 0) {
            return;
        }

        String[] ipAndBitMaskParts = ipAndBitMask.split("/");
        if (ipAndBitMaskParts.length != 2) {
            throw new VisalloException("Invalid GeoIP line. Expected first part to contain an IP address an bits. found: " + ipAndBitMaskParts.length);
        }
        String ip = ipAndBitMaskParts[0];
        int bits = Integer.parseInt(ipAndBitMaskParts[1]);
        long geonameId = Long.parseLong(parts.get(1));
        double latitude = Double.parseDouble(parts.get(7));
        double longitude = Double.parseDouble(parts.get(8));
        addGeoIp(ip, bits, geonameId, new GeoPoint(latitude, longitude));
    }

    @VisibleForTesting
    void addGeoIp(String ipAddress, int bits, Long geonameId, GeoPoint location) {
        long ipAddr = parseIpAddress(ipAddress);
        long key = getLookupTableKey(ipAddr);
        List<GeoIp> geoIps = geoIpLookupTable.get(key);
        if (geoIps == null) {
            geoIps = new ArrayList<>();
            geoIpLookupTable.put(key, geoIps);
        }
        geoIps.add(new GeoIp(ipAddr, bits, geonameId, location));
    }

    public GeoPoint find(String ipAddress) {
        long ipAddr = parseIpAddress(ipAddress);
        return find(ipAddr);
    }

    private GeoPoint find(long ipAddr) {
        List<GeoIp> geoIps = geoIpLookupTable.get(getLookupTableKey(ipAddr));
        if (geoIps == null) {
            return null;
        }
        return find(geoIps, ipAddr);
    }

    private long getLookupTableKey(long ipAddr) {
        return (ipAddr & 0xff000000L) >> 24;
    }

    private GeoPoint find(List<GeoIp> geoIps, long ipAddr) {
        GeoIp bestMatch = null;
        for (GeoIp geoIp : geoIps) {
            if (bestMatch != null && geoIp.getBits() < bestMatch.getBits()) {
                continue;
            }
            if (geoIp.isMatch(ipAddr)) {
                bestMatch = geoIp;
            }
        }
        return bestMatch == null ? null : bestMatch.getGeoPoint(locationLookupTable);
    }

    private long parseIpAddress(String ipAddress) {
        String[] partsString = ipAddress.split("\\.");
        if (partsString.length != 4) {
            throw new VisalloException("Invalid ip address '" + ipAddress + "', wrong number of parts");
        }
        long[] parts = new long[4];
        for (int i = 0; i < 4; i++) {
            try {
                parts[i] = Integer.parseInt(partsString[i]);
            } catch (NumberFormatException ex) {
                throw new VisalloException("Invalid ip address '" + ipAddress + "', must be numbers", ex);
            }
            if (parts[i] < 0 || parts[i] > 255) {
                throw new VisalloException("Invalid ip address '" + ipAddress + "', must be numbers between 0-255");
            }
        }
        return (parts[0] << 24) | (parts[1] << 16) | (parts[2] << 8) | (parts[3]);
    }

    private static class GeoIp {
        private final long from;
        private final long to;
        private final int bits;
        private final GeoPoint geoPoint;
        private final long ip;
        private final Long geonameId;

        private GeoIp(long ip, int bits, Long geonameId, GeoPoint geoPoint) {
            this.ip = ip;
            this.geonameId = geonameId;
            this.from = toFrom(ip, bits);
            this.to = toTo(ip, bits);
            this.bits = bits;
            this.geoPoint = geoPoint;
        }

        private long toFrom(long ip, int bits) {
            return ip & (0xffffffffL << (32 - bits));
        }

        private long toTo(long ip, int bits) {
            return ip | (0xffffffffL >> bits);
        }

        public GeoPoint getGeoPoint() {
            return geoPoint;
        }

        public int getBits() {
            return bits;
        }

        public Long getGeonameId() {
            return geonameId;
        }

        public GeoPoint getGeoPoint(Map<Long, String> locationLookupTable) {
            GeoPoint result = getGeoPoint();
            Long geonameId = getGeonameId();
            if (geonameId != null) {
                String location = locationLookupTable.get(geonameId);
                if (location != null) {
                    result = new GeoPoint(result.getLatitude(), result.getLongitude(), location);
                }
            }
            return result;
        }

        public boolean isMatch(long ipAddr) {
            return ipAddr >= from && ipAddr <= to;
        }

        @Override
        public String toString() {
            return String.format("%d.%d.%d.%d/%d -> %s",
                    (ip >> 24) & 0xff,
                    (ip >> 16) & 0xff,
                    (ip >> 8) & 0xff,
                    (ip >> 0) & 0xff,
                    getBits(),
                    getGeoPoint().toString()
            );
        }
    }
}
