package org.visallo.twitter.loaders;

import com.google.common.base.Charsets;
import com.google.inject.Inject;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.security.VisibilityTranslator;
import org.visallo.twitter.TwitterOntology;
import org.visallo.web.clientapi.model.VisibilityJson;
import org.vertexium.*;
import org.vertexium.property.StreamingPropertyValue;
import twitter4j.Status;
import twitter4j.TwitterObjectFactory;

import java.io.ByteArrayInputStream;
import java.util.Date;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Responsible for loading Twitter tweet status vertex information to/from the underlying data store
 */
public final class TweetVertexLoader {

    private final Graph graph;
    private final WorkQueueRepository workQueueRepository;
    private final UserRepository userRepository;
    private final VisibilityTranslator visibilityTranslator;
    private final Authorizations authorizations;

    /**
     * @param graph         The underlying graph data store instance, not null
     * @param workQueueRepo The work queue used to store pending operations, not null
     * @param userRepo      The system user repository used for retrieving users known to the system, not null
     * @param translator    The visibility expression translator, not null
     */
    @Inject
    public TweetVertexLoader(final Graph graph, final WorkQueueRepository workQueueRepo,
                             final UserRepository userRepo, final VisibilityTranslator translator) {
        this.graph = checkNotNull(graph);
        workQueueRepository = checkNotNull(workQueueRepo);
        userRepository = checkNotNull(userRepo);
        visibilityTranslator = checkNotNull(translator);

        authorizations = userRepository.getAuthorizations(userRepository.getSystemUser());
    }

    /**
     * Loads the vertex corresponding to the provided tweet status.  If the vertex cannot be found,
     * a new one will be created.
     *
     * @param status The status corresponding to the vertex of interest, not null
     * @return The vertex corresponding to the status provided
     */
    public Vertex loadVertex(final Status status, Priority priority) {
        checkNotNull(status);

        final String vertexId = "TWEET_" + status.getId();
        final Vertex tweetVertex = createTweetVertex(status, vertexId);

        workQueueRepository.pushGraphPropertyQueue(tweetVertex, VisalloProperties.TEXT.getProperty(tweetVertex, LoaderConstants.MULTI_VALUE_KEY), priority);

        return tweetVertex;
    }

    private Vertex createTweetVertex(final Status parsedStatus, final String vertexId) {
        final VertexBuilder vertexBuilder = graph.prepareVertex(vertexId, LoaderConstants.EMPTY_VISIBILITY);

        VisalloProperties.CONCEPT_TYPE.setProperty(vertexBuilder, TwitterOntology.CONCEPT_TYPE_TWEET, LoaderConstants.EMPTY_VISIBILITY);
        VisalloProperties.SOURCE.addPropertyValue(vertexBuilder, LoaderConstants.MULTI_VALUE_KEY, LoaderConstants.SOURCE_NAME, LoaderConstants.EMPTY_VISIBILITY);

        final String rawJson = TwitterObjectFactory.getRawJSON(parsedStatus);
        final StreamingPropertyValue rawValue = new StreamingPropertyValue(new ByteArrayInputStream(rawJson.getBytes(Charsets.UTF_8)), byte[].class);
        rawValue.searchIndex(false);
        LoaderConstants.TWITTER_RAW_JSON.setProperty(vertexBuilder, rawValue, LoaderConstants.EMPTY_VISIBILITY);

        final String statusText = parsedStatus.getText();
        StreamingPropertyValue textValue = new StreamingPropertyValue(new ByteArrayInputStream(statusText.getBytes()), String.class);

        final Metadata textMetadata = new Metadata();
        VisalloProperties.TEXT_DESCRIPTION_METADATA.setMetadata(textMetadata, "Tweet Text", LoaderConstants.EMPTY_VISIBILITY);
        VisalloProperties.TEXT.addPropertyValue(vertexBuilder, LoaderConstants.MULTI_VALUE_KEY, textValue, textMetadata, LoaderConstants.EMPTY_VISIBILITY);

        final String title = parsedStatus.getUser().getName() + ": " + statusText;
        VisalloProperties.TITLE.addPropertyValue(vertexBuilder, LoaderConstants.MULTI_VALUE_KEY, title, LoaderConstants.EMPTY_VISIBILITY);

        final Date publishedDate = parsedStatus.getCreatedAt();
        if (publishedDate != null) {
            TwitterOntology.PUBLISHED_DATE.addPropertyValue(vertexBuilder, LoaderConstants.MULTI_VALUE_KEY, publishedDate, LoaderConstants.EMPTY_VISIBILITY);
        }

        final VisibilityJson visibilityJson = new VisibilityJson();
        VisalloProperties.VISIBILITY_JSON.setProperty(vertexBuilder, visibilityJson, LoaderConstants.EMPTY_VISIBILITY);


        final Vertex tweetVertex = vertexBuilder.save(authorizations);
        graph.flush();

        return tweetVertex;
    }
}
