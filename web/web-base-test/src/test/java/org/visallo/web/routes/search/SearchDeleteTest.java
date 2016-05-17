package org.visallo.web.routes.search;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.exception.VisalloResourceNotFoundException;
import org.visallo.core.model.search.SearchRepository;
import org.visallo.core.user.User;
import org.visallo.web.clientapi.model.ClientApiSearch;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SearchDeleteTest {
    private SearchDelete searchDelete;

    @Mock
    private SearchRepository searchRepository;

    @Mock
    private User user;

    @Before
    public void setUp() {
        searchDelete = new SearchDelete(searchRepository);
    }

    @Test
    public void testDeleteExists() throws Exception {
        String id = "1234";
        ClientApiSearch savedSearch = new ClientApiSearch();

        when(searchRepository.getSavedSearch(eq(id), eq(user))).thenReturn(savedSearch);

        searchDelete.handle(id, user);

        verify(searchRepository).deleteSearch(id, user);
    }

    @Test
    public void testDeleteNotExists() throws Exception {
        String id = "1234";

        when(searchRepository.getSavedSearch(eq(id), eq(user))).thenReturn(null);

        try {
            searchDelete.handle(id, user);
            throw new VisalloException("Should have thrown not found");
        } catch (VisalloResourceNotFoundException ex) {
            // OK
        }
    }
}