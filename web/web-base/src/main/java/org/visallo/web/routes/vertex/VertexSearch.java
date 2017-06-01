package org.visallo.web.routes.vertex;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import org.vertexium.Graph;
import org.visallo.core.model.search.VertexiumObjectSearchRunnerBase;
import org.visallo.core.model.search.SearchRepository;
import org.visallo.core.model.search.VertexSearchRunner;

public class VertexSearch extends VertexiumObjectSearchBase implements ParameterizedHandler {
    @Inject
    public VertexSearch(Graph graph, SearchRepository searchRepository) {
        super(graph, (VertexiumObjectSearchRunnerBase) searchRepository.findSearchRunnerByUri(VertexSearchRunner.URI));
    }
}
