package org.visallo.web.routes.element;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import org.visallo.core.model.search.ElementSearchRunner;
import org.visallo.core.model.search.ElementSearchRunnerBase;
import org.visallo.core.model.search.SearchRepository;
import org.visallo.web.routes.vertex.ElementSearchBase;

public class ElementSearch extends ElementSearchBase implements ParameterizedHandler {
    @Inject
    public ElementSearch(SearchRepository searchRepository) {
        super((ElementSearchRunnerBase) searchRepository.findSearchRunnerByUri(ElementSearchRunner.URI));
    }
}
