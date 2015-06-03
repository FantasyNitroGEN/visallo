package org.visallo.zipCodeBoundaries;

import com.vividsolutions.jts.geom.MultiPolygon;
import org.opengis.feature.Property;
import org.visallo.web.clientapi.model.ClientApiObject;

import java.util.ArrayList;
import java.util.List;

public class Features implements ClientApiObject {
    private List<Feature> features = new ArrayList<>();

    public Features(List<Feature> features) {
        this.features.addAll(features);
    }

    public List<Feature> getFeatures() {
        return features;
    }

    public static class Feature {
        private String zipCode;
        private List<Coordinate> coordinates = new ArrayList<>();

        public static Feature create(org.opengis.feature.Feature feature) {
            Property zipCodeProperty = feature.getProperty("ZCTA5CE10");
            if (zipCodeProperty == null) {
                return null;
            }
            Feature result = new Feature();
            result.zipCode = (String) zipCodeProperty.getValue();

            MultiPolygon poly = (MultiPolygon) feature.getDefaultGeometryProperty().getValue();
            for (com.vividsolutions.jts.geom.Coordinate polyPt : poly.getCoordinates()) {
                result.coordinates.add(new Coordinate(polyPt));
            }

            return result;
        }

        public String getZipCode() {
            return zipCode;
        }

        public List<Coordinate> getCoordinates() {
            return coordinates;
        }
    }

    public static class Coordinate {
        private final double latitude;
        private final double longitude;

        public Coordinate(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public Coordinate(com.vividsolutions.jts.geom.Coordinate polyPt) {
            this(polyPt.y, polyPt.x);
        }

        public double getLatitude() {
            return latitude;
        }

        public double getLongitude() {
            return longitude;
        }

        @Override
        public String toString() {
            return "{" + latitude + ", " + longitude + '}';
        }
    }
}
