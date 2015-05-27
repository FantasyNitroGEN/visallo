package org.visallo.core.model.properties.types;

import org.vertexium.*;
import org.vertexium.mutation.ElementMutation;
import org.vertexium.mutation.ExistingElementMutation;

import java.util.Collections;
import java.util.List;

import static com.google.common.collect.Iterables.*;

public abstract class VisalloProperty<TRaw, TGraph> extends VisalloPropertyBase<TRaw, TGraph> {
    protected VisalloProperty(String propertyName) {
        super(propertyName);
    }

    public final void addPropertyValue(final ElementMutation<?> mutation, final String multiKey, final TRaw value, final Visibility visibility) {
        mutation.addPropertyValue(multiKey, getPropertyName(), wrap(value), visibility);
    }

    public final void addPropertyValue(final Element element, final String multiKey, final TRaw value, final Visibility visibility, Authorizations authorizations) {
        element.addPropertyValue(multiKey, getPropertyName(), wrap(value), visibility, authorizations);
    }

    public final void addPropertyValue(final Element element, final String multiKey, final TRaw value, final Metadata metadata, final Visibility visibility, Authorizations authorizations) {
        element.addPropertyValue(multiKey, getPropertyName(), wrap(value), metadata, visibility, authorizations);
    }

    public final void addPropertyValue(final ElementMutation<?> mutation,
                                       final String multiKey,
                                       final TRaw value,
                                       final Metadata metadata,
                                       final Visibility visibility) {
        mutation.addPropertyValue(multiKey, getPropertyName(), wrap(value), metadata, visibility);
    }

    public final TRaw getPropertyValue(final Element element, String propertyKey) {
        Object value = element != null ? element.getPropertyValue(propertyKey, getPropertyName()) : null;
        return value != null ? getRawConverter().apply(value) : null;
    }

    public final TRaw getPropertyValue(Property property) {
        Object value = property.getValue();
        return value != null ? getRawConverter().apply(value) : null;
    }

    @SuppressWarnings("unchecked")
    public final Iterable<TRaw> getPropertyValues(final Element element) {
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

    public Iterable<Property> getProperties(Element element) {
        return element.getProperties(getPropertyName());
    }

    public void removeProperty(Element element, String key, Authorizations authorizations) {
        element.softDeleteProperty(key, getPropertyName(), authorizations);
    }

    public void removeProperty(ElementMutation m, String key, final Visibility visibility) {
        m.softDeleteProperty(key, getPropertyName(), visibility);
    }

    public void alterVisibility(ExistingElementMutation<?> elementMutation, String propertyKey, Visibility newVisibility) {
        elementMutation.alterPropertyVisibility(propertyKey, getPropertyName(), newVisibility);
    }

    public void updateProperty(
            List<VisalloPropertyUpdate> changedProperties,
            Element element,
            ElementMutation m,
            String propertyKey,
            TRaw newValue,
            PropertyMetadata metadata,
            Visibility visibility
    ) {
        updateProperty(changedProperties, element, m, propertyKey, newValue, metadata.createMetadata(), visibility);
    }

    public void updateProperty(
            List<VisalloPropertyUpdate> changedProperties,
            Element element,
            ElementMutation m,
            String propertyKey,
            TRaw newValue,
            Metadata metadata,
            Visibility visibility
    ) {
        if (newValue == null) {
            return;
        }
        if (newValue instanceof String && ((String) newValue).length() == 0) {
            return;
        }
        Object currentValue = null;
        if (element != null) {
            currentValue = getPropertyValue(element, propertyKey);
        }
        if (currentValue == null || !newValue.equals(currentValue)) {
            addPropertyValue(m, propertyKey, newValue, metadata, visibility);
            changedProperties.add(new VisalloPropertyUpdate(this, propertyKey));
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
