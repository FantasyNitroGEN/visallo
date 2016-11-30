package org.visallo.core.model.workspace;

import org.json.JSONObject;
import org.visallo.core.model.workspace.product.Product;
import org.visallo.core.user.User;
import org.visallo.web.clientapi.model.WorkspaceAccess;

import java.util.Collection;

public interface WorkspaceListener {
    void workspaceAdded(Workspace workspace, User user);

    void workspaceBeforeDelete(Workspace workspace, User user);

    void workspaceUpdateUser(Workspace workspace, String userId, WorkspaceAccess workspaceAccess, User user);

    void workspaceUpdateEntities(Workspace workspace, Collection<String> vertexIds, User user);

    void workspaceProductUpdated(Product product, JSONObject params, User user);

    void workspaceAddProduct(Product product, User user);

    void workspaceBeforeDeleteProduct(String workspaceId, String productId, User user);

    void workspaceDeleteUser(Workspace workspace, String userId, User user);
}
