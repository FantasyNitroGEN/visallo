package org.visallo.twitter;

import com.google.inject.Inject;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import twitter4j.*;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Responsible for extracting tweet statuses and launching the processing pipeline
 */
public final class TweetExtractor {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(TweetExtractor.class);

    private final TweetTransformer tweetTransformer;

    /**
     * @param transformer The transformer used to process extracted tweet statuses, not null
     */
    @Inject
    public TweetExtractor(final TweetTransformer transformer) {
        tweetTransformer = checkNotNull(transformer);
    }

    /**
     * Fetches tweet statuses from the 'home timeline' of the user specified by the provided {@link OAuthConfiguration}
     *
     * @param authConfig The configuration information used to perform operations with the Twitter API
     * @throws TwitterException Thrown if an error occurred when fetching tweet data from Twitter
     */
    public void initiateTweetProcessing(final OAuthConfiguration authConfig, Priority priority) throws TwitterException {
        checkNotNull(authConfig);

        final Configuration clientConfig = generateTweetClientConfig(authConfig);
        final TwitterFactory twitterFactory = new TwitterFactory(clientConfig);
        final Twitter twitterClient = twitterFactory.getInstance();

        final ResponseList<Status> homeTimelineStatuses = twitterClient.getHomeTimeline();
        processStatuses(homeTimelineStatuses, priority);
    }

    private Configuration generateTweetClientConfig(final OAuthConfiguration authConfig) {
        final ConfigurationBuilder configBuilder = new ConfigurationBuilder()
                .setJSONStoreEnabled(true)
                .setDebugEnabled(true)
                .setOAuthConsumerKey(authConfig.getConsumerKey())
                .setOAuthConsumerSecret(authConfig.getConsumerSecret())
                .setOAuthAccessToken(authConfig.getToken())
                .setOAuthAccessTokenSecret(authConfig.getTokenSecret());

        return configBuilder.build();
    }

    private void processStatuses(final ResponseList<Status> tweetStatuses, Priority priority) {
        for (final Status status : tweetStatuses) {
            final long tweetId = status.getId();
            final String rawStatus = TwitterObjectFactory.getRawJSON(status);

            LOGGER.info("Processing raw tweet status id (%d) with content: %s", tweetId, rawStatus);
            tweetTransformer.transformTweetStatus(status, priority);
        }
    }
}
