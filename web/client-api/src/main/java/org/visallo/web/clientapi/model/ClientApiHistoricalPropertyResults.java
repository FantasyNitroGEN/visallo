package org.visallo.web.clientapi.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * Class contains a collection of events derived from the historicalPropertyResults.
 * Events are calculated by looking at variations between two consecutive
 * historicalPropertyResults. Valid events are defined by EventType.
 *
 */
public class ClientApiHistoricalPropertyResults implements ClientApiObject {

    public List<Event> events = new ArrayList<Event>();

    public enum EventType {
        PROPERTY_ADDED("Added"),
        PROPERTY_MODIFIED("Modified"),
        PROPERTY_DELETED("Deleted");

        private final String name;
        private EventType(String name) { this.name = name; }
        @Override
        public String toString() { return this.name; }
    }

    public static class Event implements ClientApiObject, Comparable<Event> {
        private EventType eventType;
        public long timestamp;
        public String propertyKey;
        public String propertyName;
        public String modifiedBy;

        public Map<String, String> fields;      // Current state
        public Map<String, String> changed;    // Changes from previous state

        @Override
        public int compareTo(Event o) {
            return (int)(timestamp - o.timestamp);
        }

        public void setEventType(EventType eventType) {
            this.eventType = eventType;
        }

        public String getEventType() {
            return eventType.toString();
        }
    }
}
