package org.visallo.web.routes.search;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import org.visallo.core.model.search.SearchRepository;
import org.visallo.core.user.User;
import org.visallo.web.clientapi.model.ClientApiSearchListResponse;

public class SearchList implements ParameterizedHandler {
    private final SearchRepository searchRepository;

    @Inject
    public SearchList(SearchRepository searchRepository) {
        this.searchRepository = searchRepository;
    }

    @Handle
    public ClientApiSearchListResponse handle(User user) throws Exception {
        return this.searchRepository.getSavedSearches(user);
    }
}
