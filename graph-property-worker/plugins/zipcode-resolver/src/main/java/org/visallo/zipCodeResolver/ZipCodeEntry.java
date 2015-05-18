package org.visallo.zipCodeResolver;

import org.vertexium.type.GeoPoint;

public class ZipCodeEntry {
    private final String zipCode;
    private final String city;
    private final String state;
    private final double latitude;
    private final double longitude;

    public ZipCodeEntry(String zipCode, String city, String state, double latitude, double longitude) {
        this.zipCode = zipCode;
        this.city = city;
        this.state = state;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getZipCode() {
        return zipCode;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public GeoPoint createGeoPoint() {
        return new GeoPoint(getLatitude(), getLongitude(), getDescription());
    }

    public String getDescription() {
        if (getCity() != null && getState() != null) {
            return getCity() + ", " + getState();
        }
        return getZipCode();
    }
}
