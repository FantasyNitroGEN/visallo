package org.visallo.vertexium.es;

import org.elasticsearch.action.admin.indices.stats.IndexStats;
import org.vertexium.*;
import org.vertexium.elasticsearch.ElasticSearchElementType;
import org.vertexium.elasticsearch.ElasticSearchQueryBase;
import org.vertexium.elasticsearch.ElasticSearchSearchIndexBase;
import org.vertexium.elasticsearch.IndexSelectionStrategy;
import org.vertexium.query.QueryBase;
import org.visallo.core.model.properties.VisalloProperties;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class IriIndexSelectionStrategyBase implements IndexSelectionStrategy {
    public static final String INDEX_NAME_PREFIX = "indexNamePrefix";
    public static final String DEFAULT_INDEX_NAME_PREFIX = "visallo_";
    private static final String DEFAULT_INDEX_SUFFIX = "default";
    private final String indexPrefix;
    private String[] indiciesToQuery;
    private Map<String, String> iriToIndexNameCache = new HashMap<>();

    public IriIndexSelectionStrategyBase(GraphConfiguration config) {
        indexPrefix = config.getString(GraphConfiguration.SEARCH_INDEX_PROP_PREFIX + "." + INDEX_NAME_PREFIX, DEFAULT_INDEX_NAME_PREFIX);
        indiciesToQuery = new String[]{
                indexPrefix + "*"
        };
    }

    @Override
    public String[] getIndicesToQuery(ElasticSearchSearchIndexBase elasticSearchSearchIndexBase) {
        return indiciesToQuery;
    }

    @Override
    public String getIndexName(ElasticSearchSearchIndexBase elasticSearchSearchIndexBase, Element element) {
        if (element instanceof Vertex) {
            String conceptType = VisalloProperties.CONCEPT_TYPE.getPropertyValue(element);
            if (conceptType != null) {
                return getIndexNameForConceptType(conceptType);
            }
        } else if (element instanceof Edge) {
            String edgeLabel = ((Edge) element).getLabel();
            return getIndexNameForEdgeLabel(edgeLabel);
        }
        return encodeIndexName(DEFAULT_INDEX_SUFFIX);
    }

    protected abstract String getIndexNameForEdgeLabel(String edgeLabel);

    protected abstract String getIndexNameForConceptType(String conceptType);

    protected String encodeIndexName(String string) {
        String indexName = iriToIndexNameCache.get(string);
        if (indexName != null) {
            return indexName;
        }
        indexName = indexPrefix + string.replaceAll("\\W", "_").toLowerCase();
        iriToIndexNameCache.put(string, indexName);
        return indexName;
    }

    @Override
    public String[] getIndexNames(ElasticSearchSearchIndexBase es, PropertyDefinition propertyDefinition) {
        return getManagedIndexNames(es);
    }

    @Override
    public boolean isIncluded(ElasticSearchSearchIndexBase es, String indexName) {
        return indexPrefix.startsWith(indexPrefix);
    }

    @Override
    public String[] getManagedIndexNames(ElasticSearchSearchIndexBase es) {
        Map<String, IndexStats> indices = es.getClient().admin().indices().prepareStats().execute().actionGet().getIndices();
        Set<String> indexNames = indices.keySet();
        return indexNames.toArray(new String[indexNames.size()]);
    }

    @Override
    public String[] getIndicesToQuery(ElasticSearchQueryBase query, ElasticSearchElementType elementType) {
        for (QueryBase.HasContainer hasContainer : query.getParameters().getHasContainers()) {
            if (hasContainer instanceof QueryBase.HasValueContainer) {
                QueryBase.HasValueContainer hasValueContainer = (QueryBase.HasValueContainer) hasContainer;
                if (hasValueContainer.key.equals(VisalloProperties.CONCEPT_TYPE.getPropertyName())) {
                    Object value = hasValueContainer.value;
                    if (value instanceof String) {
                        String conceptType = (String) value;
                        return new String[]{
                                getIndexNameForConceptType(conceptType)
                        };
                    }

                    if (value instanceof String[]) {
                        String[] conceptTypes = (String[]) value;
                        String[] indexNames = new String[conceptTypes.length];
                        for (int i = 0; i < conceptTypes.length; i++) {
                            indexNames[i] = getIndexNameForConceptType(conceptTypes[i]);
                        }
                        return indexNames;
                    }
                }
            }
        }

        return indiciesToQuery;
    }
}
