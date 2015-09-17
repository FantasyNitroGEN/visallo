package org.visallo.web.routes.search;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.visallo.core.model.search.SearchRepository;
import org.visallo.core.user.User;
import org.visallo.web.VisalloResponse;
import org.visallo.web.clientapi.model.ClientApiSearch;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SearchRunTest {
    private SearchRun searchRun;

    @Mock
    private SearchRepository searchRepository;

    @Mock
    private User user;

    @Mock
    private HttpServletRequest request;

    @Mock
    private VisalloResponse response;

    @Mock
    private ServletContext servletContext;

    @Mock
    private RequestDispatcher requestDispatcher;

    @Mock
    private HttpServletResponse servletResponse;

    @Before
    public void setUp() {
        searchRun = new SearchRun(searchRepository);

        when(request.getServletContext()).thenReturn(servletContext);
        when(response.getHttpServletResponse()).thenReturn(servletResponse);
    }

    @Test
    public void testRun() throws Exception {
        String workspaceId = "WS123";
        String id = "234";

        ClientApiSearch savedSearch = new ClientApiSearch();
        savedSearch.id = id;
        savedSearch.url = "/vertex/search";
        savedSearch.parameters = new HashMap<>();
        savedSearch.parameters.put("param1", "value1");

        when(servletContext.getRequestDispatcher(savedSearch.url)).thenReturn(requestDispatcher);
        when(searchRepository.getSavedSearch(eq(id), eq(user))).thenReturn(savedSearch);

        searchRun.handle(workspaceId, id, user, request, response);

        verify(request).setAttribute("param1", "value1");
        verify(requestDispatcher).forward(request, servletResponse);
    }

    @Test
    public void testRunNotFound() throws Exception {
        String workspaceId = "WS123";
        String id = "234";

        when(searchRepository.getSavedSearch(eq(id), eq(user))).thenReturn(null);

        searchRun.handle(workspaceId, id, user, request, response);

        verify(response).respondWithNotFound(any(String.class));
    }
}