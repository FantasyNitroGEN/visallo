package org.visallo.twitter.loaders;

import org.vertexium.Visibility;
import org.visallo.core.model.properties.types.StreamingSingleValueVisalloProperty;
import org.visallo.core.security.VisibilityTranslator;
import org.visallo.twitter.TweetTransformer;

public final class LoaderConstants {
    public static final String MULTI_VALUE_KEY = TweetTransformer.class.getName();
    public static final String SOURCE_NAME = "twitter.com";
    public static final StreamingSingleValueVisalloProperty TWITTER_RAW_JSON = new StreamingSingleValueVisalloProperty("http://visallo.org/twitter/rawJson");

    private Visibility emptyVisibility;

    public LoaderConstants (VisibilityTranslator visibilityTranslator) {
        emptyVisibility = visibilityTranslator.getDefaultVisibility();
    }

    public Visibility getEmptyVisibility() {
        return emptyVisibility;
    }
}
