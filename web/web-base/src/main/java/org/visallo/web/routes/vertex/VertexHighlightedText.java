package org.visallo.web.routes.vertex;

import com.google.inject.Inject;
import org.visallo.core.EntityHighlighter;
import org.visallo.core.config.Configuration;
import org.visallo.core.ingest.video.VideoTranscript;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.properties.MediaVisalloProperties;
import org.visallo.core.model.termMention.TermMentionRepository;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.user.User;
import org.visallo.core.util.JsonSerializer;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import com.v5analytics.webster.HandlerChain;
import org.visallo.web.BaseRequestHandler;
import org.apache.commons.io.IOUtils;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.Vertex;
import org.vertexium.property.StreamingPropertyValue;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class VertexHighlightedText extends BaseRequestHandler {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(VertexHighlightedText.class);
    private final Graph graph;
    private final EntityHighlighter entityHighlighter;
    private final TermMentionRepository termMentionRepository;

    @Inject
    public VertexHighlightedText(
            final Graph graph,
            final UserRepository userRepository,
            final EntityHighlighter entityHighlighter,
            final WorkspaceRepository workspaceRepository,
            final Configuration configuration,
            final TermMentionRepository termMentionRepository) {
        super(userRepository, workspaceRepository, configuration);
        this.graph = graph;
        this.entityHighlighter = entityHighlighter;
        this.termMentionRepository = termMentionRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);
        Authorizations authorizationsWithTermMention = termMentionRepository.getAuthorizations(authorizations);
        String workspaceId = getActiveWorkspaceId(request);

        String graphVertexId = getRequiredParameter(request, "graphVertexId");
        String propertyKey = getRequiredParameter(request, "propertyKey");

        Vertex artifactVertex = graph.getVertex(graphVertexId, authorizations);
        if (artifactVertex == null) {
            respondWithNotFound(response);
            return;
        }

        StreamingPropertyValue textPropertyValue = VisalloProperties.TEXT.getPropertyValue(artifactVertex, propertyKey);
        if (textPropertyValue != null) {
            LOGGER.debug("returning text for vertexId:%s property:%s", artifactVertex.getId(), propertyKey);
            String highlightedText;
            String text = IOUtils.toString(textPropertyValue.getInputStream(), "UTF-8");
            if (text == null) {
                highlightedText = "";
            } else {
                Iterable<Vertex> termMentions = termMentionRepository.findBySourceGraphVertexAndPropertyKey(artifactVertex.getId(), propertyKey, authorizationsWithTermMention);
                highlightedText = entityHighlighter.getHighlightedText(text, termMentions, workspaceId, authorizationsWithTermMention);
            }

            respondWithHtml(response, highlightedText);
            return;
        }

        VideoTranscript videoTranscript = MediaVisalloProperties.VIDEO_TRANSCRIPT.getPropertyValue(artifactVertex, propertyKey);
        if (videoTranscript != null) {
            LOGGER.debug("returning video transcript for vertexId:%s property:%s", artifactVertex.getId(), propertyKey);
            Iterable<Vertex> termMentions = termMentionRepository.findBySourceGraphVertexAndPropertyKey(artifactVertex.getId(), propertyKey, authorizations);
            VideoTranscript highlightedVideoTranscript = entityHighlighter.getHighlightedVideoTranscript(videoTranscript, termMentions, workspaceId, authorizations);
            respondWithJson(response, highlightedVideoTranscript.toJson());
            return;
        }

        videoTranscript = JsonSerializer.getSynthesisedVideoTranscription(artifactVertex, propertyKey);
        if (videoTranscript != null) {
            LOGGER.debug("returning synthesised video transcript for vertexId:%s property:%s", artifactVertex.getId(), propertyKey);
            Iterable<Vertex> termMentions = termMentionRepository.findBySourceGraphVertexAndPropertyKey(artifactVertex.getId(), propertyKey, authorizations);
            VideoTranscript highlightedVideoTranscript = entityHighlighter.getHighlightedVideoTranscript(videoTranscript, termMentions, workspaceId, authorizationsWithTermMention);
            respondWithJson(response, highlightedVideoTranscript.toJson());
            return;
        }

        respondWithNotFound(response);
    }
}
