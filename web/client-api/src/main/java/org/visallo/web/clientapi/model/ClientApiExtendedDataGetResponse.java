package org.visallo.web.clientapi.model;

import java.util.ArrayList;
import java.util.List;

public class ClientApiExtendedDataGetResponse implements ClientApiObject {
    private final List<ClientApiExtendedDataRow> rows;

    public ClientApiExtendedDataGetResponse() {
        this.rows = new ArrayList<ClientApiExtendedDataRow>();
    }

    public ClientApiExtendedDataGetResponse(List<ClientApiExtendedDataRow> rows) {
        this.rows = rows;
    }

    public List<ClientApiExtendedDataRow> getRows() {
        return rows;
    }
}
