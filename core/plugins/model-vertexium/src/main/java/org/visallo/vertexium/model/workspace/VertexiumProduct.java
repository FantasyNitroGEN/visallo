package org.visallo.vertexium.model.workspace;

import org.json.JSONObject;
import org.visallo.core.model.workspace.product.Product;

public class VertexiumProduct extends Product {
    private static final long serialVersionUID = -6351359711607833720L;

    public VertexiumProduct(String id, String workspaceId, String title, String kind, JSONObject data, JSONObject extendedData, String md5) {
        super(id, workspaceId, kind, title, data, extendedData, md5);
    }
}
