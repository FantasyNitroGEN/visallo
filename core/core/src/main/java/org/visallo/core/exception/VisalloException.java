package org.visallo.core.exception;

public class VisalloException extends RuntimeException {
    private static final long serialVersionUID = -4322348262201847859L;

    public VisalloException(String message) {
        super(message);
    }

    public VisalloException(String message, Throwable cause) {
        super(message, cause);
    }
}
