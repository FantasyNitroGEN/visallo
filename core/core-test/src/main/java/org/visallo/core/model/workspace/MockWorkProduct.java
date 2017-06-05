package org.visallo.core.model.workspace;

import org.json.JSONObject;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.Vertex;
import org.vertexium.Visibility;
import org.visallo.core.model.graph.GraphUpdateContext;
import org.visallo.core.model.workspace.product.WorkProduct;
import org.visallo.core.user.User;

public class MockWorkProduct implements WorkProduct {

    @Override
    public JSONObject getExtendedData(
            Graph graph,
            Vertex workspaceVertex,
            Vertex productVertex,
            JSONObject params,
            User user,
            Authorizations authorizations
    ) {
        return new JSONObject();
    }
}
