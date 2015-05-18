package org.visallo.web.clientapi.model;

import org.visallo.web.clientapi.util.ClientApiConverter;

import java.util.HashMap;
import java.util.Map;

public class ClientApiVerticesExistsResponse implements ClientApiObject {
    private Map<String, Boolean> exists = new HashMap<>();

    public Map<String, Boolean> getExists() {
        return exists;
    }

    @Override
    public String toString() {
        return ClientApiConverter.clientApiToString(this);
    }
}
