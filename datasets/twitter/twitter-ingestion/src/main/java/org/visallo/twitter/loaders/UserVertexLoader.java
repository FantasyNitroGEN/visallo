package org.visallo.twitter.loaders;

import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.security.VisibilityTranslator;
import org.visallo.twitter.TwitterOntology;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.Vertex;
import org.vertexium.VertexBuilder;

import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Responsible for loading Twitter user vertex information to/from the underlying data store
 */
public class UserVertexLoader {
    private final Cache<String, Vertex> userVertexCache = CacheBuilder.newBuilder().expireAfterWrite(15, TimeUnit.MINUTES).build();

    private final Graph graph;
    private final WorkQueueRepository workQueueRepository;
    private final UserRepository userRepository;
    private final Authorizations authorizations;
    private LoaderConstants loaderConstants;

    /**
     * @param graph         The underlying graph data store instance, not null
     * @param workQueueRepo The work queue used to store pending operations, not null
     * @param userRepo      The system user repository used for retrieving users known to the system, not null
     *                      loaderConstants = new LoaderConstants(translator);
     */
    @Inject
    public UserVertexLoader(final Graph graph, final WorkQueueRepository workQueueRepo, final UserRepository userRepo,
                            final VisibilityTranslator visibilityTranslator) {
        this.graph = checkNotNull(graph);
        workQueueRepository = checkNotNull(workQueueRepo);
        userRepository = checkNotNull(userRepo);

        authorizations = userRepository.getAuthorizations(userRepository.getSystemUser());
        loaderConstants = new LoaderConstants(visibilityTranslator);
    }

    /**
     * Loads the vertex corresponding to the provided vertex details.  If the vertex cannot be found,
     * a new one will be created.
     *
     * @param userDetails The details corresponding to the vertex of interest, not null
     * @return The vertex corresponding to the details provided
     */
    public Vertex loadVertex(final UserVertexDetails userDetails, Priority priority) {
        checkNotNull(userDetails);

        final String profileImageUrl = userDetails.getProfileImageUrl();

        String vertexId = "TWITTER_USER_" + userDetails.getId();
        Vertex userVertex = userVertexCache.getIfPresent(vertexId);
        if (userVertex != null) {
            return userVertex;
        }

        userVertex = graph.getVertex(vertexId, authorizations);
        if (userVertex == null) {
            userVertex = createTwitterUserVertex(userDetails, vertexId);

            workQueueRepository.pushGraphPropertyQueue(userVertex, VisalloProperties.TITLE.getProperty(userVertex, LoaderConstants.MULTI_VALUE_KEY), priority);

            if (!Strings.isNullOrEmpty(profileImageUrl)) {
                workQueueRepository.pushGraphPropertyQueue(userVertex, TwitterOntology.PROFILE_IMAGE_URL.getProperty(userVertex, LoaderConstants.MULTI_VALUE_KEY), priority);
            }

            workQueueRepository.pushGraphPropertyQueue(userVertex, TwitterOntology.SCREEN_NAME.getProperty(userVertex, LoaderConstants.MULTI_VALUE_KEY), priority);
        }

        userVertexCache.put(vertexId, userVertex);

        return userVertex;
    }


    private Vertex createTwitterUserVertex(final UserVertexDetails userDetails, final String vertexId) {
        final VertexBuilder vertexBuilder = graph.prepareVertex(vertexId, loaderConstants.getEmptyVisibility());

        // Set core ontology properties
        VisalloProperties.CONCEPT_TYPE.setProperty(vertexBuilder, TwitterOntology.CONCEPT_TYPE_USER, loaderConstants.getEmptyVisibility());
        VisalloProperties.SOURCE.addPropertyValue(vertexBuilder, LoaderConstants.MULTI_VALUE_KEY, LoaderConstants.SOURCE_NAME, loaderConstants.getEmptyVisibility());
        VisalloProperties.TITLE.addPropertyValue(vertexBuilder, LoaderConstants.MULTI_VALUE_KEY, userDetails.getName(), loaderConstants.getEmptyVisibility());


        // Add tweet properties
        TwitterOntology.SCREEN_NAME.addPropertyValue(vertexBuilder, LoaderConstants.MULTI_VALUE_KEY, userDetails.getScreenName(), loaderConstants.getEmptyVisibility());

        final String profileImageUrl = userDetails.getProfileImageUrl();
        if (!Strings.isNullOrEmpty(profileImageUrl)) {
            TwitterOntology.PROFILE_IMAGE_URL.addPropertyValue(vertexBuilder, LoaderConstants.MULTI_VALUE_KEY, profileImageUrl, loaderConstants.getEmptyVisibility());
        }

        final Vertex userVertex = vertexBuilder.save(authorizations);
        graph.flush();

        return userVertex;
    }

}
