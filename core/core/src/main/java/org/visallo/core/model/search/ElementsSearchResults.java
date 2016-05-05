package org.visallo.core.model.search;

import org.vertexium.Element;

public abstract class ElementsSearchResults extends SearchResults implements AutoCloseable {
    @Override
    public void close() throws Exception {

    }

    public abstract Iterable<? extends Element> getElements();
}
