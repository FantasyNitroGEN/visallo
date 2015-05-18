package org.visallo.gdelt;

import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.properties.types.*;

public class GDELTProperties {
    public static final StringSingleValueVisalloProperty CONCEPT_TYPE = VisalloProperties.CONCEPT_TYPE;

    // event properties
    public static final StringVisalloProperty GLOBAL_EVENT_ID = new StringVisalloProperty("http://visallo.org/gdelt#globalEventId");
    public static final DateVisalloProperty EVENT_DATE_OF_OCCURRENCE = new DateVisalloProperty("http://visallo.org/gdelt#dateOfOccurrence");
    public static final BooleanVisalloProperty EVENT_IS_ROOT_EVENT = new BooleanVisalloProperty("http://visallo.org/gdelt#isRootEvent");
    public static final StringVisalloProperty EVENT_CODE = new StringVisalloProperty("http://visallo.org/gdelt#eventCode");
    public static final StringVisalloProperty EVENT_BASE_CODE = new StringVisalloProperty("http://visallo.org/gdelt#eventBaseCode");
    public static final StringVisalloProperty EVENT_ROOT_CODE = new StringVisalloProperty("http://visallo.org/gdelt#eventRootCode");
    public static final IntegerVisalloProperty EVENT_QUAD_CLASS = new IntegerVisalloProperty("http://visallo.org/gdelt#quadClass");
    public static final DoubleVisalloProperty EVENT_GOLDSTEIN_SCALE = new DoubleVisalloProperty("http://visallo.org/gdelt#goldsteinScale");
    public static final IntegerVisalloProperty EVENT_NUM_MENTIONS = new IntegerVisalloProperty("http://visallo.org/gdelt#numMentions");
    public static final IntegerVisalloProperty EVENT_NUM_SOURCES = new IntegerVisalloProperty("http://visallo.org/gdelt#numSources");
    public static final IntegerVisalloProperty EVENT_NUM_ARTICLES = new IntegerVisalloProperty("http://visallo.org/gdelt#numArticles");
    public static final DoubleVisalloProperty EVENT_AVG_TONE = new DoubleVisalloProperty("http://visallo.org/gdelt#avgTone");
    public static final GeoPointVisalloProperty EVENT_GEOLOCATION = new GeoPointVisalloProperty("http://visallo.org/gdelt#geoLocation");
    public static final DateVisalloProperty EVENT_DATE_ADDED = new DateVisalloProperty("http://visallo.org/gdelt#dateAdded");
    public static final StringVisalloProperty EVENT_SOURCE_URL = new StringVisalloProperty("http://visallo.org/gdelt#sourceUrl");

    // actor properties
    public static final StringVisalloProperty ACTOR_CODE = new StringVisalloProperty("http://visallo.org/gdelt#actorCode");
    public static final StringVisalloProperty ACTOR_NAME = new StringVisalloProperty("http://visallo.org/gdelt#actorName");
    public static final StringVisalloProperty ACTOR_COUNTRY_CODE = new StringVisalloProperty("http://visallo.org/gdelt#countryCode");
    public static final StringVisalloProperty ACTOR_KNOWN_GROUP_CODE = new StringVisalloProperty("http://visallo.org/gdelt#knownGroupCode");
    public static final StringVisalloProperty ACTOR_ETHNIC_CODE = new StringVisalloProperty("http://visallo.org/gdelt#ethnicCode");
    public static final StringVisalloProperty ACTOR_RELIGION_CODE = new StringVisalloProperty("http://visallo.org/gdelt#religionCode");
    public static final StringVisalloProperty ACTOR_TYPE_CODE = new StringVisalloProperty("http://visallo.org/gdelt#typeCode");

    public static final String ACTOR1_TO_EVENT_EDGE = "http://visallo.org/gdelt#acted";
    public static final String EVENT_TO_ACTOR2_EDGE = "http://visallo.org/gdelt#wasActedUpon";
}


