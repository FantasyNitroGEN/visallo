package org.visallo.core.model.user;

import org.visallo.core.user.User;

import java.util.Set;

public interface UpdatablePrivilegeRepository extends PrivilegeRepository {
    void setRoles(User user, Set<String> roles, User authUser);
}
