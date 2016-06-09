package org.visallo.core.model.user;

import org.visallo.core.user.User;
import org.visallo.web.clientapi.model.UserStatus;

import java.util.Set;

public interface UserListener {
    void newUserAdded(User user);

    void userDeleted(User user);

    void userPrivilegesUpdated(User user, Set<String> privileges);

    void userRemoveAuthorization(User user, String auth);

    void userAddAuthorization(User user, String auth);

    void userLogin(User user, AuthorizationContext authorizationContext);

    void userStatusChange(User user, UserStatus status);
}
