package org.visallo.vertexium.es;

import org.vertexium.Element;
import org.vertexium.ExtendedDataRowId;
import org.vertexium.GraphConfiguration;
import org.vertexium.elasticsearch.ElasticsearchSingleDocumentSearchIndex;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workspace.WorkspaceRepository;

@Deprecated
public class SimpleVisalloIndexSelectionStrategy extends IriIndexSelectionStrategyBase {
    public SimpleVisalloIndexSelectionStrategy(GraphConfiguration config) {
        super(config);
    }

    @Override
    protected String getIndexNameForEdgeLabel(String edgeLabel) {
        return encodeIndexName("edge");
    }

    @Override
    protected String getIndexNameForConceptType(String conceptType) {
        if (conceptType.equals(UserRepository.USER_CONCEPT_IRI)) {
            return encodeIndexName("user");
        }
        if (conceptType.equals(WorkspaceRepository.WORKSPACE_CONCEPT_IRI)) {
            return encodeIndexName("workspace");
        }
        return encodeIndexName("vertex");
    }

    @Override
    public String getExtendedDataIndexName(ElasticsearchSingleDocumentSearchIndex es, Element element, String tableName, String rowId) {
        return encodeIndexName("extdata_" + tableName);
    }

    @Override
    public String getExtendedDataIndexName(ElasticsearchSingleDocumentSearchIndex es, ExtendedDataRowId rowId) {
        return encodeIndexName("extdata_" + rowId.getTableName());
    }
}
