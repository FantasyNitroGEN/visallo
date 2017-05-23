package org.visallo.web.clientapi.model;

import java.util.HashMap;
import java.util.Map;

public class ClientApiExtendedDataRow extends ClientApiVertexiumObject {
    private ClientApiExtendedDataRowId id;

    public ClientApiExtendedDataRow() {

    }

    public ClientApiExtendedDataRow(ClientApiExtendedDataRowId id) {
        this.id = id;
    }

    public ClientApiExtendedDataRowId getId() {
        return id;
    }
}
