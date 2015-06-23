package org.visallo.twitter;

import org.visallo.core.model.properties.types.DateVisalloProperty;
import org.visallo.core.model.properties.types.StringVisalloProperty;

public class TwitterOntology {
    public static final String EDGE_LABEL_TWEETED = "http://visallo.org/twitter#tweeted";
    public static final String EDGE_LABEL_MENTIONED = "http://visallo.org/twitter#mentioned";
    public static final String EDGE_LABEL_REFERENCED_URL = "http://visallo.org/twitter#refUrl";
    public static final String EDGE_LABEL_TAGGED = "http://visallo.org/twitter#tagged";
    public static final String EDGE_LABEL_RETWEET = "http://visallo.org/twitter#retweet";

    public static final String CONCEPT_TYPE_USER = "http://visallo.org/twitter#user";
    public static final String CONCEPT_TYPE_TWEET = "http://visallo.org/twitter#tweet";
    public static final String CONCEPT_TYPE_HASHTAG = "http://visallo.org/twitter#hashtag";
    public static final String CONCEPT_TYPE_URL = "http://visallo.org/twitter#url";
    public static final String CONCEPT_TYPE_PROFILE_IMAGE = "http://visallo.org/twitter#profileImage";

    public static final StringVisalloProperty PROFILE_IMAGE_URL = new StringVisalloProperty("http://visallo.org/twitter#profileImageUrl");
    public static final StringVisalloProperty SCREEN_NAME = new StringVisalloProperty("http://visallo.org/twitter#screenName");

    public static final DateVisalloProperty PUBLISHED_DATE = new DateVisalloProperty("http://visallo.org#publishedDate");
}
