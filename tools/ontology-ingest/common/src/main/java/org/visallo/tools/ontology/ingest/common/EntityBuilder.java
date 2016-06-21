package org.visallo.tools.ontology.ingest.common;

import com.google.common.base.Strings;
import org.vertexium.type.GeoPoint;
import org.visallo.core.exception.VisalloException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class EntityBuilder {
    private String id;
    private Set<PropertyAddition<?>> propertyAdditions = new HashSet<>();
    private Map<String, Object> metadata;
    private Long timestamp;
    private String visibility;

    public EntityBuilder(String id) {
        assert id != null;
        assert id.trim().length() > 0;

        this.id = id;
    }

    public String getId() {
        return id;
    }

    public abstract String getIri();

    public Set<PropertyAddition<?>> getPropertyAdditions() {
        return propertyAdditions;
    }

    public EntityBuilder withMetadata(Map<String, Object> metdata) {
        this.metadata = metdata;
        return this;
    }

    public EntityBuilder withTimestamp(Long timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public EntityBuilder withVisibility(String visibility) {
        this.visibility = visibility;
        return this;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public String getVisibility() {
        return visibility;
    }

    protected PropertyAddition<String> addStringProperty(String iri, String key, Object value) {
        String strValue = null;
        if (value != null) {
            strValue = value.toString();
            strValue = Strings.isNullOrEmpty(strValue) ? null : strValue;
        }
        return addTo(iri, key, strValue);
    }

    protected PropertyAddition<Date> addDateProperty(String iri, String key, Object value, SimpleDateFormat dateFormat) {
        Date dateValue = null;
        if (value != null) {
            if (value instanceof Date) {
                dateValue = (Date) value;
            } else {
                String strValue = value.toString();
                try {
                    dateValue = Strings.isNullOrEmpty(strValue) ? null : dateFormat.parse(strValue.trim());
                } catch (ParseException pe) {
                    throw new VisalloException("Unable to parse date: " + strValue, pe);
                }
            }
        }
        return addTo(iri, key, dateValue);
    }

    protected PropertyAddition<byte[]> addByteArrayProperty(String iri, String key, Object value) {
        byte[] byteArrayValue = null;
        if (value != null) {
            if (value instanceof byte[]) {
                byteArrayValue = (byte[]) value;
            } else {
                throw new VisalloException("Unable to assign value " + value + " as byte[]");
            }
        }
        return addTo(iri, key, byteArrayValue);
    }

    protected PropertyAddition<Boolean> addBooleanProperty(String iri, String key, Object value) {
        Boolean booleanValue = null;
        if (value != null) {
            if (value instanceof Boolean) {
                booleanValue = (Boolean) value;
            } else {
                booleanValue = Boolean.valueOf(value.toString().trim());
            }
        }
        return addTo(iri, key, booleanValue);
    }

    protected PropertyAddition<Double> addDoubleProperty(String iri, String key, Object value) {
        Double doubleValue = null;
        if (value != null) {
            if (value instanceof String) {
                String strValue = (String) value;
                if (!Strings.isNullOrEmpty(strValue)) {
                    doubleValue = Double.valueOf(strValue.trim());
                }
            } else if (value instanceof Integer) {
                doubleValue = ((Integer) value).doubleValue();
            } else {
                doubleValue = (Double) value;
            }
        }
        return addTo(iri, key, doubleValue);
    }

    protected PropertyAddition<Integer> addIntegerProperty(String iri, String key, Object value) {
        Integer intValue = null;
        if (value != null) {
            if (value instanceof String) {
                String strValue = (String) value;
                if (!Strings.isNullOrEmpty(strValue)) {
                    intValue = Integer.valueOf(strValue.trim());
                }
            } else if (value instanceof Double) {
                intValue = ((Double) value).intValue();
            } else {
                intValue = (Integer) value;
            }
        }
        return addTo(iri, key, intValue);
    }

    protected PropertyAddition<Long> addLongProperty(String iri, String key, Object value) {
        Long longValue = null;
        if (value != null) {
            if (value instanceof String) {
                String strValue = (String) value;
                if (!Strings.isNullOrEmpty(strValue)) {
                    longValue = Long.valueOf(strValue.trim());
                }
            } else if (value instanceof Integer) {
                longValue = ((Integer) value).longValue();
            } else if (value instanceof Double) {
                longValue = ((Double) value).longValue();
            } else {
                longValue = (Long) value;
            }
        }
        return addTo(iri, key, longValue);
    }

    protected PropertyAddition<GeoPoint> addGeoPointProperty(String iri, String key, Object value) {
        GeoPoint geoValue = null;
        if (value != null) {
            geoValue = (GeoPoint) value;
        }
        return addTo(iri, key, geoValue);
    }

    private <T> PropertyAddition<T> addTo(String iri, String key, T value) {
        PropertyAddition<T> addition = new PropertyAddition<>(iri, key, value);
        propertyAdditions.add(addition);
        return addition;
    }
}
