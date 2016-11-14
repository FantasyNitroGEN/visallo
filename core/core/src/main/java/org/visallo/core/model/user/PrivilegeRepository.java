package org.visallo.core.model.user;

import org.visallo.core.user.User;
import org.visallo.web.clientapi.model.Privilege;

import java.util.Set;

public interface PrivilegeRepository {
    void updateUser(User user, AuthorizationContext authorizationContext);

    Set<String> getPrivileges(User user);

    boolean hasPrivilege(User user, String privilege);

    boolean hasAllPrivileges(User user, Set<String> requiredPrivileges);

    Set<Privilege> getAllPrivileges();

    static boolean hasPrivilege(Set<String> privileges, String privilege) {
        return privileges.contains(privilege);
    }
}
