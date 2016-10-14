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

import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;

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
    private static final int DEFAULT_MAX_ELEMENT_UPDATE_CONTEXT_ITEMS = 1000;
    private final Graph graph;
    private final WorkQueueRepository workQueueRepository;
    private final VisibilityTranslator visibilityTranslator;
    private final Priority priority;
    private final User user;
    private final Authorizations authorizations;
    private final Queue<ElementUpdateContext<? extends Element>> elementUpdateContexts = new LinkedList<>();
    private int maxElementUpdateContextItems = DEFAULT_MAX_ELEMENT_UPDATE_CONTEXT_ITEMS;
    private boolean pushOnQueue = true;

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
        pushToWorkQueueRepository();
    }

    protected void pushToWorkQueueRepository() {
        graph.flush();
        if (isPushOnQueue()) {
            while (elementUpdateContexts.size() > 0) {
                ElementUpdateContext<? extends Element> elemCtx = elementUpdateContexts.remove();
                workQueueRepository.pushGraphVisalloPropertyQueue(elemCtx.getElement(), elemCtx.getProperties(), priority);
            }
        } else {
            elementUpdateContexts.clear();
        }
    }

    /**
     * Similar to {@link GraphUpdateContext#update(ElementMutation, Update)} but
     * prepares the mutation from the element.
     */
    public <T extends Element> T update(T element, Update<T> updateFn) {
        Date modifiedDate = null;
        VisibilityJson visibilityJson = null;
        String conceptType = null;
        return update(element, modifiedDate, visibilityJson, conceptType, updateFn);
    }

    /**
     * Similar to {@link GraphUpdateContext#update(Element, Update)} but calls
     * {@link ElementUpdateContext#updateBuiltInProperties(Date, VisibilityJson)} before calling
     * updateFn.
     */
    public <T extends Element> T update(
            T element,
            Date modifiedDate,
            VisibilityJson visibilityJson,
            Update<T> updateFn
    ) {
        String conceptType = null;
        return update(element, modifiedDate, visibilityJson, conceptType, updateFn);
    }

    /**
     * Similar to {@link GraphUpdateContext#update(Element, Update)} but calls
     * {@link ElementUpdateContext#updateBuiltInProperties(Date, VisibilityJson)} and
     * {@link ElementUpdateContext#setConceptType(String)} before calling
     * updateFn.
     */
    public <T extends Element> T update(
            T element,
            Date modifiedDate,
            VisibilityJson visibilityJson,
            String conceptType,
            Update<T> updateFn
    ) {
        checkNotNull(element, "element cannot be null");
        return update(element.prepareMutation(), modifiedDate, visibilityJson, conceptType, updateFn);
    }

    public <T extends Element> T update(ElementMutation<T> m, Update<T> updateFn) {
        Date modifiedDate = null;
        VisibilityJson visibilityJson = null;
        String conceptType = null;
        return update(m, modifiedDate, visibilityJson, conceptType, updateFn);
    }

    /**
     * Similar to {@link GraphUpdateContext#update(ElementMutation, Update)} but calls
     * {@link ElementUpdateContext#updateBuiltInProperties(Date, VisibilityJson)} before calling
     * updateFn.
     */
    public <T extends Element> T update(
            ElementMutation<T> m,
            Date modifiedDate,
            VisibilityJson visibilityJson,
            Update<T> updateFn
    ) {
        String conceptType = null;
        return update(m, modifiedDate, visibilityJson, conceptType, updateFn);
    }

    /**
     * Similar to {@link GraphUpdateContext#update(ElementMutation, Update)} but calls
     * {@link ElementUpdateContext#updateBuiltInProperties(Date, VisibilityJson)} and
     * {@link ElementUpdateContext#setConceptType(String)} before calling
     * updateFn.
     */
    public <T extends Element> T update(
            ElementMutation<T> m,
            Date modifiedDate,
            VisibilityJson visibilityJson,
            String conceptType,
            Update<T> updateFn
    ) {
        checkNotNull(m, "element cannot be null");
        checkNotNull(updateFn, "updateFn cannot be null");

        ElementUpdateContext<T> elementUpdateContext = new ElementUpdateContext<>(visibilityTranslator, m, user);
        if (modifiedDate != null || visibilityJson != null) {
            elementUpdateContext.updateBuiltInProperties(modifiedDate, visibilityJson);
        }
        if (conceptType != null) {
            elementUpdateContext.setConceptType(conceptType);
        }
        updateFn.update(elementUpdateContext);
        T elem = elementUpdateContext.save(authorizations);
        addToElementUpdateContexts(elementUpdateContext);
        return elem;
    }

    private <T extends Element> void addToElementUpdateContexts(ElementUpdateContext<T> elementUpdateContext) {
        if (isPushOnQueue()) {
            elementUpdateContexts.add(elementUpdateContext);
            if (elementUpdateContexts.size() > maxElementUpdateContextItems) {
                pushToWorkQueueRepository();
            }
        }
    }

    public interface Update<T extends Element> {
        void update(ElementUpdateContext<T> elemCtx);
    }

    public Priority getPriority() {
        return priority;
    }

    public User getUser() {
        return user;
    }

    public Authorizations getAuthorizations() {
        return authorizations;
    }

    public int getMaxElementUpdateContextItems() {
        return maxElementUpdateContextItems;
    }

    public GraphUpdateContext setMaxElementUpdateContextItems(int maxElementUpdateContextItems) {
        this.maxElementUpdateContextItems = maxElementUpdateContextItems;
        return this;
    }

    public boolean isPushOnQueue() {
        return pushOnQueue;
    }

    public GraphUpdateContext setPushOnQueue(boolean pushOnQueue) {
        this.pushOnQueue = pushOnQueue;
        return this;
    }
}
