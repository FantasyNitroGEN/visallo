package org.visallo.geoip;

import org.vertexium.type.GeoPoint;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GeoIpRepository {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(GeoIpRepository.class);
    private Map<Long, List<GeoIp>> lookupTable = new HashMap<>();

    public void load(InputStream in) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
            String line;
            int lineNumber = 1;
            while ((line = br.readLine()) != null) {
                try {
                    addGeoIpLine(line);
                } catch (Exception ex) {
                    LOGGER.warn("Invalid GeoIP line:%d: %s", lineNumber, line, ex);
                }
                lineNumber++;
            }
        } catch (IOException e) {
            throw new VisalloException("Could not read file", e);
        }
    }

    public void addGeoIpLine(String line) {
        String[] parts = line.split(",", -1);
        if (parts.length != 9) {
            throw new VisalloException("Invalid GeoIP line. Expected 9 parts, found " + parts.length);
        }
        String ipAndBitMask = parts[0];
        if (ipAndBitMask.equals("network")) {
            return;
        }

        // no geo-location information
        if (parts[7].length() == 0 || parts[8].length() == 0) {
            return;
        }

        String[] ipAndBitMaskParts = ipAndBitMask.split("/");
        if (ipAndBitMaskParts.length != 2) {
            throw new VisalloException("Invalid GeoIP line. Expected first part to contain an IP address an bits. found: " + ipAndBitMaskParts.length);
        }
        String ip = ipAndBitMaskParts[0];
        int bits = Integer.parseInt(ipAndBitMaskParts[1]);
        double latitude = Double.parseDouble(parts[7]);
        double longitude = Double.parseDouble(parts[8]);
        addGeoIp(ip, bits, new GeoPoint(latitude, longitude));
    }

    public void addGeoIp(String ipAddress, int bits, GeoPoint location) {
        long ipAddr = parseIpAddress(ipAddress);
        long key = getLookupTableKey(ipAddr);
        List<GeoIp> geoIps = lookupTable.get(key);
        if (geoIps == null) {
            geoIps = new ArrayList<>();
            lookupTable.put(key, geoIps);
        }
        geoIps.add(new GeoIp(ipAddr, bits, location));
    }

    public GeoPoint find(String ipAddress) {
        long ipAddr = parseIpAddress(ipAddress);
        return find(ipAddr);
    }

    private GeoPoint find(long ipAddr) {
        List<GeoIp> geoIps = lookupTable.get(getLookupTableKey(ipAddr));
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
        return bestMatch == null ? null : bestMatch.getGeoPoint();
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

        private GeoIp(long ip, int bits, GeoPoint geoPoint) {
            this.ip = ip;
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
