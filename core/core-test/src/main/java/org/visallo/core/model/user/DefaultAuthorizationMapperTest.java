package org.visallo.core.model.user;

import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.vertexium.Authorizations;
import org.vertexium.inmemory.InMemoryAuthorizations;
import org.visallo.core.user.User;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DefaultAuthorizationMapperTest {
    private DefaultAuthorizationMapper defaultAuthorizationMapper;

    @Mock
    private User user;

    @Mock
    private UserRepository userRepository;

    @Before
    public void before() {
        defaultAuthorizationMapper = new DefaultAuthorizationMapper(userRepository);
    }

    @Test
    public void testGetPrivilegesForNewUser() {
        HashSet<String> expected = Sets.newHashSet("userRepositoryPrivilege1", "userRepositoryPrivilege2");
        when(userRepository.getDefaultPrivileges()).thenReturn(expected);

        AuthorizationContext authorizationContext = new TestAuthorizationContext(null);
        Set<String> privileges = defaultAuthorizationMapper.getPrivileges(authorizationContext);
        assertEquals(expected, privileges);
    }

    @Test
    public void testGetPrivilegesForExisting() {
        HashSet<String> expected = Sets.newHashSet("userPrivilege1", "userPrivilege2");
        when(user.getPrivileges()).thenReturn(expected);

        AuthorizationContext authorizationContext = new TestAuthorizationContext(user);
        Set<String> privileges = defaultAuthorizationMapper.getPrivileges(authorizationContext);
        assertEquals(expected, privileges);
    }

    @Test
    public void testGetAuthorizationsForNewUser() {
        HashSet<String> expected = Sets.newHashSet("userRepositoryAuthorization1", "userRepositoryAuthorization2");
        when(userRepository.getDefaultAuthorizations()).thenReturn(expected);

        AuthorizationContext authorizationContext = new TestAuthorizationContext(null);
        Set<String> privileges = defaultAuthorizationMapper.getAuthorizations(authorizationContext);
        assertEquals(expected, privileges);
    }

    @Test
    public void testGetAuthorizationsForExisting() {
        String[] authorizationsArray = {"userAuthorization1", "userAuthorization2"};
        Authorizations authorizations = new InMemoryAuthorizations(authorizationsArray);
        when(userRepository.getAuthorizations(eq(user))).thenReturn(authorizations);

        AuthorizationContext authorizationContext = new TestAuthorizationContext(user);
        Set<String> privileges = defaultAuthorizationMapper.getAuthorizations(authorizationContext);
        assertEquals(Sets.newHashSet(authorizationsArray), privileges);
    }

    public static class TestAuthorizationContext extends AuthorizationContext {
        public TestAuthorizationContext(User existingUser) {
            super(existingUser);
        }
    }
}