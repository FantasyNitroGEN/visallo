package org.visallo.web.routes.vertex;

import com.v5analytics.webster.annotations.Handle;
import org.vertexium.Authorizations;
import org.vertexium.Edge;
import org.vertexium.Element;
import org.vertexium.Vertex;
import org.vertexium.query.*;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.search.QueryResultsIterableSearchResults;
import org.visallo.core.model.search.ElementSearchRunnerBase;
import org.visallo.core.model.search.SearchOptions;
import org.visallo.core.user.User;
import org.visallo.core.util.ClientApiConverter;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.clientapi.model.ClientApiElement;
import org.visallo.web.clientapi.model.ClientApiElementSearchResponse;
import org.visallo.web.clientapi.model.ClientApiSearchResponse;
import org.visallo.web.parameterProviders.ActiveWorkspaceId;
import org.visallo.web.routes.search.WebSearchOptionsFactory;

import javax.servlet.http.HttpServletRequest;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class ElementSearchBase {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(ElementSearchBase.class);
    private static final Pattern DATE_TIME_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}T.*");
    private final ElementSearchRunnerBase searchRunner;

    public ElementSearchBase(ElementSearchRunnerBase searchRunner) {
        checkNotNull(searchRunner, "searchRunner is required");
        this.searchRunner = searchRunner;
    }

    @Handle
    public ClientApiElementSearchResponse handle(
            HttpServletRequest request,
            @ActiveWorkspaceId String workspaceId,
            User user,
            Authorizations authorizations
    ) throws Exception {
        SearchOptions searchOptions = WebSearchOptionsFactory.create(request, workspaceId);
        try (QueryResultsIterableSearchResults searchResults = this.searchRunner.run(searchOptions, user, authorizations)) {
            Map<String, Double> scores = null;
            if (searchResults.getQueryResultsIterable() instanceof IterableWithScores) {
                scores = ((IterableWithScores<?>) searchResults.getQueryResultsIterable()).getScores();
            }

            List<ClientApiElement> elementList = convertElementsToClientApi(
                    searchResults.getQueryAndData(),
                    searchResults.getQueryResultsIterable(),
                    scores,
                    searchOptions.getWorkspaceId(),
                    authorizations
            );

            ClientApiElementSearchResponse results = new ClientApiElementSearchResponse();
            results.getElements().addAll(elementList);
            results.setNextOffset((int) (searchResults.getOffset() + searchResults.getSize()));

            addSearchResultsDataToResults(results, searchResults.getQueryAndData(), searchResults.getQueryResultsIterable());

            return results;
        }
    }


    private void addSearchResultsDataToResults(
            ClientApiElementSearchResponse results,
            ElementSearchRunnerBase.QueryAndData queryAndData,
            QueryResultsIterable<? extends Element> searchResults
    ) {
        results.setTotalHits(searchResults.getTotalHits());

        if (searchResults instanceof IterableWithSearchTime) {
            results.setSearchTime(((IterableWithSearchTime) searchResults).getSearchTimeNanoSeconds());
        }
        for (Aggregation aggregation : queryAndData.getQuery().getAggregations()) {
            results.getAggregates().put(aggregation.getAggregationName(), toClientApiAggregateResult(searchResults, aggregation));
        }
    }

    private ClientApiSearchResponse.AggregateResult toClientApiAggregateResult(QueryResultsIterable<? extends Element> searchResults, Aggregation aggregation) {
        AggregationResult aggResult;
        if (aggregation instanceof TermsAggregation) {
            aggResult = searchResults.getAggregationResult(aggregation.getAggregationName(), TermsResult.class);
        } else if (aggregation instanceof GeohashAggregation) {
            aggResult = searchResults.getAggregationResult(aggregation.getAggregationName(), GeohashResult.class);
        } else if (aggregation instanceof HistogramAggregation || aggregation instanceof CalendarFieldAggregation) {
            aggResult = searchResults.getAggregationResult(aggregation.getAggregationName(), HistogramResult.class);
        } else if (aggregation instanceof StatisticsAggregation) {
            aggResult = searchResults.getAggregationResult(aggregation.getAggregationName(), StatisticsResult.class);
        } else {
            throw new VisalloException("Unhandled aggregation type: " + aggregation.getClass().getName());
        }
        return toClientApiAggregateResult(aggResult);
    }

    private Map<String, ClientApiSearchResponse.AggregateResult> toClientApiNestedResults(Map<String, AggregationResult> nestedResults) {
        Map<String, ClientApiSearchResponse.AggregateResult> results = new HashMap<>();
        for (Map.Entry<String, AggregationResult> entry : nestedResults.entrySet()) {
            ClientApiSearchResponse.AggregateResult aggResult = toClientApiAggregateResult(entry.getValue());
            results.put(entry.getKey(), aggResult);
        }
        if (results.size() == 0) {
            return null;
        }
        return results;
    }

    private ClientApiSearchResponse.AggregateResult toClientApiAggregateResult(AggregationResult aggResult) {
        if (aggResult instanceof TermsResult) {
            return toClientApiTermsAggregateResult((TermsResult) aggResult);
        }
        if (aggResult instanceof GeohashResult) {
            return toClientApiGeohashResult((GeohashResult) aggResult);
        }
        if (aggResult instanceof HistogramResult) {
            return toClientApiHistogramResult((HistogramResult) aggResult);
        }
        if (aggResult instanceof StatisticsResult) {
            return toClientApiStatisticsResult((StatisticsResult) aggResult);
        }
        throw new VisalloException("Unhandled aggregation result type: " + aggResult.getClass().getName());
    }

    private ClientApiSearchResponse.AggregateResult toClientApiStatisticsResult(StatisticsResult agg) {
        ClientApiSearchResponse.StatisticsAggregateResult result = new ClientApiSearchResponse.StatisticsAggregateResult();
        result.setCount(agg.getCount());
        result.setAverage(agg.getAverage());
        result.setMin(agg.getMin());
        result.setMax(agg.getMax());
        result.setStandardDeviation(agg.getStandardDeviation());
        result.setSum(agg.getSum());
        return result;
    }

    private ClientApiSearchResponse.AggregateResult toClientApiHistogramResult(HistogramResult agg) {
        DateFormat bucketDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        ClientApiSearchResponse.HistogramAggregateResult result = new ClientApiSearchResponse.HistogramAggregateResult();
        for (HistogramBucket histogramBucket : agg.getBuckets()) {
            ClientApiSearchResponse.HistogramAggregateResult.Bucket b = new ClientApiSearchResponse.HistogramAggregateResult.Bucket(
                    histogramBucket.getCount(),
                    toClientApiNestedResults(histogramBucket.getNestedResults())
            );
            String key = histogramBucket.getKey().toString();
            if (DATE_TIME_PATTERN.matcher(key).matches()) {
                try {
                    Date date = bucketDateFormat.parse(key);
                    if (date != null) {
                        key = String.valueOf(date.getTime());
                    }
                } catch (ParseException pe) {
                    LOGGER.warn("Unable to parse histogram date", pe);
                }
            }
            result.getBuckets().put(key, b);
        }
        return result;
    }

    private ClientApiSearchResponse.AggregateResult toClientApiGeohashResult(GeohashResult agg) {
        ClientApiSearchResponse.GeohashAggregateResult result = new ClientApiSearchResponse.GeohashAggregateResult();
        result.setMaxCount(agg.getMaxCount());
        for (GeohashBucket geohashBucket : agg.getBuckets()) {
            ClientApiSearchResponse.GeohashAggregateResult.Bucket b = new ClientApiSearchResponse.GeohashAggregateResult.Bucket(
                    ClientApiConverter.toClientApiGeoRect(geohashBucket.getGeoCell()),
                    ClientApiConverter.toClientApiGeoPoint(geohashBucket.getGeoPoint()),
                    geohashBucket.getCount(),
                    toClientApiNestedResults(geohashBucket.getNestedResults())
            );
            result.getBuckets().put(geohashBucket.getKey(), b);
        }
        return result;
    }

    private ClientApiSearchResponse.TermsAggregateResult toClientApiTermsAggregateResult(TermsResult agg) {
        ClientApiSearchResponse.TermsAggregateResult result = new ClientApiSearchResponse.TermsAggregateResult();
        for (TermsBucket termsBucket : agg.getBuckets()) {
            ClientApiSearchResponse.TermsAggregateResult.Bucket b = new ClientApiSearchResponse.TermsAggregateResult.Bucket(
                    termsBucket.getCount(),
                    toClientApiNestedResults(termsBucket.getNestedResults())
            );
            result.getBuckets().put(termsBucket.getKey().toString(), b);
        }
        return result;
    }

    protected List<ClientApiElement> convertElementsToClientApi(
            ElementSearchRunnerBase.QueryAndData queryAndData,
            Iterable<? extends Element> searchResults,
            Map<String, Double> scores,
            String workspaceId,
            Authorizations authorizations
    ) {
        List<ClientApiElement> elementsList = new ArrayList<>();
        for (Element element : searchResults) {
            Integer commonCount = getCommonCount(queryAndData, element);
            ClientApiElement elem;
            if (element instanceof Vertex) {
                elem = ClientApiConverter.toClientApiVertex((Vertex) element, workspaceId, commonCount, authorizations);
            } else if (element instanceof Edge) {
                elem = ClientApiConverter.toClientApiEdge((Edge) element, workspaceId);
            } else {
                throw new VisalloException("Unhandled element type: " + element.getClass().getName());
            }
            if (scores != null) {
                elem.setScore(scores.get(element.getId()));
            }
            elementsList.add(elem);
        }
        return elementsList;
    }

    protected Integer getCommonCount(ElementSearchRunnerBase.QueryAndData queryAndData, Element element) {
        return null;
    }
}
