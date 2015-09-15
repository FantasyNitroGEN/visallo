package org.visallo.web.routes.search;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.visallo.core.model.search.SearchRepository;
import org.visallo.core.user.User;
import org.visallo.web.VisalloResponse;
import org.visallo.web.clientapi.model.ClientApiSaveSearchResponse;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SearchSaveTest {
    private SearchSave searchSave;

    @Mock
    private SearchRepository searchRepository;

    @Mock
    private User user;

    @Mock
    private VisalloResponse response;

    @Before
    public void setUp() {
        searchSave = new SearchSave(searchRepository);
    }

    @Test
    public void testHandleFirstSave() throws Exception {
        String id = null;
        String name = null;
        String url = "/vertex/search";
        JSONObject searchParameters = new JSONObject();
        String newId = "1234";

        when(searchRepository.saveSearch(eq(user), eq(id), eq(name), eq(url), eq(searchParameters))).thenReturn(newId);

        searchSave.handle(id, name, url, searchParameters, user, response);

        ClientApiSaveSearchResponse expectedResult = new ClientApiSaveSearchResponse();
        expectedResult.id = newId;
        verify(response).respondWithClientApiObject(expectedResult);
    }

    @Test
    public void testHandleExistingSave() throws Exception {
        String id = "1234";
        String name = null;
        String url = "/vertex/search";
        JSONObject searchParameters = new JSONObject();
        String newId = "1234";

        when(searchRepository.saveSearch(eq(user), eq(id), eq(name), eq(url), eq(searchParameters))).thenReturn(newId);

        searchSave.handle(id, name, url, searchParameters, user, response);

        ClientApiSaveSearchResponse expectedResult = new ClientApiSaveSearchResponse();
        expectedResult.id = newId;
        verify(response).respondWithClientApiObject(expectedResult);
    }
}