package org.visallo.web.routes.product;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Required;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.user.User;
import org.visallo.web.VisalloResponse;
import org.visallo.web.clientapi.model.ClientApiSuccess;
import org.visallo.web.parameterProviders.ActiveWorkspaceId;

public class ProductDelete implements ParameterizedHandler {
    private final WorkspaceRepository workspaceRepository;

    @Inject
    public ProductDelete(WorkspaceRepository workspaceRepository) {
        this.workspaceRepository = workspaceRepository;
    }

    @Handle
    public ClientApiSuccess handle(
            @Required(name = "productId") String productId,
            @ActiveWorkspaceId String workspaceId,
            User user
    ) throws Exception {
        workspaceRepository.deleteProduct(workspaceId, productId, user);
        return VisalloResponse.SUCCESS;
    }
}
