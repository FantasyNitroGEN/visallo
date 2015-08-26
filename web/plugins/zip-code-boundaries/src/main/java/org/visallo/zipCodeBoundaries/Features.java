package org.visallo.zipCodeBoundaries;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.metadata.iso.extent.GeographicBoundingBoxImpl;
import org.opengis.feature.Property;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.visallo.core.exception.VisalloException;
import org.visallo.web.clientapi.model.ClientApiObject;

import java.util.ArrayList;
import java.util.List;

public class Features implements ClientApiObject {
    private List<Feature> features = new ArrayList<>();
    private ReferencedEnvelope envelope;

    public Features(List<Feature> features) {
        this.features.addAll(features);

        for (Feature feature : features) {
            if(envelope == null) {
                envelope = ReferencedEnvelope.create(feature.coordinateSystem);
            }

            double[][] bbox = feature.getBoundingBox();
            envelope.expandToInclude(new ReferencedEnvelope(bbox[0][1], bbox[1][1], bbox[0][0], bbox[1][0], feature.coordinateSystem));
        }
    }

    public List<Feature> getFeatures() {
        return features;
    }

    public double[] getCentroid() {
        return new double[] {envelope.getMedian(1), envelope.getMedian(0)};
    }

    public double[][] getBoundingBox() {
        try {
            GeographicBoundingBoxImpl geoBBox = new GeographicBoundingBoxImpl(envelope);
            return new double[][]{
                    {geoBBox.getNorthBoundLatitude(), geoBBox.getWestBoundLongitude()},
                    {geoBBox.getSouthBoundLatitude(), geoBBox.getEastBoundLongitude()}
            };
        } catch (TransformException e) {
            throw new VisalloException("Error calculating bounding box.", e);
        }
    }

    public static class Feature implements ClientApiObject {
        private CoordinateReferenceSystem coordinateSystem;
        private String zipCode;
        private List<List<double[]>> coordinates = new ArrayList<>();
        private double[][] boundingBox;

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

            Coordinate[] envelope = multiPoly.getEnvelope().getCoordinates();
            result.boundingBox = new double[][]{
                    {envelope[0].y, envelope[0].x},
                    {envelope[2].y, envelope[2].x}
            };

            result.coordinateSystem = feature.getType().getGeometryDescriptor().getCoordinateReferenceSystem();

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

        public double[][] getBoundingBox() {
            return boundingBox;
        }
    }
}
