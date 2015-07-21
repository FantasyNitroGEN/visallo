package org.visallo.vertexium.es;

import org.vertexium.GraphConfiguration;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workspace.WorkspaceRepository;

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
}
