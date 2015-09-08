package org.visallo.web.clientapi.model;

public class ClientApiGeoRect {
    public ClientApiGeoPoint northWest;
    public ClientApiGeoPoint southEast;

    public ClientApiGeoRect(ClientApiGeoPoint northWest, ClientApiGeoPoint southEast) {
        this.northWest = northWest;
        this.southEast = southEast;
    }
}
