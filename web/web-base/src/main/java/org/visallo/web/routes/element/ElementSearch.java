package org.visallo.web.routes.element;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import org.vertexium.Graph;
import org.visallo.core.model.search.ElementSearchRunner;
import org.visallo.core.model.search.VertexiumObjectSearchRunnerBase;
import org.visallo.core.model.search.SearchRepository;
import org.visallo.web.routes.vertex.VertexiumObjectSearchBase;

public class ElementSearch extends VertexiumObjectSearchBase implements ParameterizedHandler {
    @Inject
    public ElementSearch(Graph graph, SearchRepository searchRepository) {
        super(graph, (VertexiumObjectSearchRunnerBase) searchRepository.findSearchRunnerByUri(ElementSearchRunner.URI));
    }
}
