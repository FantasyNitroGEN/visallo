package org.visallo.core.model.properties.types;

import org.vertexium.Metadata;
import org.vertexium.Property;
import org.vertexium.Visibility;
import org.vertexium.mutation.ExistingElementMutation;

import java.util.Map;

public abstract class MetadataVisalloProperty<TRaw, TGraph> {
    private final String metadataKey;

    protected MetadataVisalloProperty(String metadataKey) {
        this.metadataKey = metadataKey;
    }

    /**
     * Convert the raw value to an appropriate value for storage
     * in Vertexium.
     */
    public abstract TGraph wrap(final TRaw value);

    /**
     * Convert the Vertexium value to its original raw type.
     */
    public abstract TRaw unwrap(final Object value);

    public String getMetadataKey() {
        return metadataKey;
    }

    public TRaw getMetadataValue(Metadata metadata) {
        return unwrap(metadata.getValue(getMetadataKey()));
    }

    public TRaw getMetadataValueOrDefault(Metadata metadata, TRaw defaultValue) {
        Object value = metadata.getValue(getMetadataKey());
        if (value == null) {
            return defaultValue;
        }
        return unwrap(value);
    }

    public TRaw getMetadataValue(Map<String, Object> metadata) {
        return unwrap(metadata.get(getMetadataKey()));
    }

    public TRaw getMetadataValue(Metadata metadata, TRaw defaultValue) {
        if (metadata.getEntry(getMetadataKey()) == null) {
            return defaultValue;
        }
        return unwrap(metadata.getValue(getMetadataKey()));
    }

    public void setMetadata(Metadata metadata, TRaw value, Visibility visibility) {
        metadata.add(getMetadataKey(), wrap(value), visibility);
    }

    public void setMetadata(PropertyMetadata metadata, TRaw value, Visibility visibility) {
        metadata.add(getMetadataKey(), wrap(value), visibility);
    }

    public void setMetadata(ExistingElementMutation m, Property property, TRaw value, Visibility visibility) {
        m.setPropertyMetadata(property, getMetadataKey(), wrap(value), visibility);
    }
}
