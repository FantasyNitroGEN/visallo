package org.visallo.core.model.graph;

import org.vertexium.Authorizations;
import org.vertexium.Element;
import org.vertexium.Graph;
import org.vertexium.mutation.ElementMutation;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.security.VisibilityTranslator;
import org.visallo.core.user.User;
import org.visallo.web.clientapi.model.VisibilityJson;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Helper class to create or update graph Elements.
 * <p>
 * Example
 * <pre>
 * {@code
 * try (GraphUpdateContext ctx = graphRepository.beginGraphUpdate(Priority.NORMAL, user, authorizations)) {
 *   ElementMutation<Vertex> m = graph.prepareVertex("v1", visibility);
 *   ctx.update(m, updateContext -> {
 *     VisalloProperties.FILE_NAME.updateProperty(updateContext, "key", fileName, metadata, visibility);
 *   });
 * }
 * }
 * </pre>
 */
public class GraphUpdateContext implements AutoCloseable {
    private final Graph graph;
    private final WorkQueueRepository workQueueRepository;
    private final VisibilityTranslator visibilityTranslator;
    private final Priority priority;
    private final User user;
    private final Authorizations authorizations;
    private final List<ElementUpdateContext> elementUpdateContexts = new ArrayList<>();

    public GraphUpdateContext(
            Graph graph,
            WorkQueueRepository workQueueRepository,
            VisibilityTranslator visibilityTranslator,
            Priority priority,
            User user,
            Authorizations authorizations
    ) {
        this.graph = graph;
        this.workQueueRepository = workQueueRepository;
        this.visibilityTranslator = visibilityTranslator;
        this.priority = priority;
        this.user = user;
        this.authorizations = authorizations;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void close() throws Exception {
        graph.flush();
        for (ElementUpdateContext ctx : elementUpdateContexts) {
            workQueueRepository.pushGraphVisalloPropertyQueue(ctx.getElement(), ctx.getProperties(), priority);
        }
    }

    /**
     * Similar to {@link GraphUpdateContext#update(ElementMutation, Update)} but
     * prepares the mutation from the element.
     */
    public <T extends Element> void update(T element, Update<T> updateFn) {
        Date modifiedDate = null;
        VisibilityJson visibilityJson = null;
        String conceptType = null;
        update(element, modifiedDate, visibilityJson, conceptType, updateFn);
    }

    /**
     * Similar to {@link GraphUpdateContext#update(Element, Update)} but calls
     * {@link ElementUpdateContext#updateBuiltInProperties(Date, VisibilityJson)} before calling
     * updateFn.
     */
    public <T extends Element> void update(
            T element,
            Date modifiedDate,
            VisibilityJson visibilityJson,
            Update<T> updateFn
    ) {
        String conceptType = null;
        update(element, modifiedDate, visibilityJson, conceptType, updateFn);
    }

    /**
     * Similar to {@link GraphUpdateContext#update(Element, Update)} but calls
     * {@link ElementUpdateContext#updateBuiltInProperties(Date, VisibilityJson)} and
     * {@link ElementUpdateContext#setConceptType(String)} before calling
     * updateFn.
     */
    public <T extends Element> void update(
            T element,
            Date modifiedDate,
            VisibilityJson visibilityJson,
            String conceptType,
            Update<T> updateFn
    ) {
        checkNotNull(element, "element cannot be null");
        update(element.prepareMutation(), modifiedDate, visibilityJson, conceptType, updateFn);
    }

    public <T extends Element> void update(ElementMutation<T> m, Update<T> updateFn) {
        Date modifiedDate = null;
        VisibilityJson visibilityJson = null;
        String conceptType = null;
        update(m, modifiedDate, visibilityJson, conceptType, updateFn);
    }

    /**
     * Similar to {@link GraphUpdateContext#update(ElementMutation, Update)} but calls
     * {@link ElementUpdateContext#updateBuiltInProperties(Date, VisibilityJson)} before calling
     * updateFn.
     */
    public <T extends Element> void update(
            ElementMutation<T> m,
            Date modifiedDate,
            VisibilityJson visibilityJson,
            Update<T> updateFn
    ) {
        String conceptType = null;
        update(m, modifiedDate, visibilityJson, conceptType, updateFn);
    }

    /**
     * Similar to {@link GraphUpdateContext#update(ElementMutation, Update)} but calls
     * {@link ElementUpdateContext#updateBuiltInProperties(Date, VisibilityJson)} and
     * {@link ElementUpdateContext#setConceptType(String)} before calling
     * updateFn.
     */
    public <T extends Element> void update(
            ElementMutation<T> m,
            Date modifiedDate,
            VisibilityJson visibilityJson,
            String conceptType,
            Update<T> updateFn
    ) {
        checkNotNull(m, "element cannot be null");
        checkNotNull(updateFn, "updateFn cannot be null");

        ElementUpdateContext<T> elementUpdateContext = new ElementUpdateContext<T>(visibilityTranslator, m, user);
        if (modifiedDate != null || visibilityJson != null) {
            elementUpdateContext.updateBuiltInProperties(modifiedDate, visibilityJson);
        }
        if (conceptType != null) {
            elementUpdateContext.setConceptType(conceptType);
        }
        updateFn.update(elementUpdateContext);
        elementUpdateContext.save(authorizations);
        elementUpdateContexts.add(elementUpdateContext);
    }

    public interface Update<T extends Element> {
        void update(ElementUpdateContext<T> ctx);
    }
}
