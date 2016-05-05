package org.visallo.web.routes.edge;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import org.visallo.core.model.search.EdgeSearchRunner;
import org.visallo.core.model.search.ElementSearchRunnerBase;
import org.visallo.core.model.search.SearchRepository;
import org.visallo.web.routes.vertex.ElementSearchBase;

public class EdgeSearch extends ElementSearchBase implements ParameterizedHandler {
    @Inject
    public EdgeSearch(SearchRepository searchRepository) {
        super((ElementSearchRunnerBase) searchRepository.findSearchRunnerByUri(EdgeSearchRunner.URI));
    }
}
