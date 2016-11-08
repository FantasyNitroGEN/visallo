package org.visallo.core.model.properties.types;

import org.vertexium.*;
import org.vertexium.mutation.ElementMutation;
import org.vertexium.mutation.ExistingElementMutation;
import org.visallo.core.model.graph.ElementUpdateContext;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.clientapi.model.ClientApiElement;
import org.visallo.web.clientapi.model.ClientApiProperty;

import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class SingleValueVisalloProperty<TRaw, TGraph> extends VisalloPropertyBase<TRaw, TGraph> {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(SingleValueVisalloProperty.class);

    protected SingleValueVisalloProperty(String propertyName) {
        super(propertyName);
    }

    public final void setProperty(ElementMutation<?> mutation, TRaw value, Visibility visibility) {
        mutation.setProperty(getPropertyName(), wrap(value), visibility);
    }

    public final void setProperty(ElementMutation<?> mutation, TRaw value, Metadata metadata, Visibility visibility) {
        setProperty(mutation, value, metadata, null, visibility);
    }

    public final void setProperty(
            ElementMutation<?> mutation,
            TRaw value,
            Metadata metadata,
            Long timestamp,
            Visibility visibility
    ) {
        // Vertexium's ElementMutation doesn't have a setProperty that takes a timestamp. Calling addPropertyValue
        //  is effectively the same thing
        mutation.addPropertyValue(
                ElementMutation.DEFAULT_KEY,
                getPropertyName(),
                wrap(value),
                metadata,
                timestamp,
                visibility
        );
    }

    public final void setProperty(Element element, TRaw value, Visibility visibility, Authorizations authorizations) {
        element.setProperty(getPropertyName(), wrap(value), visibility, authorizations);
    }

    public final void setProperty(
            Element element,
            TRaw value,
            Metadata metadata,
            Visibility visibility,
            Authorizations authorizations
    ) {
        element.setProperty(getPropertyName(), wrap(value), metadata, visibility, authorizations);
    }

    public void setProperty(Map<String, Object> properties, Object value) {
        properties.put(getPropertyName(), value);
    }

    public final TRaw getPropertyValue(Element element) {
        Object value = element != null ? element.getPropertyValue(getPropertyName()) : null;
        return value != null ? getRawConverter().apply(value) : null;
    }

    public final TRaw getPropertyValueRequired(Element element) {
        checkNotNull(element, "Element cannot be null");
        Object value = element.getPropertyValue(getPropertyName());
        checkNotNull(value, "Property value of property " + getPropertyName() + " cannot be null");
        return getRawConverter().apply(value);
    }

    public final TRaw getPropertyValue(Map<String, Object> map) {
        Object value = map != null ? map.get(getPropertyName()) : null;
        return value != null ? getRawConverter().apply(value) : null;
    }

    public TRaw getPropertyValue(ClientApiElement clientApiElement) {
        return getPropertyValue(clientApiElement, null);
    }

    public TRaw getPropertyValue(ClientApiElement clientApiElement, TRaw defaultValue) {
        ClientApiProperty property = clientApiElement.getProperty(ElementMutation.DEFAULT_KEY, getPropertyName());
        if (property == null) {
            return defaultValue;
        }
        //noinspection unchecked
        return (TRaw) property.getValue();
    }

    public boolean hasProperty(Element element) {
        return element.getProperty(ElementMutation.DEFAULT_KEY, getPropertyName()) != null;
    }

    public Property getProperty(Element element) {
        return element.getProperty(getPropertyName());
    }

    public void removeProperty(Element element, Authorizations authorizations) {
        element.softDeleteProperty(ElementMutation.DEFAULT_KEY, getPropertyName(), authorizations);
    }

    public void removeProperty(ElementMutation m, Visibility visibility) {
        m.softDeleteProperty(getPropertyName(), visibility);
    }

    public void removeMetadata(Metadata metadata) {
        metadata.remove(getPropertyName());
    }

    public void removeMetadata(Metadata metadata, Visibility visibility) {
        metadata.remove(getPropertyName(), visibility);
    }

    public void alterVisibility(ExistingElementMutation<?> elementMutation, Visibility newVisibility) {
        elementMutation.alterPropertyVisibility(getPropertyName(), newVisibility);
    }

    public void removeProperty(
            List<VisalloPropertyUpdate> changedPropertiesOut,
            Element element,
            ElementMutation m,
            Visibility visibility
    ) {
        Object currentValue = getPropertyValue(element);
        if (currentValue != null) {
            long beforeDeletionTimestamp = System.currentTimeMillis() - 1;
            removeProperty(m, visibility);
            changedPropertiesOut.add(new VisalloPropertyUpdateRemove(this, beforeDeletionTimestamp, true, false));
        }
    }

    public <T extends Element> void updateProperty(
            ElementUpdateContext<T> ctx,
            TRaw newValue,
            Visibility visibility
    ) {
        updateProperty(ctx.getProperties(), ctx.getElement(), ctx.getMutation(), newValue, (Metadata) null, null, visibility);
    }

    /**
     * @param changedPropertiesOut Adds the property to this list if the property value changed
     * @deprecated Use {@link #updateProperty(List, Element, ElementMutation, Object, PropertyMetadata)}
     */
    @SuppressWarnings("deprecation")
    @Deprecated
    public void updateProperty(
            List<VisalloPropertyUpdate> changedPropertiesOut,
            Element element,
            ElementMutation m,
            TRaw newValue,
            PropertyMetadata metadata,
            Visibility visibility
    ) {
        updateProperty(changedPropertiesOut, element, m, newValue, metadata, null, visibility);
    }

    public void updateProperty(
            List<VisalloPropertyUpdate> changedPropertiesOut,
            Element element,
            ElementMutation m,
            TRaw newValue,
            PropertyMetadata metadata
    ) {
        checkNotNull(metadata, "metadata cannot be null");
        updateProperty(changedPropertiesOut, element, m, newValue, metadata.createMetadata(), null, metadata.getVisibility());
    }

    public <T extends Element> void updateProperty(
            ElementUpdateContext<T> ctx,
            TRaw newValue,
            PropertyMetadata metadata
    ) {
        updateProperty(ctx.getProperties(), ctx.getElement(), ctx.getMutation(), newValue, metadata.createMetadata(), null, metadata.getVisibility());
    }

    /**
     * @param changedPropertiesOut Adds the property to this list if the property value changed
     * @deprecated Use {@link #updateProperty(List, Element, ElementMutation, Object, PropertyMetadata, Long)}
     */
    @Deprecated
    public void updateProperty(
            List<VisalloPropertyUpdate> changedPropertiesOut,
            Element element,
            ElementMutation m,
            TRaw newValue,
            PropertyMetadata metadata,
            Long timestamp,
            Visibility visibility
    ) {
        updateProperty(changedPropertiesOut, element, m, newValue, metadata == null ? null : metadata.createMetadata(), timestamp, visibility);
    }

    public void updateProperty(
            List<VisalloPropertyUpdate> changedPropertiesOut,
            Element element,
            ElementMutation m,
            TRaw newValue,
            PropertyMetadata metadata,
            Long timestamp
    ) {
        checkNotNull(metadata, "metadata cannot be null");
        updateProperty(changedPropertiesOut, element, m, newValue, metadata.createMetadata(), timestamp, metadata.getVisibility());
    }

    public <T extends Element> void updateProperty(
            ElementUpdateContext<T> ctx,
            TRaw newValue,
            PropertyMetadata metadata,
            Long timestamp
    ) {
        updateProperty(ctx.getProperties(), ctx.getElement(), ctx.getMutation(), newValue, metadata.createMetadata(), timestamp, metadata.getVisibility());
    }

    /**
     * @param changedPropertiesOut Adds the property to this list if the property value changed
     */
    public void updateProperty(
            List<VisalloPropertyUpdate> changedPropertiesOut,
            Element element,
            ElementMutation m,
            TRaw newValue,
            Metadata metadata,
            Visibility visibility
    ) {
        updateProperty(changedPropertiesOut, element, m, newValue, metadata, null, visibility);
    }

    public <T extends Element> void updateProperty(
            ElementUpdateContext<T> ctx,
            TRaw newValue,
            Metadata metadata,
            Visibility visibility
    ) {
        updateProperty(ctx.getProperties(), ctx.getElement(), ctx.getMutation(), newValue, metadata, null, visibility);
    }

    /**
     * @param changedPropertiesOut Adds the property to this list if the property value changed
     */
    public void updateProperty(
            List<VisalloPropertyUpdate> changedPropertiesOut,
            Element element,
            ElementMutation m,
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
        TRaw currentValue = null;
        if (element != null) {
            currentValue = getPropertyValue(element);
        }
        if (currentValue == null || !isEquals(newValue, currentValue)) {
            setProperty(m, newValue, metadata, timestamp, visibility);
            changedPropertiesOut.add(new VisalloPropertyUpdate(this));
        }
    }

    public <T extends Element> void updateProperty(
            ElementUpdateContext<T> ctx,
            TRaw newValue,
            Metadata metadata,
            Long timestamp,
            Visibility visibility
    ) {
        updateProperty(ctx.getProperties(), ctx.getElement(), ctx.getMutation(), newValue, metadata, timestamp, visibility);
    }
}
