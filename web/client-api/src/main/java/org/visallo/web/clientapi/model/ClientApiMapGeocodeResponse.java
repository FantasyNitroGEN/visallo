package org.visallo.web.clientapi.model;

import java.util.ArrayList;
import java.util.List;

public class ClientApiMapGeocodeResponse implements ClientApiObject {
    public List<Result> results = new ArrayList<>();

    public static class Result implements ClientApiObject {
        public String name;
        public double latitude;
        public double longitude;

        public Result() {

        }

        public Result(String name, double latitude, double longitude) {
            this.name = name;
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }
}
