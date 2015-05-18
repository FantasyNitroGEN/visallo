package org.visallo.core.exception;

import org.visallo.core.user.User;

public class VisalloAccessDeniedException extends VisalloException {
    private final User user;
    private final Object resourceId;

    public VisalloAccessDeniedException(String message, User user, Object resourceId) {
        super(message);
        this.user = user;
        this.resourceId = resourceId;
    }

    public User getUser() {
        return user;
    }

    public Object getResourceId() {
        return resourceId;
    }
}
