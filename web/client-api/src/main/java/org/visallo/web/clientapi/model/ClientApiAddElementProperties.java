package org.visallo.web.clientapi.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ClientApiAddElementProperties implements ClientApiObject {
    public List<Property> properties = new ArrayList<>();

    public ClientApiAddElementProperties() {

    }

    public ClientApiAddElementProperties(Property... properties) {
        Collections.addAll(this.properties, properties);
    }

    public ClientApiAddElementProperties add(Property property) {
        this.properties.add(property);
        return this;
    }

    public static class Property {
        public String propertyKey;
        public String propertyName;
        public String value;
        public String visibilitySource;
        public String metadataString;

        public Property() {

        }

        public Property(String propertyKey, String propertyName, String value, String visibilitySource, String metadataString) {
            this.propertyKey = propertyKey;
            this.propertyName = propertyName;
            this.value = value;
            this.visibilitySource = visibilitySource;
            this.metadataString = metadataString;
        }

        public Property setPropertyKey(String propertyKey) {
            this.propertyKey = propertyKey;
            return this;
        }

        public Property setPropertyName(String propertyName) {
            this.propertyName = propertyName;
            return this;
        }

        public Property setValue(String value) {
            this.value = value;
            return this;
        }

        public Property setVisibilitySource(String visibilitySource) {
            this.visibilitySource = visibilitySource;
            return this;
        }

        public Property setMetadataString(String metadataString) {
            this.metadataString = metadataString;
            return this;
        }
    }
}
