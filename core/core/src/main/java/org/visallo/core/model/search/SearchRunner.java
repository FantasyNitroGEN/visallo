package org.visallo.core.model.search;

import org.vertexium.Authorizations;
import org.visallo.core.user.User;

public abstract class SearchRunner {
    public abstract String getUri();

    public abstract SearchResults run(
            SearchOptions searchOptions,
            User user,
            Authorizations authorizations
    );
}
