package org.visallo.twitter.loaders;

import org.visallo.core.model.properties.types.StreamingSingleValueVisalloProperty;
import org.visallo.core.security.VisalloVisibility;
import org.visallo.twitter.TweetTransformer;

import org.vertexium.Visibility;

public final class LoaderConstants {
    public static final Visibility EMPTY_VISIBILITY = new VisalloVisibility().getVisibility();
    public static final String MULTI_VALUE_KEY = TweetTransformer.class.getName();
    public static final String SOURCE_NAME = "twitter.com";
    public static final StreamingSingleValueVisalloProperty TWITTER_RAW_JSON = new StreamingSingleValueVisalloProperty("http://visallo.org/twitter/rawJson");


    private LoaderConstants() {
        throw new AssertionError();
    }
}
