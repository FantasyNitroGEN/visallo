package org.visallo.core.model.search;

import org.json.JSONObject;
import org.visallo.core.user.User;
import org.visallo.web.clientapi.model.ClientApiSearch;
import org.visallo.web.clientapi.model.ClientApiSearchListResponse;

public abstract class SearchRepository {
    public abstract String saveSearch(User user, String id, String name, String url, JSONObject searchParameters);

    public abstract ClientApiSearchListResponse getSavedSearches(User user);

    public abstract ClientApiSearch getSavedSearch(String id, User user);

    public abstract void deleteSearch(String id, User user);
}
