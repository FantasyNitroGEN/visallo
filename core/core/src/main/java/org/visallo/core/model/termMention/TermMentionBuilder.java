package org.visallo.core.model.termMention;

import org.vertexium.*;
import org.vertexium.mutation.EdgeMutation;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.security.VisalloVisibility;
import org.visallo.core.security.VisibilityTranslator;
import org.visallo.core.user.User;
import org.visallo.core.util.ClientApiConverter;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.clientapi.model.VisibilityJson;

import java.util.Date;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class TermMentionBuilder {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(TermMentionBuilder.class);
    private static final String TERM_MENTION_VERTEX_ID_PREFIX = "TM_";
    private Vertex outVertex;
    private String propertyKey;
    private String propertyName;
    private long start = -1;
    private long end = -1;
    private String title;
    private String conceptIri;
    private VisibilityJson visibilityJson;
    private String process;
    private String resolvedToVertexId;
    private String resolvedEdgeId;
    private String snippet;

    public TermMentionBuilder() {

    }

    /**
     * Copy an existing term mention.
     *
     * @param existingTermMention The term mention you would like to copy.
     * @param outVertex           The vertex that contains this term mention (ie Document, Html page, etc).
     */
    public TermMentionBuilder(Vertex existingTermMention, Vertex outVertex) {
        this.outVertex = outVertex;
        this.propertyKey = VisalloProperties.TERM_MENTION_PROPERTY_KEY.getPropertyValue(existingTermMention);
        this.propertyName = VisalloProperties.TERM_MENTION_PROPERTY_NAME.getPropertyValue(existingTermMention);
        this.start = VisalloProperties.TERM_MENTION_START_OFFSET.getPropertyValue(existingTermMention, 0);
        this.end = VisalloProperties.TERM_MENTION_END_OFFSET.getPropertyValue(existingTermMention, 0);
        this.title = VisalloProperties.TERM_MENTION_TITLE.getPropertyValue(existingTermMention, "");
        this.snippet = VisalloProperties.TERM_MENTION_SNIPPET.getPropertyValue(existingTermMention);
        this.conceptIri = VisalloProperties.TERM_MENTION_CONCEPT_TYPE.getPropertyValue(existingTermMention, "");
        this.visibilityJson = VisalloProperties.TERM_MENTION_VISIBILITY_JSON.getPropertyValue(existingTermMention, new VisibilityJson());
    }

    /**
     * The start offset within the property text that this term mention appears.
     */
    public TermMentionBuilder start(long start) {
        this.start = start;
        return this;
    }

    /**
     * The end offset within the property text that this term mention appears.
     */
    public TermMentionBuilder end(long end) {
        this.end = end;
        return this;
    }

    /**
     * The property key of the {@link VisalloProperties#TEXT} that this term mention references.
     */
    public TermMentionBuilder propertyKey(String propertyKey) {
        this.propertyKey = propertyKey;
        return this;
    }

    /**
     * The property name of the {@link VisalloProperties#TEXT} that this term mention references.
     */
    public TermMentionBuilder propertyName(String propertyName) {
        this.propertyName = propertyName;
        return this;
    }

    /**
     * Visibility JSON string. This will be applied to the newly created term.
     */
    public TermMentionBuilder visibilityJson(String visibilityJsonString) {
        return visibilityJson(visibilityJsonStringToJson(visibilityJsonString));
    }

    /**
     * Visibility JSON object. This will be applied to the newly created term.
     */
    public TermMentionBuilder visibilityJson(VisibilityJson visibilitySource) {
        this.visibilityJson = visibilitySource;
        return this;
    }

    private static VisibilityJson visibilityJsonStringToJson(String visibilityJsonString) {
        if (visibilityJsonString == null) {
            return new VisibilityJson();
        }
        if (visibilityJsonString.length() == 0) {
            return new VisibilityJson();
        }
        return ClientApiConverter.toClientApi(visibilityJsonString, VisibilityJson.class);
    }

    /**
     * If this is a resolved term mention. This allows setting that information.
     *
     * @param resolvedToVertex The vertex this term mention resolves to.
     * @param resolvedEdge     The edge that links the source vertex to the resolved vertex.
     */
    public TermMentionBuilder resolvedTo(Vertex resolvedToVertex, Edge resolvedEdge) {
        return resolvedTo(resolvedToVertex.getId(), resolvedEdge.getId());
    }

    /**
     * If this is a resolved term mention. This allows setting that information.
     *
     * @param resolvedToVertexId The vertex id this term mention resolves to.
     * @param resolvedEdgeId     The edge id that links the source vertex to the resolved vertex.
     */
    public TermMentionBuilder resolvedTo(String resolvedToVertexId, String resolvedEdgeId) {
        this.resolvedToVertexId = resolvedToVertexId;
        this.resolvedEdgeId = resolvedEdgeId;
        return this;
    }

    /**
     * The process that created this term mention.
     */
    public TermMentionBuilder process(String process) {
        this.process = process;
        return this;
    }

    /**
     * The vertex that contains this term mention (ie Document, Html page, etc).
     */
    public TermMentionBuilder outVertex(Vertex outVertex) {
        this.outVertex = outVertex;
        return this;
    }

    /**
     * The title/text of this term mention. (ie Job Ferner, Paris, etc).
     */
    public TermMentionBuilder title(String title) {
        this.title = title;
        return this;
    }

    public TermMentionBuilder snippet(String snippet) {
        this.snippet = snippet;
        return this;
    }

    /**
     * The concept type of this term mention.
     */
    public TermMentionBuilder conceptIri(String conceptIri) {
        this.conceptIri = conceptIri;
        return this;
    }

    /**
     * Saves the term mention to the graph.
     * <p/>
     * The resulting graph for non-resolved terms will be:
     * <p/>
     * Source  -- Has --> Term
     * Vertex             Mention
     * <p/>
     * The resulting graph for resolved terms will be:
     * <p/>
     * Source  -- Has --> Term    -- Resolved To --> Resolved
     * Vertex             Mention                    Vertex
     */
    public Vertex save(Graph graph, VisibilityTranslator visibilityTranslator, User user, Authorizations authorizations) {
        checkNotNull(outVertex, "outVertex cannot be null");
        checkNotNull(propertyKey, "propertyKey cannot be null");
        checkNotNull(title, "title cannot be null");
        checkArgument(title.length() > 0, "title cannot be an empty string");
        checkNotNull(conceptIri, "conceptIri cannot be null");
        checkArgument(conceptIri.length() > 0, "conceptIri cannot be an empty string");
        checkNotNull(visibilityJson, "visibilityJson cannot be null");
        checkNotNull(process, "process cannot be null");
        checkArgument(process.length() > 0, "process cannot be an empty string");
        checkArgument(start >= 0, "start must be greater than or equal to 0");
        checkArgument(end >= 0, "start must be greater than or equal to 0");

        if (propertyName == null) {
            LOGGER.warn("Not setting a propertyName when building a term mention is deprecated");
        }

        Date now = new Date();
        String vertexId = createVertexId();
        Visibility defaultVisibility = visibilityTranslator.getDefaultVisibility();
        Visibility visibility = VisalloVisibility.and(visibilityTranslator.toVisibility(this.visibilityJson).getVisibility(), TermMentionRepository.VISIBILITY_STRING);
        VertexBuilder vertexBuilder = graph.prepareVertex(vertexId, visibility);
        VisalloProperties.TERM_MENTION_VISIBILITY_JSON.setProperty(vertexBuilder, this.visibilityJson, visibility);
        VisalloProperties.TERM_MENTION_CONCEPT_TYPE.setProperty(vertexBuilder, this.conceptIri, visibility);
        VisalloProperties.TERM_MENTION_TITLE.setProperty(vertexBuilder, this.title, visibility);
        VisalloProperties.TERM_MENTION_START_OFFSET.setProperty(vertexBuilder, this.start, visibility);
        VisalloProperties.TERM_MENTION_END_OFFSET.setProperty(vertexBuilder, this.end, visibility);
        VisalloProperties.TERM_MENTION_PROCESS.setProperty(vertexBuilder, this.process, visibility);
        VisalloProperties.TERM_MENTION_PROPERTY_KEY.setProperty(vertexBuilder, this.propertyKey, visibility);
        if (this.propertyName != null) {
            VisalloProperties.TERM_MENTION_PROPERTY_NAME.setProperty(vertexBuilder, this.propertyName, visibility);
        }
        if (this.resolvedEdgeId != null) {
            VisalloProperties.TERM_MENTION_RESOLVED_EDGE_ID.setProperty(vertexBuilder, this.resolvedEdgeId, visibility);
        }
        if (this.snippet != null) {
            VisalloProperties.TERM_MENTION_SNIPPET.setProperty(vertexBuilder, this.snippet, visibility);
        }
        if (this.resolvedToVertexId != null) {
            VisalloProperties.TERM_MENTION_FOR_ELEMENT_ID.setProperty(vertexBuilder, resolvedToVertexId, visibility);
            VisalloProperties.TERM_MENTION_FOR_TYPE.setProperty(vertexBuilder, TermMentionFor.VERTEX, visibility);
        }
        Vertex termMentionVertex = vertexBuilder.save(authorizations);

        String hasTermMentionId = vertexId + "_hasTermMention";
        EdgeBuilder termMentionEdgeBuilder = graph.prepareEdge(hasTermMentionId, this.outVertex, termMentionVertex, VisalloProperties.TERM_MENTION_LABEL_HAS_TERM_MENTION, visibility);
        VisalloProperties.TERM_MENTION_VISIBILITY_JSON.setProperty(termMentionEdgeBuilder, this.visibilityJson, visibility);
        VisalloProperties.MODIFIED_BY.setProperty(termMentionEdgeBuilder, user.getUserId(), defaultVisibility);
        VisalloProperties.MODIFIED_DATE.setProperty(termMentionEdgeBuilder, now, defaultVisibility);
        termMentionEdgeBuilder.save(authorizations);
        if (this.resolvedToVertexId != null) {
            String resolvedToId = vertexId + "_resolvedTo";
            EdgeMutation resolvedToEdgeBuilder = graph.prepareEdge(resolvedToId, termMentionVertex.getId(), resolvedToVertexId, VisalloProperties.TERM_MENTION_LABEL_RESOLVED_TO, visibility);
            VisalloProperties.TERM_MENTION_VISIBILITY_JSON.setProperty(resolvedToEdgeBuilder, this.visibilityJson, visibility);
            VisalloProperties.MODIFIED_BY.setProperty(resolvedToEdgeBuilder, user.getUserId(), defaultVisibility);
            VisalloProperties.MODIFIED_DATE.setProperty(resolvedToEdgeBuilder, now, defaultVisibility);
            resolvedToEdgeBuilder.save(authorizations);
        }

        return termMentionVertex;
    }

    private String createVertexId() {
        String id = TERM_MENTION_VERTEX_ID_PREFIX + this.outVertex.getId();
        if (this.propertyName != null) {
            id += "-" + this.propertyName;
        }
        return id
                + "-"
                + this.propertyKey
                + "-"
                + this.start
                + "-"
                + this.end
                + "-"
                + this.process;
    }
}
