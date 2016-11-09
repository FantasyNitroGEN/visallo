package org.visallo.core.model.workspace.product;

import java.io.Serializable;

public abstract class Product implements Serializable {
    static long serialVersionUID = 1L;
    private final String id;
    private final String workspaceId;
    private final String title;
    private final String kind;
    private final String data;
    private final String extendedData;
    private final String previewImageMD5;

    public Product(String id, String workspaceId, String kind, String title, String data, String extendedData, String md5) {
        this.id = id;
        this.workspaceId = workspaceId;
        this.kind = kind;
        this.data = data;
        this.extendedData = extendedData;
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

    public String getData() {
        return data;
    }

    public String getTitle() {
        return title;
    }

    public String getExtendedData() {
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
