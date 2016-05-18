package org.visallo.core.model.directory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.user.User;
import org.visallo.web.clientapi.model.DirectoryEntity;
import org.visallo.web.clientapi.model.DirectoryGroup;
import org.visallo.web.clientapi.model.DirectoryPerson;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UserRepositoryDirectoryRepositoryTest {
    private UserRepositoryDirectoryRepository userRepositoryDirectoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private User user;

    @Mock
    private User userJoe;

    @Before
    public void before() {
        userRepositoryDirectoryRepository = new UserRepositoryDirectoryRepository(userRepository);

        when(userJoe.getUserId()).thenReturn("USER_joe");
        when(userJoe.getDisplayName()).thenReturn("Joe");
    }

    @Test
    public void testSearchPeople() {
        List<User> users = new ArrayList<>();
        users.add(userJoe);
        when(userRepository.find("joe")).thenReturn(users);

        List<DirectoryPerson> results = userRepositoryDirectoryRepository.searchPeople("joe", user);
        assertEquals(1, results.size());
        assertEquals("person", results.get(0).getType());
        assertEquals("USER_joe", results.get(0).getId());
    }

    @Test
    public void testSearchGroups() {
        List<DirectoryGroup> results = userRepositoryDirectoryRepository.searchGroups("*", user);
        assertEquals(0, results.size());
    }

    @Test
    public void testFindById() {
        when(userRepository.findById("USER_joe")).thenReturn(userJoe);

        DirectoryEntity result = userRepositoryDirectoryRepository.findById("USER_joe", user);
        assertEquals("USER_joe", result.getId());
    }

    @Test
    public void testGetDirectoryEntityId() {
        String directoryEntityId = userRepositoryDirectoryRepository.getDirectoryEntityId(userJoe);
        assertEquals("USER_joe", directoryEntityId);
    }
}