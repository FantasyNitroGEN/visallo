package org.visallo.core.exception;

public class VisalloException extends RuntimeException {
    public VisalloException(String message) {
        super(message);
    }

    public VisalloException(String message, Throwable cause) {
        super(message, cause);
    }
}
