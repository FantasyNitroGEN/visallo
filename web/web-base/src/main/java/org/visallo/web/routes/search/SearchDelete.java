package org.visallo.web.routes.search;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Required;
import org.visallo.core.exception.VisalloResourceNotFoundException;
import org.visallo.core.model.search.SearchRepository;
import org.visallo.core.user.User;
import org.visallo.web.clientapi.model.ClientApiSearch;

public class SearchDelete implements ParameterizedHandler {
    private final SearchRepository searchRepository;

    @Inject
    public SearchDelete(SearchRepository searchRepository) {
        this.searchRepository = searchRepository;
    }

    @Handle
    public void handle(
            @Required(name = "id") String id,
            User user
    ) throws Exception {
        ClientApiSearch savedSearch = this.searchRepository.getSavedSearch(id, user);
        if (savedSearch == null) {
            throw new VisalloResourceNotFoundException("Could not find saved search with id " + id);
        }

        this.searchRepository.deleteSearch(id, user);
    }
}
