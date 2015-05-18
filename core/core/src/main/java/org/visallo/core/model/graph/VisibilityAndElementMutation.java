package org.visallo.core.model.graph;

import org.visallo.core.security.VisalloVisibility;
import org.vertexium.Element;
import org.vertexium.mutation.ExistingElementMutation;

public class VisibilityAndElementMutation<T extends Element> {
    public final ExistingElementMutation<T> elementMutation;
    public final VisalloVisibility visibility;

    public VisibilityAndElementMutation(VisalloVisibility visibility, ExistingElementMutation<T> elementMutation) {
        this.visibility = visibility;
        this.elementMutation = elementMutation;
    }
}
