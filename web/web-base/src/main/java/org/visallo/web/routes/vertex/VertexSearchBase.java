package org.visallo.web.routes.vertex;

import com.google.inject.Inject;
import com.v5analytics.webster.HandlerChain;
import org.json.JSONArray;
import org.json.JSONObject;
import org.vertexium.Authorizations;
import org.vertexium.FetchHint;
import org.vertexium.Graph;
import org.vertexium.Vertex;
import org.vertexium.query.*;
import org.vertexium.util.CloseableIterable;
import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.ontology.Concept;
import org.visallo.core.model.ontology.OntologyProperty;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.trace.Trace;
import org.visallo.core.trace.TraceSpan;
import org.visallo.core.user.User;
import org.visallo.core.util.ClientApiConverter;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.BaseRequestHandler;
import org.visallo.web.clientapi.model.ClientApiVertex;
import org.visallo.web.clientapi.model.ClientApiVertexSearchResponse;
import org.visallo.web.clientapi.model.PropertyType;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.ParseException;
import java.util.*;

public abstract class VertexSearchBase extends BaseRequestHandler {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(VertexSearch.class);
    private final Graph graph;
    private final OntologyRepository ontologyRepository;
    private int defaultSearchResultCount;

    @Inject
    public VertexSearchBase(
            final OntologyRepository ontologyRepository,
            final Graph graph,
            final UserRepository userRepository,
            final Configuration configuration,
            final WorkspaceRepository workspaceRepository) {
        super(userRepository, workspaceRepository, configuration);
        this.ontologyRepository = ontologyRepository;
        this.graph = graph;
        defaultSearchResultCount = configuration.getInt(Configuration.DEFAULT_SEARCH_RESULT_COUNT, 100);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        long totalStartTime = System.nanoTime();

        long startTime = System.nanoTime();

        User user = getUser(request);
        final Authorizations authorizations = getAuthorizations(request, user);
        String workspaceId = getActiveWorkspaceId(request);
        JSONArray filterJson = getFilterJson(request);

        QueryAndData queryAndData = getQuery(request, authorizations);
        applyFiltersToQuery(queryAndData, filterJson);
        applyConceptTypeFilterToQuery(queryAndData, request);

        EnumSet<FetchHint> fetchHints = getOptionalParameterFetchHints(request, "fetchHints", ClientApiConverter.SEARCH_FETCH_HINTS);
        final int offset = getOptionalParameterInt(request, "offset", 0);
        final int size = getOptionalParameterInt(request, "size", defaultSearchResultCount);
        queryAndData.getQuery().limit(size);
        queryAndData.getQuery().skip(offset);

        Iterable<Vertex> searchResults = getSearchResults(queryAndData, fetchHints);

        Map<String, Double> scores = null;
        if (searchResults instanceof IterableWithScores) {
            scores = ((IterableWithScores<?>) searchResults).getScores();
        }

        long retrievalStartTime = System.nanoTime();
        List<ClientApiVertex> verticesList = convertVerticesToClientApi(queryAndData, searchResults, scores, workspaceId, authorizations);
        long retrievalEndTime = System.nanoTime();

        sortVertices(verticesList);

        long totalEndTime = System.nanoTime();

        ClientApiVertexSearchResponse results = new ClientApiVertexSearchResponse();
        results.getVertices().addAll(verticesList);
        results.setNextOffset(offset + size);
        results.setRetrievalTime(retrievalEndTime - retrievalStartTime);
        results.setTotalTime(totalEndTime - totalStartTime);

        if (searchResults instanceof IterableWithTotalHits) {
            results.setTotalHits(((IterableWithTotalHits) searchResults).getTotalHits());
        }
        if (searchResults instanceof IterableWithSearchTime) {
            results.setSearchTime(((IterableWithSearchTime) searchResults).getSearchTimeNanoSeconds());
        }

        long endTime = System.nanoTime();
        LOGGER.info("Search found %d vertices in %dms", verticesList.size(), (endTime - startTime) / 1000 / 1000);

        if (searchResults instanceof CloseableIterable) {
            ((CloseableIterable) searchResults).close();
        }

        respondWithClientApiObject(response, results);
    }

    protected List<ClientApiVertex> convertVerticesToClientApi(
            QueryAndData queryAndData,
            Iterable<Vertex> searchResults,
            Map<String, Double> scores,
            String workspaceId,
            Authorizations authorizations
    ) {
        List<ClientApiVertex> verticesList = new ArrayList<>();
        for (Vertex vertex : searchResults) {
            Integer commonCount = getCommonCount(queryAndData, vertex);
            ClientApiVertex v = ClientApiConverter.toClientApiVertex(vertex, workspaceId, commonCount, authorizations);
            if (scores != null) {
                v.setScore(scores.get(vertex.getId()));
            }
            verticesList.add(v);
        }
        return verticesList;
    }

    protected Iterable<Vertex> getSearchResults(QueryAndData queryAndData, EnumSet<FetchHint> fetchHints) {
        try (TraceSpan trace = Trace.start("getSearchResults")) {
            return queryAndData.getQuery().vertices(fetchHints);
        }
    }

    protected Integer getCommonCount(QueryAndData queryAndData, Vertex vertex) {
        return null;
    }

    protected void sortVertices(List<ClientApiVertex> verticesList) {
        Collections.sort(verticesList, new Comparator<ClientApiVertex>() {
            @Override
            public int compare(ClientApiVertex o1, ClientApiVertex o2) {
                double score1 = o1.getScore(0.0);
                double score2 = o2.getScore(0.0);
                return -Double.compare(score1, score2);
            }
        });
    }

    protected abstract QueryAndData getQuery(HttpServletRequest request, Authorizations authorizations);

    protected void applyConceptTypeFilterToQuery(QueryAndData queryAndData, HttpServletRequest request) {
        final String conceptType = getOptionalParameter(request, "conceptType");
        final String includeChildNodes = getOptionalParameter(request, "includeChildNodes");
        if (conceptType != null) {
            Concept concept = ontologyRepository.getConceptByIRI(conceptType);
            if (includeChildNodes == null || !includeChildNodes.equals("false")) {
                Set<Concept> childConcepts = ontologyRepository.getConceptAndAllChildren(concept);
                if (childConcepts.size() > 0) {
                    String[] conceptIds = new String[childConcepts.size()];
                    int count = 0;
                    for (Concept c : childConcepts) {
                        conceptIds[count] = c.getIRI();
                        count++;
                    }
                    queryAndData.getQuery().has(VisalloProperties.CONCEPT_TYPE.getPropertyName(), Contains.IN, conceptIds);
                }
            } else {
                queryAndData.getQuery().has(VisalloProperties.CONCEPT_TYPE.getPropertyName(), conceptType);
            }
        }
    }

    protected void applyFiltersToQuery(QueryAndData queryAndData, JSONArray filterJson) throws ParseException {
        for (int i = 0; i < filterJson.length(); i++) {
            JSONObject obj = filterJson.getJSONObject(i);
            if (obj.length() > 0) {
                updateQueryWithFilter(queryAndData.getQuery(), obj);
            }
        }
    }

    protected JSONArray getFilterJson(HttpServletRequest request) {
        final String filter = getRequiredParameter(request, "filter");
        JSONArray filterJson = new JSONArray(filter);
        ontologyRepository.resolvePropertyIds(filterJson);
        return filterJson;
    }

    private void updateQueryWithFilter(Query graphQuery, JSONObject obj) throws ParseException {
        String predicateString = obj.optString("predicate");
        String propertyName = obj.getString("propertyName");
        if ("has".equals(predicateString)) {
            graphQuery.has(propertyName);
        } else if ("hasNot".equals(predicateString)) {
            graphQuery.hasNot(propertyName);
        } else {
            PropertyType propertyDataType = PropertyType.convert(obj.optString("propertyDataType"));
            JSONArray values = obj.getJSONArray("values");
            Object value0 = jsonValueToObject(values, propertyDataType, 0);

            if (PropertyType.STRING.equals(propertyDataType) && (predicateString == null || "~".equals(predicateString) || "".equals(predicateString))) {
                graphQuery.has(propertyName, TextPredicate.CONTAINS, value0);
            } else if (PropertyType.BOOLEAN.equals(propertyDataType) && (predicateString == null || "".equals(predicateString))) {
                graphQuery.has(propertyName, Compare.EQUAL, value0);
            } else if ("<".equals(predicateString)) {
                graphQuery.has(propertyName, Compare.LESS_THAN, value0);
            } else if (">".equals(predicateString)) {
                graphQuery.has(propertyName, Compare.GREATER_THAN, value0);
            } else if ("range".equals(predicateString)) {
                graphQuery.has(propertyName, Compare.GREATER_THAN_EQUAL, value0);
                graphQuery.has(propertyName, Compare.LESS_THAN_EQUAL, jsonValueToObject(values, propertyDataType, 1));
            } else if ("=".equals(predicateString) || "equal".equals(predicateString)) {
                graphQuery.has(propertyName, Compare.EQUAL, value0);
            } else if (PropertyType.GEO_LOCATION.equals(propertyDataType)) {
                graphQuery.has(propertyName, GeoCompare.WITHIN, value0);
            } else {
                throw new VisalloException("unhandled query\n" + obj.toString(2));
            }
        }
    }

    private Object jsonValueToObject(JSONArray values, PropertyType propertyDataType, int index) throws ParseException {
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
