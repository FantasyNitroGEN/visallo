package org.visallo.core.model.user;

import org.visallo.core.user.User;

public class UserNameAuthorizationContext extends AuthorizationContext {
    private final String username;

    public UserNameAuthorizationContext(User existingUser, String username) {
        super(existingUser);
        this.username = username;
    }

    public String getUsername() {
        return username;
    }
}
