package org.visallo.web.clientapi.model;

import java.util.ArrayList;
import java.util.List;

public class ClientApiEdgeSearchResponse extends ClientApiSearchResponse {
    private List<ClientApiEdge> results = new ArrayList<>();

    public List<ClientApiEdge> getResults() {
        return results;
    }

    @Override
    public int getItemCount() {
        return getResults().size();
    }
}
