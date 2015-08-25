package org.visallo.web.clientapi.model;

import java.util.HashMap;
import java.util.Map;

public class ClientApiImportProperty {
    private String key;
    private String name;
    private Map<String, Object> metadata = new HashMap<>();
    private String value;
    private String visibilitySource;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getVisibilitySource() {
        return visibilitySource;
    }

    public void setVisibilitySource(String visibilitySource) {
        this.visibilitySource = visibilitySource;
    }
}
