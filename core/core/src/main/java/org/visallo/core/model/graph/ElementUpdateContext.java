package org.visallo.core.model.graph;

import org.vertexium.Authorizations;
import org.vertexium.Edge;
import org.vertexium.Element;
import org.vertexium.Visibility;
import org.vertexium.mutation.EdgeMutation;
import org.vertexium.mutation.ElementMutation;
import org.vertexium.mutation.ExistingElementMutation;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.properties.types.VisalloPropertyUpdate;
import org.visallo.core.security.VisibilityTranslator;
import org.visallo.core.user.User;
import org.visallo.web.clientapi.model.VisibilityJson;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ElementUpdateContext<T extends Element> {
    private final VisibilityTranslator visibilityTranslator;
    private final ElementMutation<T> mutation;
    private final User user;
    private final List<VisalloPropertyUpdate> properties = new ArrayList<>();
    private T element;

    public ElementUpdateContext(VisibilityTranslator visibilityTranslator, ElementMutation<T> mutation, User user) {
        this.visibilityTranslator = visibilityTranslator;
        this.mutation = mutation;
        this.user = user;
        if (mutation instanceof ExistingElementMutation) {
            element = ((ExistingElementMutation<T>) mutation).getElement();
        }
    }

    public T save(Authorizations authorizations) {
        this.element = this.mutation.save(authorizations);
        return this.element;
    }

    public ElementMutation<T> getMutation() {
        return mutation;
    }

    public List<VisalloPropertyUpdate> getProperties() {
        return properties;
    }

    public T getElement() {
        return element;
    }

    public void updateBuiltInProperties(
            Date modifiedDate,
            VisibilityJson visibilityJson
    ) {
        Visibility defaultVisibility = visibilityTranslator.getDefaultVisibility();
        VisalloProperties.MODIFIED_BY.updateProperty(this, user.getUserId(), defaultVisibility);
        VisalloProperties.MODIFIED_DATE.updateProperty(this, modifiedDate, defaultVisibility);
        VisalloProperties.VISIBILITY_JSON.updateProperty(this, visibilityJson, defaultVisibility);
    }

    public void setConceptType(String conceptType) {
        if (isEdgeMutation()) {
            throw new IllegalArgumentException("Cannot set concept type on edges");
        }

        Visibility defaultVisibility = visibilityTranslator.getDefaultVisibility();
        VisalloProperties.CONCEPT_TYPE.updateProperty(this, conceptType, defaultVisibility);
    }

    private boolean isEdgeMutation() {
        if (getMutation() instanceof EdgeMutation) {
            return true;
        }
        if (getMutation() instanceof ExistingElementMutation) {
            ExistingElementMutation m = (ExistingElementMutation) getMutation();
            if (m.getElement() instanceof Edge) {
                return true;
            }
        }
        return false;
    }
}
