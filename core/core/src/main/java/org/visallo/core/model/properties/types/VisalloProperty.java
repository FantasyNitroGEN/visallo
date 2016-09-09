package org.visallo.core.model.properties.types;

import org.vertexium.*;
import org.vertexium.mutation.ElementMutation;
import org.vertexium.mutation.ExistingElementMutation;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

import java.util.Collections;
import java.util.List;

import static com.google.common.collect.Iterables.*;

public abstract class VisalloProperty<TRaw, TGraph> extends VisalloPropertyBase<TRaw, TGraph> {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(VisalloProperty.class);

    protected VisalloProperty(String propertyName) {
        super(propertyName);
    }

    public final void addPropertyValue(ElementMutation<?> mutation, String multiKey, TRaw value, Visibility visibility) {
        mutation.addPropertyValue(multiKey, getPropertyName(), wrap(value), visibility);
    }

    public final void addPropertyValue(
            Element element,
            String multiKey,
            TRaw value,
            Visibility visibility,
            Authorizations authorizations
    ) {
        element.addPropertyValue(multiKey, getPropertyName(), wrap(value), visibility, authorizations);
    }

    public final void addPropertyValue(
            Element element,
            String multiKey,
            TRaw value,
            Metadata metadata,
            Visibility visibility,
            Authorizations authorizations
    ) {
        element.addPropertyValue(multiKey, getPropertyName(), wrap(value), metadata, visibility, authorizations);
    }

    public final void addPropertyValue(
            ElementMutation<?> mutation,
            String multiKey,
            TRaw value,
            Metadata metadata,
            Visibility visibility
    ) {
        addPropertyValue(mutation, multiKey, value, metadata, null, visibility);
    }

    public final void addPropertyValue(
            ElementMutation<?> mutation,
            String multiKey,
            TRaw value,
            Metadata metadata,
            Long timestamp,
            Visibility visibility
    ) {
        mutation.addPropertyValue(multiKey, getPropertyName(), wrap(value), metadata, timestamp, visibility);
    }

    public final TRaw getPropertyValue(Element element, String propertyKey) {
        Object value = element != null ? element.getPropertyValue(propertyKey, getPropertyName()) : null;
        return value != null ? getRawConverter().apply(value) : null;
    }

    public final TRaw getPropertyValue(Property property) {
        Object value = property.getValue();
        return value != null ? getRawConverter().apply(value) : null;
    }

    @SuppressWarnings("unchecked")
    public final Iterable<TRaw> getPropertyValues(Element element) {
        Iterable<Object> values = element != null ? element.getPropertyValues(getPropertyName()) : null;
        return values != null ? transform(values, getRawConverter()) : Collections.EMPTY_LIST;
    }

    public boolean hasProperty(Element element, String propertyKey) {
        return element.getProperty(propertyKey, getPropertyName()) != null;
    }

    public boolean hasProperty(Element element) {
        return size(element.getProperties(getPropertyName())) > 0;
    }

    public Property getProperty(Element element, String key) {
        return element.getProperty(key, getPropertyName());
    }

    public Property getOnlyProperty(Element element) {
        return getOnlyElement(element.getProperties(getPropertyName()), null);
    }

    public Property getFirstProperty(Element element) {
        return getFirst(element.getProperties(getPropertyName()), null);
    }

    public TRaw getFirstPropertyValue(Element element) {
        Property property = getFirstProperty(element);
        if (property == null) {
            return null;
        }
        return getPropertyValue(property);
    }

    public Iterable<Property> getProperties(Element element) {
        return element.getProperties(getPropertyName());
    }

    public void removeProperty(Element element, String key, Authorizations authorizations) {
        element.softDeleteProperty(key, getPropertyName(), authorizations);
    }

    public void removeProperty(ElementMutation m, String key, Visibility visibility) {
        m.softDeleteProperty(key, getPropertyName(), visibility);
    }

    public void alterVisibility(ExistingElementMutation<?> elementMutation, String propertyKey, Visibility newVisibility) {
        elementMutation.alterPropertyVisibility(propertyKey, getPropertyName(), newVisibility);
    }

    public void removeProperty(
            List<VisalloPropertyUpdate> changedPropertiesOut,
            Element element,
            ElementMutation m,
            String propertyKey,
            Visibility visibility
    ) {
        Object currentValue = getPropertyValue(element, propertyKey);
        if (currentValue != null) {
            long beforeDeletionTimestamp = System.currentTimeMillis() - 1;
            removeProperty(m, propertyKey, visibility);
            changedPropertiesOut.add(new VisalloPropertyUpdateRemove(this, propertyKey, beforeDeletionTimestamp, true, false));
        }
    }

    /**
     * @param changedPropertiesOut Adds the property to this list if the property value changed
     */
    public void updateProperty(
            List<VisalloPropertyUpdate> changedPropertiesOut,
            Element element,
            ElementMutation m,
            String propertyKey,
            TRaw newValue,
            PropertyMetadata metadata,
            Visibility visibility
    ) {
        updateProperty(changedPropertiesOut, element, m, propertyKey, newValue, metadata, null, visibility);
    }

    /**
     * @param changedPropertiesOut Adds the property to this list if the property value changed
     */
    public void updateProperty(
            List<VisalloPropertyUpdate> changedPropertiesOut,
            Element element,
            ElementMutation m,
            String propertyKey,
            TRaw newValue,
            PropertyMetadata metadata,
            Long timestamp,
            Visibility visibility
    ) {
        updateProperty(changedPropertiesOut, element, m, propertyKey, newValue, metadata.createMetadata(), timestamp, visibility);
    }

    /**
     * @param changedPropertiesOut Adds the property to this list if the property value changed
     */
    public void updateProperty(
            List<VisalloPropertyUpdate> changedPropertiesOut,
            Element element,
            ElementMutation m,
            String propertyKey,
            TRaw newValue,
            Metadata metadata,
            Visibility visibility
    ) {
        updateProperty(changedPropertiesOut, element, m, propertyKey, newValue, metadata, null, visibility);
    }

    /**
     * @param changedPropertiesOut Adds the property to this list if the property value changed
     */
    public void updateProperty(
            List<VisalloPropertyUpdate> changedPropertiesOut,
            Element element,
            ElementMutation m,
            String propertyKey,
            TRaw newValue,
            Metadata metadata,
            Long timestamp,
            Visibility visibility
    ) {
        if (newValue == null) {
            LOGGER.error("passing a null value to updateProperty will not be allowed in the future: %s", this);
            return;
        }
        if (newValue instanceof String && ((String) newValue).length() == 0) {
            LOGGER.error("passing an empty string value to updateProperty will not be allowed in the future: %s", this);
            return;
        }
        Object currentValue = null;
        if (element != null) {
            currentValue = getPropertyValue(element, propertyKey);
        }
        if (currentValue == null || !newValue.equals(currentValue)) {
            addPropertyValue(m, propertyKey, newValue, metadata, timestamp, visibility);
            changedPropertiesOut.add(new VisalloPropertyUpdate(this, propertyKey));
        }
    }

    public TRaw getOnlyPropertyValue(Element element) {
        Object value = getOnlyElement(element.getPropertyValues(getPropertyName()), null);
        if (value != null) {
            return unwrap(value);
        }
        return null;
    }
}
