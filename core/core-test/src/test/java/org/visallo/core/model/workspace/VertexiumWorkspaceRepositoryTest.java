package org.visallo.core.model.workspace;

import org.visallo.core.model.workspace.product.WorkProduct;
import org.visallo.vertexium.model.workspace.VertexiumWorkspaceRepository;

import java.util.ArrayList;
import java.util.List;

public class VertexiumWorkspaceRepositoryTest extends WorkspaceRepositoryTestBase {
    private WorkspaceRepository workspaceRepository;

    @Override
    protected WorkspaceRepository getWorkspaceRepository() {
        if (workspaceRepository == null) {
            workspaceRepository = new VertexiumWorkspaceRepository(
                    getGraph(),
                    getConfiguration(),
                    getGraphRepository(),
                    getUserRepository(),
                    getGraphAuthorizationRepository(),
                    getWorkspaceDiffHelper(),
                    getLockRepository(),
                    getVisibilityTranslator(),
                    getTermMentionRepository(),
                    getOntologyRepository(),
                    getWorkQueueRepository(),
                    getAuthorizationRepository()
            );
            List<WorkProduct> workProducts = new ArrayList<>();
            workProducts.add(new MockWorkProduct());
            ((VertexiumWorkspaceRepository) workspaceRepository).setWorkProducts(workProducts);
        }
        return workspaceRepository;
    }
}
