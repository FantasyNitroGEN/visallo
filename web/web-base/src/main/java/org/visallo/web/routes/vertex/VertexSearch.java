package org.visallo.web.routes.vertex;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import org.visallo.core.config.Configuration;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.query.CompositeGraphQuery;
import org.vertexium.query.Query;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public class VertexSearch extends VertexSearchBase {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(VertexSearch.class);

    @Inject
    public VertexSearch(
            OntologyRepository ontologyRepository,
            Graph graph,
            UserRepository userRepository,
            Configuration configuration,
            WorkspaceRepository workspaceRepository
    ) {
        super(ontologyRepository, graph, userRepository, configuration, workspaceRepository);
    }

    @Override
    protected QueryAndData getQuery(HttpServletRequest request, final Authorizations authorizations) {
        final String[] relatedToVertexIdsParam = getOptionalParameterArray(request, "relatedToVertexIds[]");
        final JSONArray filterJson = getFilterJson(request);
        final List<String> relatedToVertexIds;
        final String queryString;
        if (relatedToVertexIdsParam == null) {
            queryString = getRequiredParameter(request, "q");
            relatedToVertexIds = ImmutableList.of();
        } else {
            queryString = getOptionalParameter(request, "q");
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
            graphQuery = getGraph().getVertex(relatedToVertexId, authorizations).query(authorizations);
        } else {
            graphQuery = getGraph().getVertex(relatedToVertexId, authorizations).query(query, authorizations);
        }
        return graphQuery;
    }
}
