package org.visallo.core.model.properties.types;

import org.vertexium.property.StreamingPropertyValue;

/**
 * An IdentityVisalloProperty for StreamingPropertyValues.
 */
public class StreamingVisalloProperty extends IdentityVisalloProperty<StreamingPropertyValue> {
    /**
     * Create a new StreamingVisalloProperty.
     * @param key the property key
     */
    public StreamingVisalloProperty(String key) {
        super(key);
    }
}
