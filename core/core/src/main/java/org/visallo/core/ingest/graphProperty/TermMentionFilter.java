package org.visallo.core.ingest.graphProperty;

import com.google.inject.Inject;
import org.visallo.core.config.Configuration;
import org.visallo.core.security.VisibilityTranslator;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.Vertex;

public abstract class TermMentionFilter {
    private Configuration configuration;
    private Graph graph;
    private VisibilityTranslator visibilityTranslator;

    public void prepare(TermMentionFilterPrepareData termMentionFilterPrepareData) throws Exception {
    }

    public abstract void apply(Vertex artifactGraphVertex, Iterable<Vertex> termMentions, Authorizations authorizations) throws Exception;

    @Inject
    public final void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    protected final Configuration getConfiguration() {
        return configuration;
    }

    protected final Graph getGraph() {
        return graph;
    }

    @Inject
    public final void setGraph(Graph graph) {
        this.graph = graph;
    }

    public VisibilityTranslator getVisibilityTranslator() {
        return visibilityTranslator;
    }

    @Inject
    public final void setVisibilityTranslator(VisibilityTranslator visibilityTranslator) {
        this.visibilityTranslator = visibilityTranslator;
    }
}
