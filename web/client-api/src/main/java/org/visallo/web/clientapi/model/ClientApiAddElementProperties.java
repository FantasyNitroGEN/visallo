package org.visallo.web.clientapi.model;

import java.util.ArrayList;
import java.util.List;

public class ClientApiAddElementProperties implements ClientApiObject {
    public List<Property> properties = new ArrayList<>();

    public static class Property {
        public String propertyKey;
        public String propertyName;
        public String value;
        public String visibilitySource;
        public String metadataString;
    }
}
