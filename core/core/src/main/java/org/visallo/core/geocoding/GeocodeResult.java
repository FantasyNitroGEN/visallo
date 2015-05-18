package org.visallo.core.geocoding;

public class GeocodeResult {
    private final String name;
    private final double latitude;
    private final double longitude;

    public GeocodeResult(String name, double latitude, double longitude) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getName() {
        return name;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }
}
