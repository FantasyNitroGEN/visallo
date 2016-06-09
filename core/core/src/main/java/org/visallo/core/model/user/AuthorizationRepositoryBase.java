package org.visallo.core.model.user;

import org.vertexium.Graph;
import org.visallo.core.bootstrap.InjectHelper;
import org.visallo.core.user.User;

import java.util.Collections;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class AuthorizationRepositoryBase implements AuthorizationRepository {
    private final Graph graph;
    private UserRepository userRepository;

    protected AuthorizationRepositoryBase(Graph graph) {
        this.graph = graph;
    }

    public org.vertexium.Authorizations getGraphAuthorizations(User user, String... additionalAuthorizations) {
        checkNotNull(user, "User cannot be null");
        Set<String> userAuthorizations = getAuthorizations(user);
        Collections.addAll(userAuthorizations, additionalAuthorizations);
        return graph.createAuthorizations(userAuthorizations);
    }

    // Need to late bind since UserRepository injects AuthorizationRepository in constructor
    protected UserRepository getUserRepository() {
        if (userRepository == null) {
            userRepository = InjectHelper.getInstance(UserRepository.class);
        }
        return userRepository;
    }
}
