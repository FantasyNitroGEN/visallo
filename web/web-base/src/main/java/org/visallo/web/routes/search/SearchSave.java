package org.visallo.web.routes.search;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Optional;
import com.v5analytics.webster.annotations.Required;
import org.json.JSONObject;
import org.visallo.core.model.search.SearchRepository;
import org.visallo.core.user.User;
import org.visallo.web.clientapi.model.ClientApiSaveSearchResponse;

public class SearchSave implements ParameterizedHandler {
    private final SearchRepository searchRepository;

    @Inject
    public SearchSave(SearchRepository searchRepository) {
        this.searchRepository = searchRepository;
    }

    @Handle
    public ClientApiSaveSearchResponse handle(
            @Optional(name = "id") String id,
            @Optional(name = "name") String name,
            @Required(name = "url") String url,
            @Required(name = "parameters") JSONObject searchParameters,
            @Optional(name = "global", defaultValue = "false") boolean global,
            User user
    ) throws Exception {
        if (global) {
            id = this.searchRepository.saveGlobalSearch(id, name, url, searchParameters, user);
        } else {
            id = this.searchRepository.saveSearch(id, name, url, searchParameters, user);
        }
        ClientApiSaveSearchResponse saveSearchResponse = new ClientApiSaveSearchResponse();
        saveSearchResponse.id = id;
        return saveSearchResponse;
    }
}
