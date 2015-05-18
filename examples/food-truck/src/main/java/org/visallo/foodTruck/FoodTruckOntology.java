package org.visallo.foodTruck;

import org.visallo.core.model.properties.types.DateVisalloProperty;
import org.visallo.core.model.properties.types.GeoPointVisalloProperty;
import org.visallo.core.model.properties.types.StringVisalloProperty;

public class FoodTruckOntology {
    public static final String EDGE_LABEL_HAS_KEYWORD = "http://visallo.org/foodtruck#tweetHasKeyword";
    public static final String EDGE_LABEL_HAS_TWITTER_USER = "http://visallo.org/foodtruck#foodTruckHasTwitterUser";

    public static final String CONCEPT_TYPE_FOOD_TRUCK = "http://visallo.org/foodtruck#foodTruck";
    public static final String CONCEPT_TYPE_LOCATION = "http://visallo.org/foodtruck#location";

    public static final GeoPointVisalloProperty GEO_LOCATION = new GeoPointVisalloProperty("http://visallo.org/foodtruck#geoLocation");
    public static final DateVisalloProperty GEO_LOCATION_DATE = new DateVisalloProperty("http://visallo.org/foodtruck#geoLocationDate");
    public static final StringVisalloProperty KEYWORD = new StringVisalloProperty("http://visallo.org/foodtruck#keyword");
}
