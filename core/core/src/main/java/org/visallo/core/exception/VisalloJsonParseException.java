package org.visallo.core.exception;

import org.json.JSONException;

public class VisalloJsonParseException extends RuntimeException {
    public VisalloJsonParseException(String jsonString, JSONException cause) {
        super("Could not parse json string: " + jsonString, cause);
    }
}
