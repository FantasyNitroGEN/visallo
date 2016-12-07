package org.visallo.core.model.workspace.product;

import org.json.JSONObject;
import org.visallo.core.util.JSONUtil;

import java.io.Serializable;
import java.util.Map;

public abstract class Product implements Serializable {
    static long serialVersionUID = 1L;
    private final String id;
    private final String workspaceId;
    private final String title;
    private final String kind;
    private final Map<String, Object> data;
    private final Map<String, Object> extendedData;
    private final String previewImageMD5;

    public Product(String id, String workspaceId, String kind, String title, JSONObject data, JSONObject extendedData, String md5) {
        this.id = id;
        this.workspaceId = workspaceId;
        this.kind = kind;
        this.data = data == null ? null : JSONUtil.toMap(data);
        this.extendedData = extendedData == null ? null : JSONUtil.toMap(extendedData);
        this.title = title;
        this.previewImageMD5 = md5;
    }

    public String getId() {
        return id;
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public String getKind() {
        return kind;
    }

    public String getTitle() {
        return title;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public Map<String, Object> getExtendedData() {
        return extendedData;
    }

    public String getPreviewImageMD5() {
        return previewImageMD5;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Product product = (Product) o;

        return id.equals(product.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Product{" +
                "title='" + getTitle() + '\'' +
                ", id='" + id + '\'' +
                ", workspaceId='" + workspaceId + '\'' +
                '}';
    }
}
