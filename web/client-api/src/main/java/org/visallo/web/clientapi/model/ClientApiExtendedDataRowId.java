package org.visallo.web.clientapi.model;

public class ClientApiExtendedDataRowId implements ClientApiObject {
    private String elementType;
    private String elementId;
    private String tableName;
    private String rowId;

    public ClientApiExtendedDataRowId() {
    }

    public ClientApiExtendedDataRowId(String elementType, String elementId, String tableName, String rowId) {
        this.elementType = elementType;
        this.elementId = elementId;
        this.tableName = tableName;
        this.rowId = rowId;
    }

    public String getElementType() {
        return elementType;
    }

    public String getElementId() {
        return elementId;
    }

    public String getTableName() {
        return tableName;
    }

    public String getRowId() {
        return rowId;
    }
}
