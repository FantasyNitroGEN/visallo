package org.visallo.core.model.user;

import org.visallo.core.user.User;
import org.visallo.web.clientapi.model.Privilege;

import java.util.Set;

public class DefaultUserListener implements UserListener {
    @Override
    public void newUserAdded(User user) {

    }

    @Override
    public void userDeleted(User user) {

    }

    @Override
    public void userPrivilegesUpdated(User user, Set<Privilege> privileges) {

    }

    @Override
    public void userRemoveAuthorization(User user, String auth) {

    }

    @Override
    public void userAddAuthorization(User user, String auth) {

    }
}
