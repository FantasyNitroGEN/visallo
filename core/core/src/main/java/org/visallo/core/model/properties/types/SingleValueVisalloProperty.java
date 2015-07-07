package org.visallo.core.model.properties.types;

import org.vertexium.*;
import org.vertexium.mutation.ElementMutation;
import org.vertexium.mutation.ExistingElementMutation;

import java.util.List;

public abstract class SingleValueVisalloProperty<TRaw, TGraph> extends VisalloPropertyBase<TRaw, TGraph> {
    protected SingleValueVisalloProperty(String propertyName) {
        super(propertyName);
    }

    public final void setProperty(final ElementMutation<?> mutation, final TRaw value, final Visibility visibility) {
        mutation.setProperty(getPropertyName(), wrap(value), visibility);
    }

    public final void setProperty(final ElementMutation<?> mutation, final TRaw value, final Metadata metadata, final Visibility visibility) {
        mutation.setProperty(getPropertyName(), wrap(value), metadata, visibility);
    }

    public final void setProperty(final Element element, final TRaw value, final Visibility visibility, Authorizations authorizations) {
        element.setProperty(getPropertyName(), wrap(value), visibility, authorizations);
    }

    public final void setProperty(final Element element, final TRaw value, final Metadata metadata, final Visibility visibility, Authorizations authorizations) {
        element.setProperty(getPropertyName(), wrap(value), metadata, visibility, authorizations);
    }

    public final TRaw getPropertyValue(final Element element) {
        Object value = element != null ? element.getPropertyValue(getPropertyName()) : null;
        return value != null ? getRawConverter().apply(value) : null;
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

    public void removeProperty(ElementMutation m, final Visibility visibility) {
        m.softDeleteProperty(getPropertyName(), visibility);
    }

    public void removeMetadata(Metadata metadata) {
        metadata.remove(getPropertyName());
    }

    public void removeMetadata(Metadata metadata, final Visibility visibility) {
        metadata.remove(getPropertyName(), visibility);
    }

    public void alterVisibility(ExistingElementMutation<?> elementMutation, Visibility newVisibility) {
        elementMutation.alterPropertyVisibility(getPropertyName(), newVisibility);
    }

    /**
     * @param changedPropertiesOut Adds the property to this list if the property value changed
     */
    public void updateProperty(
            List<VisalloPropertyUpdate> changedPropertiesOut,
            Element element,
            ElementMutation m,
            TRaw newValue,
            PropertyMetadata metadata,
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
            currentValue = getPropertyValue(element);
        }
        if (currentValue == null || !newValue.equals(currentValue)) {
            setProperty(m, newValue, metadata.createMetadata(), visibility);
            changedPropertiesOut.add(new VisalloPropertyUpdate(this));
        }
    }
}
