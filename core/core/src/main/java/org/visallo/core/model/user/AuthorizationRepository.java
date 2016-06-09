package org.visallo.core.model.user;

import org.visallo.core.user.User;

import java.util.Set;

public interface AuthorizationRepository {
    /**
     * Called by UserRepository when a user is authenticated possibly by a web authentication handler
     */
    void updateUser(User user, AuthorizationContext authorizationContext);

    Set<String> getAuthorizations(User user);

    org.vertexium.Authorizations getGraphAuthorizations(User user, String... additionalAuthorizations);
}
