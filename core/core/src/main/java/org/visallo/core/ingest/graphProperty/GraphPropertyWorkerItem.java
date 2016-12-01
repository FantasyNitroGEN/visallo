package org.visallo.core.ingest.graphProperty;

import com.google.common.collect.ImmutableList;
import org.vertexium.Element;

public class GraphPropertyWorkerItem extends WorkerItem {
    private final GraphPropertyMessage message;
    private final ImmutableList<Element> elements;

    public GraphPropertyWorkerItem(GraphPropertyMessage message, ImmutableList<Element> elements) {
        this.message = message;
        this.elements = elements;
    }

    public GraphPropertyMessage getMessage() {
        return message;
    }

    public ImmutableList<Element> getElements() {
        return elements;
    }
}
