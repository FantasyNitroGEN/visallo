package org.visallo.web.routes.vertex;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import org.visallo.core.model.search.ElementSearchRunnerBase;
import org.visallo.core.model.search.SearchRepository;
import org.visallo.core.model.search.VertexSearchRunner;

public class VertexSearch extends ElementSearchBase implements ParameterizedHandler {
    @Inject
    public VertexSearch(SearchRepository searchRepository) {
        super((ElementSearchRunnerBase) searchRepository.findSearchRunnerByUri(VertexSearchRunner.URI));
    }
}
