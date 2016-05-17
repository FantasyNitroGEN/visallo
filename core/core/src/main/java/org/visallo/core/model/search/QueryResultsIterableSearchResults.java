package org.visallo.core.model.search;

import org.vertexium.Element;
import org.vertexium.query.QueryResultsIterable;

public class QueryResultsIterableSearchResults extends ElementsSearchResults implements AutoCloseable {
    private final QueryResultsIterable<? extends Element> searchResults;
    private final ElementSearchRunnerBase.QueryAndData queryAndData;
    private final long offset;
    private final long size;

    public QueryResultsIterableSearchResults(
            QueryResultsIterable<? extends Element> searchResults,
            ElementSearchRunnerBase.QueryAndData queryAndData,
            long offset,
            long size
    ) {
        this.searchResults = searchResults;
        this.queryAndData = queryAndData;
        this.offset = offset;
        this.size = size;
    }

    public QueryResultsIterable<? extends Element> getQueryResultsIterable() {
        return searchResults;
    }

    public ElementSearchRunnerBase.QueryAndData getQueryAndData() {
        return queryAndData;
    }

    public long getOffset() {
        return offset;
    }

    public long getSize() {
        return size;
    }

    @Override
    public void close() throws Exception {
        this.searchResults.close();
    }

    @Override
    public Iterable<? extends Element> getElements() {
        return searchResults;
    }
}
