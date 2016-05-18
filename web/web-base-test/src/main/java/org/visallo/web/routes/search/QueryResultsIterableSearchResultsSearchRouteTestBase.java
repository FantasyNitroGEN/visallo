package org.visallo.web.routes.search;

import org.mockito.Mock;
import org.mockito.Mockito;
import org.vertexium.Element;
import org.vertexium.query.Aggregation;
import org.vertexium.query.AggregationResult;
import org.vertexium.query.Query;
import org.vertexium.query.QueryResultsIterable;
import org.visallo.core.model.search.ElementSearchRunnerBase;
import org.visallo.core.model.search.QueryResultsIterableSearchResults;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.mockito.Mockito.when;

public abstract class QueryResultsIterableSearchResultsSearchRouteTestBase extends SearchRouteTestBase {
    protected List<Aggregation> aggregations;
    protected long queryResultsIterableTotalHits;
    protected ArrayList<Element> queryResultsIterableElements;
    protected ElementSearchRunnerBase.QueryAndData queryAndData;

    @Mock
    protected QueryResultsIterableSearchResults results;

    @Mock
    protected Query query;

    @Override
    protected void before() throws IOException {
        super.before();

        aggregations = new ArrayList<>();
        queryResultsIterableTotalHits = 0L;
        queryResultsIterableElements = new ArrayList<>();

        QueryResultsIterable resultsIterable = new QueryResultsIterable<Element>() {
            @Override
            public <TResult extends AggregationResult> TResult getAggregationResult(String s, Class<? extends TResult> aClass) {
                return null;
            }

            @Override
            public void close() throws IOException {

            }

            @Override
            public long getTotalHits() {
                return queryResultsIterableTotalHits;
            }

            @Override
            public Iterator<Element> iterator() {
                return queryResultsIterableElements.iterator();
            }
        };
        when(results.getQueryResultsIterable()).thenReturn(resultsIterable);

        when(query.getAggregations()).thenReturn(aggregations);

        queryAndData = createQueryAndData();
        when(queryAndData.getQuery()).thenReturn(query);

        when(results.getQueryAndData()).thenReturn(queryAndData);
    }

    protected ElementSearchRunnerBase.QueryAndData createQueryAndData() {
        return Mockito.mock(ElementSearchRunnerBase.QueryAndData.class);
    }
}
