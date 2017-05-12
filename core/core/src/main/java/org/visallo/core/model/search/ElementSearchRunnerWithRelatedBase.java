package org.visallo.core.model.search;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.Vertex;
import org.vertexium.query.CompositeGraphQuery;
import org.vertexium.query.Query;
import org.visallo.core.config.Configuration;
import org.visallo.core.model.directory.DirectoryRepository;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class ElementSearchRunnerWithRelatedBase extends ElementSearchRunnerBase {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(ElementSearchRunnerWithRelatedBase.class);

    protected ElementSearchRunnerWithRelatedBase(
            OntologyRepository ontologyRepository,
            Graph graph,
            Configuration configuration,
            DirectoryRepository directoryRepository
    ) {
        super(ontologyRepository, graph, configuration, directoryRepository);
    }

    @Override
    protected QueryAndData getQuery(SearchOptions searchOptions, Authorizations authorizations) {
        final String[] relatedToVertexIdsParam = searchOptions.getOptionalParameter("relatedToVertexIds[]", String[].class);
        final JSONArray filterJson = getFilterJson(searchOptions);
        final List<String> relatedToVertexIds;
        final String queryString;
        if (relatedToVertexIdsParam == null) {
            queryString = searchOptions.getRequiredParameter("q", String.class);
            relatedToVertexIds = ImmutableList.of();
        } else {
            queryString = searchOptions.getOptionalParameter("q", String.class);
            relatedToVertexIds = ImmutableList.copyOf(relatedToVertexIdsParam);
        }
        LOGGER.debug("search %s (relatedToVertexIds: %s)\n%s", queryString, Joiner.on(",").join(relatedToVertexIds), filterJson.toString(2));

        Query graphQuery;
        if (relatedToVertexIds.isEmpty()) {
            graphQuery = query(queryString, null, authorizations);
        } else if (relatedToVertexIds.size() == 1) {
            graphQuery = query(queryString, relatedToVertexIds.get(0), authorizations);
        } else {
            graphQuery = new CompositeGraphQuery(getGraph(), Lists.transform(
                    relatedToVertexIds,
                    relatedToVertexId -> query(queryString, relatedToVertexId, authorizations)
            ));
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
