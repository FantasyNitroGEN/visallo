package org.visallo.web.routes.search;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.visallo.core.model.search.SearchRepository;
import org.visallo.core.user.User;
import org.visallo.web.VisalloResponse;
import org.visallo.web.clientapi.model.ClientApiSearchListResponse;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SearchListTest {
    private SearchList searchList;

    @Mock
    private SearchRepository searchRepository;

    @Mock
    private User user;

    @Mock
    private VisalloResponse response;

    @Before
    public void setUp() {
        searchList = new SearchList(searchRepository);
    }

    @Test
    public void testList() throws Exception {
        ClientApiSearchListResponse savedSearches = new ClientApiSearchListResponse();

        when(searchRepository.getSavedSearches(eq(user))).thenReturn(savedSearches);

        searchList.handle(user, response);

        verify(response).respondWithClientApiObject(savedSearches);
    }
}