package org.visallo.web;

import java.util.Collection;

public class BadRequestException extends RuntimeException {
    private final String parameterName;
    private final String message;
    private final Collection<String> invalidValues;

    public BadRequestException(String message) {
        this(null, message, null);
    }

    public BadRequestException(String parameterName, String message) {
        this(parameterName, message, null);
    }

    public BadRequestException(String parameterName, String message, Collection<String> invalidValues) {
        this.parameterName = parameterName;
        this.message = message;
        this.invalidValues = invalidValues;
    }

    public String getParameterName() {
        return parameterName;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public Collection<String> getInvalidValues() {
        return invalidValues;
    }

    @Override
    public String toString() {
        return "BadRequestException{" +
                "parameterName='" + parameterName + '\'' +
                ", message='" + message + '\'' +
                ", invalidValues=" + invalidValues +
                '}';
    }
}
