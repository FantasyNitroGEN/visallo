package org.visallo.core.model.user;

import org.visallo.core.user.User;

public class AuthorizationContext {
    private final User existingUser;

    public AuthorizationContext(User existingUser) {
        this.existingUser = existingUser;
    }

    public User getExistingUser() {
        return existingUser;
    }

    public boolean isNewUser() {
        return getExistingUser() == null;
    }
}
