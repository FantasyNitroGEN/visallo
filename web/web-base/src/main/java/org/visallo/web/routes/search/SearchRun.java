package org.visallo.web.routes.search;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Required;
import org.visallo.core.model.search.SearchRepository;
import org.visallo.core.user.User;
import org.visallo.web.VisalloResponse;
import org.visallo.web.clientapi.model.ClientApiSearch;
import org.visallo.web.parameterProviders.ActiveWorkspaceId;
import org.visallo.web.parameterProviders.VisalloBaseParameterProvider;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

public class SearchRun implements ParameterizedHandler {
    private final SearchRepository searchRepository;

    @Inject
    public SearchRun(SearchRepository searchRepository) {
        this.searchRepository = searchRepository;
    }

    @Handle
    public void handle(
            @ActiveWorkspaceId String workspaceId,
            @Required(name = "id") String id,
            User user,
            HttpServletRequest request,
            VisalloResponse response
    ) throws Exception {
        ClientApiSearch savedSearch = this.searchRepository.getSavedSearch(id, user);
        if (savedSearch == null) {
            response.respondWithNotFound("Could not find search with id " + id);
            return;
        }

        request.setAttribute(VisalloBaseParameterProvider.VISALLO_WORKSPACE_ID_HEADER_NAME, workspaceId);
        if (savedSearch.parameters != null) {
            for (Object k : savedSearch.parameters.keySet()) {
                String key = (String) k;
                Object value = savedSearch.parameters.get(key);

                if (value instanceof List) {
                    List list = (List) value;
                    String[] valueArray = new String[list.size()];
                    value = list.toArray(valueArray);
                } else {
                    value = value.toString();
                }

                request.setAttribute(key, value);
            }
        }

        RequestDispatcher dispatcher = request.getServletContext().getRequestDispatcher(savedSearch.url);
        dispatcher.forward(request, response.getHttpServletResponse());
    }
}
