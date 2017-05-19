package org.visallo.core.model.search;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.vertexium.Authorizations;
import org.vertexium.ElementType;
import org.vertexium.Graph;
import org.vertexium.Vertex;
import org.vertexium.query.CompositeGraphQuery;
import org.vertexium.query.Query;
import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.directory.DirectoryRepository;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class VertexiumObjectSearchRunnerWithRelatedBase extends VertexiumObjectSearchRunnerBase {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(VertexiumObjectSearchRunnerWithRelatedBase.class);

    protected VertexiumObjectSearchRunnerWithRelatedBase(
            OntologyRepository ontologyRepository,
            Graph graph,
            Configuration configuration,
            DirectoryRepository directoryRepository
    ) {
        super(ontologyRepository, graph, configuration, directoryRepository);
    }

    @Override
    protected QueryAndData getQuery(SearchOptions searchOptions, Authorizations authorizations) {
        JSONArray filterJson = getFilterJson(searchOptions);
        String queryString;

        String[] relatedToVertexIdsParam = searchOptions.getOptionalParameter("relatedToVertexIds[]", String[].class);
        String elementExtendedDataParam = searchOptions.getOptionalParameter("elementExtendedData", String.class);
        List<String> relatedToVertexIds;
        ElementExtendedData elementExtendedData;
        if (relatedToVertexIdsParam == null && elementExtendedDataParam == null) {
            queryString = searchOptions.getRequiredParameter("q", String.class);
            relatedToVertexIds = ImmutableList.of();
            elementExtendedData = null;
        } else if (elementExtendedDataParam != null) {
            queryString = searchOptions.getOptionalParameter("q", String.class);
            relatedToVertexIds = ImmutableList.of();
            elementExtendedData = ElementExtendedData.fromJsonString(elementExtendedDataParam);
        } else if (relatedToVertexIdsParam != null) {
            queryString = searchOptions.getOptionalParameter("q", String.class);
            relatedToVertexIds = ImmutableList.copyOf(relatedToVertexIdsParam);
            elementExtendedData = null;
        } else {
            throw new VisalloException("Unexpected state");
        }
        LOGGER.debug(
                "search %s (relatedToVertexIds: %s, elementExtendedData: %s)\n%s",
                queryString,
                Joiner.on(",").join(relatedToVertexIds),
                elementExtendedData,
                filterJson.toString(2)
        );

        Query graphQuery;
        if (relatedToVertexIds.isEmpty()) {
            graphQuery = query(queryString, null, elementExtendedData, authorizations);
        } else if (relatedToVertexIds.size() == 1) {
            graphQuery = query(queryString, relatedToVertexIds.get(0), null, authorizations);
        } else {
            graphQuery = new CompositeGraphQuery(getGraph(), Lists.transform(
                    relatedToVertexIds,
                    relatedToVertexId -> query(queryString, relatedToVertexId, null, authorizations)
            ));
        }

        return new QueryAndData(graphQuery);
    }

    private Query query(
            String query,
            String relatedToVertexId,
            ElementExtendedData elementExtendedData,
            Authorizations authorizations
    ) {
        Query graphQuery;
        if (relatedToVertexId == null && elementExtendedData == null) {
            graphQuery = getGraph().query(query, authorizations);
        } else if (elementExtendedData != null) {
            graphQuery = getGraph().query(query, authorizations)
                    .hasExtendedData(elementExtendedData.elementType, elementExtendedData.elementId, elementExtendedData.tableName);
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

    private static class ElementExtendedData {
        public final ElementType elementType;
        public final String elementId;
        public final String tableName;

        private ElementExtendedData(
                ElementType elementType,
                String elementId,
                String tableName
        ) {
            this.elementType = elementType;
            this.elementId = elementId;
            this.tableName = tableName;
        }

        public static ElementExtendedData fromJsonString(String str) {
            JSONObject json = new JSONObject(str);
            ElementType elementType = null;
            String elementTypeString = json.optString("elementType");
            if (!Strings.isNullOrEmpty(elementTypeString)) {
                elementType = ElementType.valueOf(elementTypeString.toUpperCase());
            }
            String elementId = json.optString("elementId", null);
            String tableName = json.optString("tableName", null);
            return new ElementExtendedData(elementType, elementId, tableName);
        }

        @Override
        public String toString() {
            return "ElementExtendedData{" +
                    "elementType='" + elementType + '\'' +
                    ", elementId='" + elementId + '\'' +
                    ", tableName='" + tableName + '\'' +
                    '}';
        }
    }
}
