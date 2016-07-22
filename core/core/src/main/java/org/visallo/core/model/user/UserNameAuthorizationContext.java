package org.visallo.core.model.user;

import org.visallo.core.user.User;

public class UserNameAuthorizationContext extends AuthorizationContext {
    private final String username;

    public UserNameAuthorizationContext(String username, String remoteAddr) {
        super(remoteAddr);
        this.username = username;
    }

    public String getUsername() {
        return username;
    }
}
