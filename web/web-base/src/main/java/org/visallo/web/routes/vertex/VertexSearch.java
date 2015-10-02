package org.visallo.web.routes.vertex;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.Vertex;
import org.vertexium.query.CompositeGraphQuery;
import org.vertexium.query.Query;
import org.visallo.core.config.Configuration;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.clientapi.model.ClientApiVertexSearchResponse;
import org.visallo.web.parameterProviders.ActiveWorkspaceId;
import org.visallo.web.parameterProviders.VisalloBaseParameterProvider;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class VertexSearch extends VertexSearchBase implements ParameterizedHandler {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(VertexSearch.class);

    @Inject
    public VertexSearch(
            OntologyRepository ontologyRepository,
            Graph graph,
            Configuration configuration
    ) {
        super(ontologyRepository, graph, configuration);
    }

    @Override
    @Handle
    public ClientApiVertexSearchResponse handle(
            HttpServletRequest request,
            @ActiveWorkspaceId String workspaceId,
            Authorizations authorizations
    ) throws Exception {
        return super.handle(request, workspaceId, authorizations);
    }

    @Override
    protected QueryAndData getQuery(HttpServletRequest request, final Authorizations authorizations) {
        final String[] relatedToVertexIdsParam = VisalloBaseParameterProvider.getOptionalParameterArray(request, "relatedToVertexIds[]");
        final JSONArray filterJson = getFilterJson(request);
        final List<String> relatedToVertexIds;
        final String queryString;
        if (relatedToVertexIdsParam == null) {
            queryString = VisalloBaseParameterProvider.getRequiredParameter(request, "q");
            relatedToVertexIds = ImmutableList.of();
        } else {
            queryString = VisalloBaseParameterProvider.getOptionalParameter(request, "q");
            relatedToVertexIds = ImmutableList.copyOf(relatedToVertexIdsParam);
        }
        LOGGER.debug("search %s\n%s", queryString, filterJson.toString(2));

        Query graphQuery;
        if (relatedToVertexIds == null || relatedToVertexIds.isEmpty()) {
            graphQuery = query(queryString, null, authorizations);
        } else if (relatedToVertexIds.size() == 1) {
            graphQuery = query(queryString, relatedToVertexIds.get(0), authorizations);
        } else {
            graphQuery = new CompositeGraphQuery(Lists.transform(relatedToVertexIds, new Function<String, Query>() {
                @Override
                public Query apply(String relatedToVertexId) {
                    return query(queryString, relatedToVertexId, authorizations);
                }
            }));
        }

        return new QueryAndData(graphQuery);
    }

    private Query query(String query, String relatedToVertexId, Authorizations authorizations) {
        Query graphQuery;
        if (relatedToVertexId == null) {
            graphQuery = getGraph().query(query, authorizations);
        } else if (StringUtils.isBlank(query)) {
            Vertex relatedToVertex = getGraph().getVertex(relatedToVertexId, authorizations);
            checkNotNull(relatedToVertex, "Could not find vertex: " + relatedToVertexId);
            graphQuery = relatedToVertex.query(authorizations);
        } else {
            Vertex relatedToVertex = getGraph().getVertex(relatedToVertexId, authorizations);
            checkNotNull(relatedToVertex, "Could not find vertex: " + relatedToVertexId);
            graphQuery = relatedToVertex.query(query, authorizations);
        }
        return graphQuery;
    }
}
