package org.visallo.web.routes.vertex;

import com.google.inject.Inject;
import org.json.JSONArray;
import org.json.JSONObject;
import org.vertexium.*;
import org.vertexium.query.*;
import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.directory.DirectoryRepository;
import org.visallo.core.model.ontology.OntologyProperty;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.trace.Trace;
import org.visallo.core.trace.TraceSpan;
import org.visallo.core.user.User;
import org.visallo.core.util.ClientApiConverter;
import org.visallo.core.util.JSONUtil;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.clientapi.model.*;
import org.visallo.web.parameterProviders.VisalloBaseParameterProvider;

import javax.servlet.http.HttpServletRequest;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

public abstract class ElementSearchBase {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(ElementSearchBase.class);
    private static final Pattern dateTimePattern = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}T.*");
    private final Graph graph;
    private final DirectoryRepository directoryRepository;
    private final OntologyRepository ontologyRepository;
    private int defaultSearchResultCount;

    @Inject
    public ElementSearchBase(
            OntologyRepository ontologyRepository,
            Graph graph,
            Configuration configuration,
            DirectoryRepository directoryRepository
    ) {
        this.ontologyRepository = ontologyRepository;
        this.graph = graph;
        this.directoryRepository = directoryRepository;
        defaultSearchResultCount = configuration.getInt(Configuration.DEFAULT_SEARCH_RESULT_COUNT, 100);
    }

    protected ClientApiElementSearchResponse handle(
            HttpServletRequest request,
            String workspaceId,
            User user,
            Authorizations authorizations
    ) throws Exception {
        long totalStartTime = System.nanoTime();

        long startTime = System.nanoTime();

        JSONArray filterJson = getFilterJson(request);

        QueryAndData queryAndData = getQuery(request, authorizations);
        applyFiltersToQuery(queryAndData, filterJson, user);
        applyConceptTypeFilterToQuery(queryAndData, request);
        applyEdgeLabelFilterToQuery(queryAndData, request);
        applySortToQuery(queryAndData, request);
        applyAggregationsToQuery(queryAndData, request);

        EnumSet<FetchHint> fetchHints = VisalloBaseParameterProvider.getOptionalParameterFetchHints(request, "fetchHints", ClientApiConverter.SEARCH_FETCH_HINTS);
        final int offset = VisalloBaseParameterProvider.getOptionalParameterInt(request, "offset", 0);
        final int size = VisalloBaseParameterProvider.getOptionalParameterInt(request, "size", defaultSearchResultCount);
        queryAndData.getQuery().limit(size);
        queryAndData.getQuery().skip(offset);

        QueryResultsIterable<? extends Element> searchResults = getSearchResults(queryAndData, fetchHints);

        Map<String, Double> scores = null;
        if (searchResults instanceof IterableWithScores) {
            scores = ((IterableWithScores<?>) searchResults).getScores();
        }

        long retrievalStartTime = System.nanoTime();
        List<ClientApiElement> elementList = convertElementsToClientApi(queryAndData, searchResults, scores, workspaceId, authorizations);
        long retrievalEndTime = System.nanoTime();

        long totalEndTime = System.nanoTime();

        ClientApiElementSearchResponse results = new ClientApiElementSearchResponse();
        results.getElements().addAll(elementList);
        results.setNextOffset(offset + size);
        results.setRetrievalTime(retrievalEndTime - retrievalStartTime);
        results.setTotalTime(totalEndTime - totalStartTime);

        addSearchResultsDataToResults(results, queryAndData, searchResults);

        long endTime = System.nanoTime();
        LOGGER.info("Search found %d vertices in %dms", elementList.size(), (endTime - startTime) / 1000 / 1000);

        searchResults.close();

        return results;
    }

    private void addSearchResultsDataToResults(ClientApiElementSearchResponse results, QueryAndData queryAndData, QueryResultsIterable<? extends Element> searchResults) {
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
        } else if (aggregation instanceof HistogramAggregation) {
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
            if (dateTimePattern.matcher(key).matches()) {
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

    private void applyAggregationsToQuery(QueryAndData queryAndData, HttpServletRequest request) {
        Query query = queryAndData.getQuery();
        String[] aggregates = VisalloBaseParameterProvider.getOptionalParameterAsStringArray(request, "aggregations[]");
        if (aggregates == null) {
            return;
        }
        for (String aggregate : aggregates) {
            JSONObject aggregateJson = new JSONObject(aggregate);
            Aggregation aggregation = getAggregation(aggregateJson);
            query.addAggregation(aggregation);
        }
    }

    private Aggregation getAggregation(JSONObject aggregateJson) {
        String field;
        String aggregationName = aggregateJson.getString("name");
        String type = aggregateJson.getString("type");
        Aggregation aggregation;
        switch (type) {
            case "term":
                field = aggregateJson.getString("field");
                aggregation = new TermsAggregation(aggregationName, field);
                break;
            case "geohash":
                field = aggregateJson.getString("field");
                int precision = aggregateJson.getInt("precision");
                aggregation = new GeohashAggregation(aggregationName, field, precision);
                break;
            case "histogram":
                field = aggregateJson.getString("field");
                String interval = aggregateJson.getString("interval");
                Long minDocumentCount = JSONUtil.getOptionalLong(aggregateJson, "minDocumentCount");
                aggregation = new HistogramAggregation(aggregationName, field, interval, minDocumentCount);
                break;
            case "statistics":
                field = aggregateJson.getString("field");
                aggregation = new StatisticsAggregation(aggregationName, field);
                break;
            default:
                throw new VisalloException("Invalid aggregation type: " + type);
        }

        JSONArray nestedAggregates = aggregateJson.optJSONArray("nested");
        if (nestedAggregates != null && nestedAggregates.length() > 0) {
            if (!(aggregation instanceof SupportsNestedAggregationsAggregation)) {
                throw new VisalloException("Aggregation does not support nesting: " + aggregation.getClass().getName());
            }
            for (int i = 0; i < nestedAggregates.length(); i++) {
                JSONObject nestedAggregateJson = nestedAggregates.getJSONObject(i);
                Aggregation nestedAggregate = getAggregation(nestedAggregateJson);
                ((SupportsNestedAggregationsAggregation) aggregation).addNestedAggregation(nestedAggregate);
            }
        }

        return aggregation;
    }

    protected void applySortToQuery(QueryAndData queryAndData, HttpServletRequest request) {
        String[] sorts = VisalloBaseParameterProvider.getOptionalParameterAsStringArray(request, "sort[]");
        if (sorts == null) {
            return;
        }
        for (String sort : sorts) {
            String propertyName = sort;
            SortDirection direction = SortDirection.ASCENDING;
            if (propertyName.toUpperCase().endsWith(":ASCENDING")) {
                direction = SortDirection.ASCENDING;
                propertyName = propertyName.substring(0, propertyName.length() - ":ASCENDING".length());
            } else if (propertyName.toUpperCase().endsWith(":DESCENDING")) {
                direction = SortDirection.DESCENDING;
                propertyName = propertyName.substring(0, propertyName.length() - ":DESCENDING".length());
            }
            queryAndData.getQuery().sort(propertyName, direction);
        }
    }

    protected List<ClientApiElement> convertElementsToClientApi(
            QueryAndData queryAndData,
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

    protected QueryResultsIterable<? extends Element> getSearchResults(QueryAndData queryAndData, EnumSet<FetchHint> fetchHints) {
        //noinspection unused
        try (TraceSpan trace = Trace.start("getSearchResults")) {
            if (getResultType().contains(ElementType.VERTEX) && getResultType().contains(ElementType.EDGE)) {
                return queryAndData.getQuery().elements(fetchHints);
            } else if (getResultType().contains(ElementType.VERTEX)) {
                return queryAndData.getQuery().vertices(fetchHints);
            } else if (getResultType().contains(ElementType.EDGE)) {
                return queryAndData.getQuery().edges(fetchHints);
            } else {
                throw new VisalloException("Unexpected result type: " + getResultType());
            }
        }
    }

    protected abstract EnumSet<ElementType> getResultType();

    protected Integer getCommonCount(QueryAndData queryAndData, Element element) {
        return null;
    }

    protected abstract QueryAndData getQuery(HttpServletRequest request, Authorizations authorizations);

    protected void applyConceptTypeFilterToQuery(QueryAndData queryAndData, HttpServletRequest request) {
        final String conceptType = VisalloBaseParameterProvider.getOptionalParameter(request, "conceptType");
        final String includeChildNodes = VisalloBaseParameterProvider.getOptionalParameter(request, "includeChildNodes");
        Query query = queryAndData.getQuery();
        if (conceptType != null) {
            boolean includeChildNodesBoolean = includeChildNodes == null || !includeChildNodes.equals("false");
            ontologyRepository.addConceptTypeFilterToQuery(query, conceptType, includeChildNodesBoolean);
        }
    }

    protected void applyEdgeLabelFilterToQuery(QueryAndData queryAndData, HttpServletRequest request) {
        final String edgeLabel = VisalloBaseParameterProvider.getOptionalParameter(request, "edgeLabel");
        final String includeChildNodes = VisalloBaseParameterProvider.getOptionalParameter(request, "includeChildNodes");
        Query query = queryAndData.getQuery();
        if (edgeLabel != null) {
            boolean includeChildNodesBoolean = includeChildNodes == null || !includeChildNodes.equals("false");
            ontologyRepository.addEdgeLabelFilterToQuery(query, edgeLabel, includeChildNodesBoolean);
        }
    }

    protected void applyFiltersToQuery(QueryAndData queryAndData, JSONArray filterJson, User user) throws ParseException {
        for (int i = 0; i < filterJson.length(); i++) {
            JSONObject obj = filterJson.getJSONObject(i);
            if (obj.length() > 0) {
                updateQueryWithFilter(queryAndData.getQuery(), obj, user);
            }
        }
    }

    protected JSONArray getFilterJson(HttpServletRequest request) {
        final String filter = VisalloBaseParameterProvider.getRequiredParameter(request, "filter");
        JSONArray filterJson = new JSONArray(filter);
        ontologyRepository.resolvePropertyIds(filterJson);
        return filterJson;
    }

    private void updateQueryWithFilter(Query graphQuery, JSONObject obj, User user) throws ParseException {
        String predicateString = obj.optString("predicate");
        String propertyName = obj.getString("propertyName");
        if ("has".equals(predicateString)) {
            graphQuery.has(propertyName);
        } else if ("hasNot".equals(predicateString)) {
            graphQuery.hasNot(propertyName);
        } else if ("in".equals(predicateString)) {
            graphQuery.has(propertyName, Contains.IN, JSONUtil.toList(obj.getJSONArray("values")));
        } else {
            PropertyType propertyDataType = PropertyType.convert(obj.optString("propertyDataType"));
            JSONArray values = obj.getJSONArray("values");
            Object value0 = jsonValueToObject(values, propertyDataType, 0);

            if (PropertyType.STRING.equals(propertyDataType) && (predicateString == null || "~".equals(predicateString) || "".equals(predicateString))) {
                graphQuery.has(propertyName, TextPredicate.CONTAINS, value0);
            } else if (PropertyType.DATE.equals(propertyDataType)) {
                applyDateToQuery(graphQuery, obj, predicateString, values);
            } else if (PropertyType.BOOLEAN.equals(propertyDataType)) {
                graphQuery.has(propertyName, Compare.EQUAL, value0);
            } else if (PropertyType.GEO_LOCATION.equals(propertyDataType)) {
                graphQuery.has(propertyName, GeoCompare.WITHIN, value0);
            } else if ("<".equals(predicateString)) {
                graphQuery.has(propertyName, Compare.LESS_THAN, value0);
            } else if (">".equals(predicateString)) {
                graphQuery.has(propertyName, Compare.GREATER_THAN, value0);
            } else if ("range".equals(predicateString)) {
                graphQuery.has(propertyName, Compare.GREATER_THAN_EQUAL, value0);
                graphQuery.has(propertyName, Compare.LESS_THAN_EQUAL, jsonValueToObject(values, propertyDataType, 1));
            } else if ("=".equals(predicateString) || "equal".equals(predicateString)) {
                if (PropertyType.DIRECTORY_ENTITY.equals(propertyDataType) && value0 instanceof JSONObject) {
                    applyDirectoryEntityJsonObjectEqualityToQuery(graphQuery, propertyName, (JSONObject) value0, user);
                } else if (PropertyType.DOUBLE.equals(propertyDataType)) {
                    applyDoubleEqualityToQuery(graphQuery, obj, value0);
                } else {
                    graphQuery.has(propertyName, Compare.EQUAL, value0);
                }
            } else {
                throw new VisalloException("unhandled query\n" + obj.toString(2));
            }
        }
    }

    private void applyDirectoryEntityJsonObjectEqualityToQuery(Query graphQuery, String propertyName, JSONObject value0, User user) {
        String directoryEntityId = value0.optString("directoryEntityId", null);
        if (directoryEntityId != null) {
            graphQuery.has(propertyName, Compare.EQUAL, directoryEntityId);
        } else if (value0.optBoolean("currentUser", false)) {
            directoryEntityId = directoryRepository.getDirectoryEntityId(user);
            graphQuery.has(propertyName, Compare.EQUAL, directoryEntityId);
        } else {
            throw new VisalloException("Invalid directory entity JSONObject filter:\n" + value0.toString(2));
        }
    }

    private void applyDoubleEqualityToQuery(Query graphQuery, JSONObject obj, Object value0) throws ParseException {
        String propertyName = obj.getString("propertyName");
        JSONObject metadata = obj.has("metadata") ? obj.getJSONObject("metadata") : null;

        if (metadata != null && metadata.has("http://visallo.org#inputPrecision") && value0 instanceof Double) {
            double doubleParam = (double) value0;
            double buffer = Math.pow(10, -(Math.abs(metadata.getInt("http://visallo.org#inputPrecision")) + 1)) * 5;

            graphQuery.has(propertyName, Compare.GREATER_THAN, doubleParam - buffer);
            graphQuery.has(propertyName, Compare.LESS_THAN, doubleParam + buffer);
        } else {
            graphQuery.has(propertyName, Compare.EQUAL, value0);
        }
    }

    private void applyDateToQuery(Query graphQuery, JSONObject obj, String predicate, JSONArray values) throws ParseException {
        String propertyName = obj.getString("propertyName");
        PropertyType propertyDataType = PropertyType.DATE;
        OntologyProperty property = ontologyRepository.getPropertyByIRI(propertyName);

        if (property != null && values.length() > 0) {
            String displayType = property.getDisplayType();
            boolean isDateOnly = displayType != null && displayType.equals("dateOnly");
            boolean isRelative = values.get(0) instanceof JSONObject;

            Calendar calendar = new GregorianCalendar();
            calendar.setTimeZone(TimeZone.getTimeZone("UTC"));

            if (isRelative) {
                JSONObject fromNow = (JSONObject) values.get(0);
                calendar.setTime(new Date());
                moveDateToStart(calendar, isDateOnly);
                //noinspection MagicConstant
                calendar.add(fromNow.getInt("unit"), fromNow.getInt("amount"));
            } else {
                Date date0 = (Date) jsonValueToObject(values, propertyDataType, 0);
                calendar.setTime(date0);
            }

            if (predicate == null || predicate.equals("equal") || predicate.equals("=")) {
                moveDateToStart(calendar, isDateOnly);
                graphQuery.has(propertyName, Compare.GREATER_THAN_EQUAL, calendar.getTime());

                moveDateToEnd(calendar, isDateOnly);
                graphQuery.has(propertyName, Compare.LESS_THAN, calendar.getTime());
            } else if (predicate.equals("range")) {
                if (!isRelative) {
                    moveDateToStart(calendar, isDateOnly);
                }
                graphQuery.has(propertyName, Compare.GREATER_THAN_EQUAL, calendar.getTime());

                if (values.get(1) instanceof JSONObject) {
                    JSONObject fromNow = (JSONObject) values.get(1);
                    calendar.setTime(new Date());
                    moveDateToStart(calendar, isDateOnly);
                    //noinspection MagicConstant
                    calendar.add(fromNow.getInt("unit"), fromNow.getInt("amount"));
                    moveDateToEnd(calendar, isDateOnly);
                    graphQuery.has(propertyName, Compare.LESS_THAN, calendar.getTime());
                } else {
                    calendar.setTime((Date) jsonValueToObject(values, propertyDataType, 1));
                    moveDateToEnd(calendar, isDateOnly);
                    graphQuery.has(propertyName, Compare.LESS_THAN, calendar.getTime());
                }
            } else if (predicate.equals("<")) {
                moveDateToStart(calendar, isDateOnly);
                graphQuery.has(propertyName, Compare.LESS_THAN, calendar.getTime());
            } else if (predicate.equals(">")) {
                moveDateToEnd(calendar, isDateOnly);
                graphQuery.has(propertyName, Compare.GREATER_THAN_EQUAL, calendar.getTime());
            }
        }
    }

    private void moveDateToStart(Calendar calendar, boolean dateOnly) {
        if (dateOnly) {
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
        }
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }

    private void moveDateToEnd(Calendar calendar, boolean dateOnly) {
        if (dateOnly) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        } else {
            calendar.add(Calendar.MINUTE, 1);
        }
    }

    private Object jsonValueToObject(JSONArray values, PropertyType propertyDataType, int index) throws ParseException {
        // JSONObject can be sent to search in the case of relative date searching or advanced directory entry searching
        if (values.get(index) instanceof JSONObject) {
            return values.get(index);
        }
        return OntologyProperty.convert(values, propertyDataType, index);
    }

    protected Graph getGraph() {
        return graph;
    }

    protected static class QueryAndData {
        private final Query query;

        public QueryAndData(Query query) {
            this.query = query;
        }

        public Query getQuery() {
            return query;
        }
    }
}
