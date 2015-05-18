package org.visallo.it;

import org.visallo.core.model.properties.types.GeoPointVisalloProperty;

public class TestOntology {
    public static final String CONCEPT_PERSON = "http://visallo.org/test#person";
    public static final String CONCEPT_CITY = "http://visallo.org/test#city";
    public static final String CONCEPT_ZIP_CODE = "http://visallo.org/test#zipCode";

    public static final String EDGE_LABEL_WORKS_FOR = "http://visallo.org/test#worksFor";

    public static final GeoPointVisalloProperty PROPERTY_GEO_LOCATION = new GeoPointVisalloProperty("http://visallo.org/test#geolocation");
    public static final String PROPERTY_NAME = "http://visallo.org/test#name";
}
