package org.visallo.core.model.workspace.product;

import org.json.JSONObject;
import org.vertexium.*;
import org.visallo.core.model.graph.ElementUpdateContext;
import org.visallo.core.user.User;

public interface WorkProduct {
    void update(JSONObject params, Graph graph, Vertex workspaceVertex, ElementUpdateContext<Vertex> vertexBuilder, User user, Visibility visibility, Authorizations authorizations);

    JSONObject get(JSONObject params, Graph graph, Vertex vertex, Vertex productVertex, User user, Authorizations authorizations);

}
