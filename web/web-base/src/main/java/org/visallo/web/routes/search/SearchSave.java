package org.visallo.web.routes.search;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Optional;
import com.v5analytics.webster.annotations.Required;
import org.json.JSONObject;
import org.visallo.core.model.search.SearchRepository;
import org.visallo.core.user.User;
import org.visallo.web.VisalloResponse;
import org.visallo.web.clientapi.model.ClientApiSaveSearchResponse;

public class SearchSave implements ParameterizedHandler {
    private final SearchRepository searchRepository;

    @Inject
    public SearchSave(SearchRepository searchRepository) {
        this.searchRepository = searchRepository;
    }

    @Handle
    public void handle(
            @Optional(name = "id") String id,
            @Optional(name = "name") String name,
            @Required(name = "url") String url,
            @Required(name = "parameters") JSONObject searchParameters,
            User user,
            VisalloResponse response
    ) throws Exception {
        id = this.searchRepository.saveSearch(user, id, name, url, searchParameters);
        ClientApiSaveSearchResponse saveSearchResponse = new ClientApiSaveSearchResponse();
        saveSearchResponse.id = id;
        response.respondWithClientApiObject(saveSearchResponse);
    }
}
