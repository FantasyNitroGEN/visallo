package org.visallo.web.clientapi;

public class VisalloClientApiException extends RuntimeException {
    public VisalloClientApiException(String message, Throwable cause) {
        super(message, cause);
    }

    public VisalloClientApiException(String message) {
        super(message);
    }
}
