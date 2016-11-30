package org.visallo.core.model.workspace;

import org.json.JSONObject;
import org.visallo.core.model.workspace.product.Product;
import org.visallo.core.user.User;
import org.visallo.web.clientapi.model.WorkspaceAccess;

import java.util.Collection;

public class DefaultWorkspaceListener implements WorkspaceListener {
    @Override
    public void workspaceAdded(Workspace workspace, User user) {

    }

    @Override
    public void workspaceBeforeDelete(Workspace workspace, User user) {

    }

    @Override
    public void workspaceUpdateUser(Workspace workspace, String userId, WorkspaceAccess workspaceAccess, User user) {

    }

    @Override
    public void workspaceUpdateEntities(Workspace workspace, Collection<String> vertexIds, User user) {

    }

    @Override
    public void workspaceProductUpdated(Product product, JSONObject params, User user) {

    }

    @Override
    public void workspaceAddProduct(Product product, User user) {

    }

    @Override
    public void workspaceBeforeDeleteProduct(String workspaceId, String productId, User user) {

    }

    @Override
    public void workspaceDeleteUser(Workspace workspace, String userId, User user) {

    }
}
