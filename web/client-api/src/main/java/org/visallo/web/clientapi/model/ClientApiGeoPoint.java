package org.visallo.web.clientapi.model;

public class ClientApiGeoPoint {
    public double latitude;
    public double longitude;

    public ClientApiGeoPoint() {

    }

    public ClientApiGeoPoint(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
