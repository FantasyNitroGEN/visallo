package org.visallo.web.routes.search;

import org.mockito.Mock;
import org.vertexium.Authorizations;
import org.vertexium.Visibility;
import org.visallo.core.model.directory.DirectoryRepository;
import org.visallo.core.model.search.SearchRepository;
import org.visallo.web.routes.RouteTestBase;

import java.io.IOException;

public abstract class SearchRouteTestBase extends RouteTestBase {
    protected Authorizations authorizations;
    protected Visibility visibility;

    @Mock
    protected DirectoryRepository directoryRepository;

    @Mock
    protected SearchRepository searchRepository;

    @Override
    protected void before() throws IOException {
        super.before();

        visibility = new Visibility("");
        authorizations = graph.createAuthorizations("");
    }
}
