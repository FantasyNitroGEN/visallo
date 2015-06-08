package org.visallo.zipCodeBoundaries;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
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
        private List<List<double[]>> coordinates = new ArrayList<>();

        public static Feature create(org.opengis.feature.Feature feature) {
            String zipCode = getZipCode(feature);
            if (zipCode == null) {
                return null;
            }
            Feature result = new Feature();
            result.zipCode = zipCode;

            MultiPolygon multiPoly = (MultiPolygon) feature.getDefaultGeometryProperty().getValue();
            for (int i = 0; i < multiPoly.getNumGeometries(); i++) {
                List<double[]> polyCoords = new ArrayList<>();
                Polygon poly = (Polygon) multiPoly.getGeometryN(i);
                for (Coordinate polyPt : poly.getCoordinates()) {
                    polyCoords.add(new double[]{polyPt.y, polyPt.x});
                }
                result.coordinates.add(polyCoords);
            }

            return result;
        }

        public static String getZipCode(org.opengis.feature.Feature feature) {
            Property prop = feature.getProperty("ZCTA5CE10");
            if (prop == null) {
                return null;
            }
            return (String) prop.getValue();
        }

        public String getZipCode() {
            return zipCode;
        }

        public List<List<double[]>> getCoordinates() {
            return coordinates;
        }
    }
}
