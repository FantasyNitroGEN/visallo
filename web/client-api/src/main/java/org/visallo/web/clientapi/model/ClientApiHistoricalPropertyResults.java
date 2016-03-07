package org.visallo.web.clientapi.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientApiHistoricalPropertyResults implements ClientApiObject {
    public List<Event> events = new ArrayList<Event>();

    public static class Event implements ClientApiObject {
        public long timestamp;
        public Map<String, Object> metadata = new HashMap<String, Object>();
        public Object value;
        public String propertyKey;
        public String propertyName;
        public String propertyVisibility;
    }
}
