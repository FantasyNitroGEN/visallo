package org.visallo.core.model.search;

import org.vertexium.Element;

public class VertexFindRelatedSearchResults extends VertexiumObjectsSearchResults {
    private final Iterable<? extends Element> elements;
    private final long count;

    public VertexFindRelatedSearchResults(Iterable<? extends Element> elements, long count) {
        this.elements = elements;
        this.count = count;
    }

    @Override
    public Iterable<? extends Element> getVertexiumObjects() {
        return elements;
    }

    public long getCount() {
        return count;
    }
}
