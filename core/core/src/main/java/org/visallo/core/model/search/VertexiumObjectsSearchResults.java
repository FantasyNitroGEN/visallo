package org.visallo.core.model.search;

import org.vertexium.VertexiumObject;

public abstract class VertexiumObjectsSearchResults extends SearchResults implements AutoCloseable {
    @Override
    public void close() throws Exception {

    }

    public abstract Iterable<? extends VertexiumObject> getVertexiumObjects();
}
