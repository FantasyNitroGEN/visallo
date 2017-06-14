package org.visallo.vertexium.model.user;

import org.junit.Test;
import org.visallo.core.model.user.*;
import org.visallo.core.util.VisalloInMemoryTestBase;

import java.util.Collection;

import static org.junit.Assert.assertEquals;

public class VertexiumUserRepositoryTest extends VisalloInMemoryTestBase {
    private VertexiumUserRepository vertexiumUserRepository;

    @Override
    protected UserRepository getUserRepository() {
        if (vertexiumUserRepository != null) {
            return vertexiumUserRepository;
        }
        vertexiumUserRepository = new VertexiumUserRepository(
                getConfiguration(),
                getSimpleOrmSession(),
                getGraphAuthorizationRepository(),
                getGraph(),
                getOntologyRepository(),
                getUserSessionCounterRepository(),
                getWorkQueueRepository(),
                getLockRepository(),
                getAuthorizationRepository(),
                getPrivilegeRepository()
        ) {
            @Override
            protected Collection<UserListener> getUserListeners() {
                return VertexiumUserRepositoryTest.this.getUserListeners();
            }

            @Override
            protected AuthorizationRepository getAuthorizationRepository() {
                return VertexiumUserRepositoryTest.this.getAuthorizationRepository();
            }
        };
        return vertexiumUserRepository;
    }

    @Test
    public void testFindOrAddUser() {
        getUserRepository().findOrAddUser("12345", "testUser", null, "testPassword");

        VertexiumUser vertexiumUser = (VertexiumUser) vertexiumUserRepository.findByUsername("12345");
        assertEquals("testUser", vertexiumUser.getDisplayName());
    }
}
