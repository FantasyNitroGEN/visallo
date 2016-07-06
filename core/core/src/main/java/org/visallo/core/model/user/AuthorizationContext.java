package org.visallo.core.model.user;

import org.visallo.core.user.User;

public abstract class AuthorizationContext {
    private final User existingUser;

    protected AuthorizationContext(User existingUser) {
        this.existingUser = existingUser;
    }

    public User getExistingUser() {
        return existingUser;
    }

    public boolean isNewUser() {
        return getExistingUser() == null;
    }
}
