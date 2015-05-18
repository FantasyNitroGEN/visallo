package org.visallo.core.exception;

public class VisalloResourceNotFoundException extends VisalloException {
    private final Object resourceId;

    public VisalloResourceNotFoundException(String message) {
        super(message);
        this.resourceId = null;
    }

    public VisalloResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
        this.resourceId = null;
    }

    public VisalloResourceNotFoundException(String message, Object resourceId) {
        super(message);
        this.resourceId = resourceId;
    }


    public Object getResourceId() {
        return resourceId;
    }
}
