package org.visallo.twitter;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.visallo.core.bootstrap.InjectHelper;
import org.visallo.core.cmdline.CommandLineTool;
import org.visallo.core.cmdline.converters.WorkQueuePriorityConverter;
import org.visallo.core.config.Configuration;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import twitter4j.TwitterException;

@Parameters(commandDescription = "Load Twitter data")
public final class TwitterDataIngestRunner extends CommandLineTool {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(TwitterDataIngestRunner.class);
    private static final String CONSUMER_KEY = "twitter.consumerKey";
    private static final String CONSUMER_SECRET = "twitter.consumerSecret";
    private static final String TOKEN = "twitter.token";
    private static final String TOKEN_SECRET = "twitter.tokenSecret";
    private static final int SUCCESSFUL_EXIT = 0;
    private static final int FAILURE_EXIT = 0;

    @Parameter(names = {"--priority", "-p"}, arity = 1, converter = WorkQueuePriorityConverter.class, description = "Priority at which to enqueue")
    private Priority priority = Priority.NORMAL;

    public static void main(String[] args) throws Exception {
        CommandLineTool.main(new TwitterDataIngestRunner(), args);
    }

    @Override
    protected int run() {
        int exitCode = SUCCESSFUL_EXIT;

        final OAuthConfiguration authConfig = retrieveAuthConfig();
        final TweetExtractor extractor = InjectHelper.getInstance(TweetExtractor.class);

        LOGGER.info("Running data ingestion");
        final long startTime = System.currentTimeMillis();

        try {
            extractor.initiateTweetProcessing(authConfig, priority);
            LOGGER.info("Data ingestion completed in %d ms", System.currentTimeMillis() - startTime);
        } catch (final TwitterException e) {
            LOGGER.error("Error occurred during data ingestion", e);
            exitCode = FAILURE_EXIT;
        }

        return exitCode;
    }

    private OAuthConfiguration retrieveAuthConfig() {
        final Configuration appConfig = getConfiguration();

        return new OAuthConfiguration(
                appConfig.get(CONSUMER_KEY, null),
                appConfig.get(CONSUMER_SECRET, null),
                appConfig.get(TOKEN, null),
                appConfig.get(TOKEN_SECRET, null));
    }
}
