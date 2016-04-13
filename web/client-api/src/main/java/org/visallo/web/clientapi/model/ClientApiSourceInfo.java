package org.visallo.web.clientapi.model;

import org.visallo.web.clientapi.util.ObjectMapperFactory;

import java.io.IOException;

public class ClientApiSourceInfo {
    public long startOffset;
    public long endOffset;
    public String vertexId;
    public String snippet;
    public String textPropertyKey;
    public String textPropertyName;

    public static ClientApiSourceInfo fromString(String sourceInfoString) {
        try {
            if (sourceInfoString == null || sourceInfoString.length() == 0) {
                return null;
            }
            return ObjectMapperFactory.getInstance().readValue(sourceInfoString, ClientApiSourceInfo.class);
        } catch (IOException e) {
            throw new RuntimeException("Could not read value: " + sourceInfoString, e);
        }
    }
}
