package org.visallo.vertexium.model.workspace;

import org.visallo.core.model.workspace.product.Product;

import java.io.InputStream;

public class VertexiumProduct extends Product {

    public VertexiumProduct(String id, String workspaceId, String title, String kind, String data, String extendedData, InputStream previewDataUrl, String md5) {
        super(id, workspaceId, kind, title, data, extendedData, previewDataUrl, md5);
    }

}
