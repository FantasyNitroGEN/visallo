package org.visallo.tools.ontology.ingest.common;

import com.google.common.base.Strings;
import org.vertexium.Visibility;
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
        this(id, null);
    }

    public EntityBuilder(String id, String visibility) {
        assert id != null;
        assert id.trim().length() > 0;

        this.id = id;
        this.visibility = visibility;
    }

    public String getId() {
        return id;
    }

    public abstract String getIri();

    public Set<PropertyAddition<?>> getPropertyAdditions() {
        return propertyAdditions;
    }

    /**
     * @deprecated replaced by {@link #setMetadata(Map<String, Object>)} to get rid of confusion of casting return value back to subclass
     */
    @Deprecated
    public EntityBuilder withMetadata(Map<String, Object> metdata) {
        this.setMetadata(metdata);
        return this;
    }

    /**
     * @deprecated replaced by {@link #setTimestamp(Long)} to get rid of confusion of casting return value back to subclass
     */
    @Deprecated
    public EntityBuilder withTimestamp(Long timestamp) {
        this.setTimestamp(timestamp);
        return this;
    }

    /**
     * @deprecated replaced by {@link #setVisibility(String)}  to get rid of confusion of casting return value back to subclass
     */
    @Deprecated
    public EntityBuilder withVisibility(String visibility) {
        this.setVisibility(visibility);
        return this;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
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
        return addStringProperty(iri, key, value, null);
    }

    protected PropertyAddition<String> addStringProperty(String iri, String key, Object value, String visibility) {
        String strValue = null;
        if (value != null) {
            strValue = value.toString();
            strValue = Strings.isNullOrEmpty(strValue) ? null : strValue;
        }
        return addTo(iri, key, strValue, visibility);
    }

    protected PropertyAddition<Date> addDateProperty(String iri, String key, Object value, SimpleDateFormat dateFormat) {
        return addDateProperty(iri, key, value, dateFormat, null);
    }

    protected PropertyAddition<Date> addDateProperty(String iri, String key, Object value, SimpleDateFormat dateFormat, String visibility) {
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
        return addTo(iri, key, dateValue, visibility);
    }

    protected PropertyAddition<byte[]> addByteArrayProperty(String iri, String key, Object value) {
        return addByteArrayProperty(iri, key, value, null);
    }

    protected PropertyAddition<byte[]> addByteArrayProperty(String iri, String key, Object value, String visibility) {
        byte[] byteArrayValue = null;
        if (value != null) {
            if (value instanceof byte[]) {
                byteArrayValue = (byte[]) value;
            } else {
                throw new VisalloException("Unable to assign value " + value + " as byte[]");
            }
        }
        return addTo(iri, key, byteArrayValue, visibility);
    }

    protected PropertyAddition<Boolean> addBooleanProperty(String iri, String key, Object value) {
        return addBooleanProperty(iri, key, value, null);
    }

    protected PropertyAddition<Boolean> addBooleanProperty(String iri, String key, Object value, String visibility) {
        Boolean booleanValue = null;
        if (value != null) {
            if (value instanceof Boolean) {
                booleanValue = (Boolean) value;
            } else {
                booleanValue = Boolean.valueOf(value.toString().trim());
            }
        }
        return addTo(iri, key, booleanValue, visibility);
    }

    protected PropertyAddition<Double> addDoubleProperty(String iri, String key, Object value) {
        return addDoubleProperty(iri, key, value, null);
    }

    protected PropertyAddition<Double> addDoubleProperty(String iri, String key, Object value, String visibility) {
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
        return addTo(iri, key, doubleValue, visibility);
    }

    protected PropertyAddition<Integer> addIntegerProperty(String iri, String key, Object value) {
        return addIntegerProperty(iri, key, value, null);
    }

    protected PropertyAddition<Integer> addIntegerProperty(String iri, String key, Object value, String visibility) {
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
        return addTo(iri, key, intValue, visibility);
    }

    protected PropertyAddition<Long> addLongProperty(String iri, String key, Object value) {
        return addLongProperty(iri, key, value, null);
    }

    protected PropertyAddition<Long> addLongProperty(String iri, String key, Object value, String visibility) {
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
        return addTo(iri, key, longValue, visibility);
    }

    protected PropertyAddition<GeoPoint> addGeoPointProperty(String iri, String key, Object value) {
        return addGeoPointProperty(iri, key, value, null);
    }

    protected PropertyAddition<GeoPoint> addGeoPointProperty(String iri, String key, Object value, String visibility) {
        GeoPoint geoValue = null;
        if (value != null) {
            geoValue = (GeoPoint) value;
        }
        return addTo(iri, key, geoValue, visibility);
    }

    private <T> PropertyAddition<T> addTo(String iri, String key, T value, String visibility) {
        PropertyAddition<T> addition = new PropertyAddition<>(iri, key, value).withVisibility(visibility);
        propertyAdditions.add(addition);
        return addition;
    }
}
