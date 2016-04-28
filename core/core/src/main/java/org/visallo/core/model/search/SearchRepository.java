package org.visallo.core.model.search;

import org.json.JSONObject;
import org.visallo.core.user.User;
import org.visallo.web.clientapi.model.ClientApiSearch;
import org.visallo.web.clientapi.model.ClientApiSearchListResponse;

public abstract class SearchRepository {
    public abstract String saveSearch(
            String id,
            String name,
            String url,
            JSONObject searchParameters,
            User user
    );

    public abstract String saveGlobalSearch(
            String id,
            String name,
            String url,
            JSONObject searchParameters,
            User user
    );

    public abstract ClientApiSearchListResponse getSavedSearches(User user);

    public abstract ClientApiSearch getSavedSearch(String id, User user);

    public abstract void deleteSearch(String id, User user);
}
