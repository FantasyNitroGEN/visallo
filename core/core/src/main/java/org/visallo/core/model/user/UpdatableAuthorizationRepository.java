package org.visallo.core.model.user;

import org.visallo.core.user.User;

import java.util.Set;

public interface UpdatableAuthorizationRepository extends AuthorizationRepository {
    void addAuthorization(User user, String auth, User authUser);

    void removeAuthorization(User user, String auth, User authUser);

    void setAuthorizations(User user, Set<String> newAuthorizations, User authUser);
}
