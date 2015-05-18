package org.visallo.web.clientapi.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientApiHistoricalPropertyValues implements ClientApiObject {
    public List<Value> values = new ArrayList<>();

    public static class Value implements ClientApiObject {
        public long timestamp;
        public Map<String, Object> metadata = new HashMap<>();
        public Object value;
    }
}
