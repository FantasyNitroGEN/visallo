package org.visallo.web.clientapi.model;

import java.util.ArrayList;
import java.util.List;

public class ClientApiScalarSearchResponse extends ClientApiSearchResponse {
    private List<Object> results = new ArrayList<Object>();

    public List<Object> getResults() {
        return results;
    }

    @Override
    public int getItemCount() {
        return getResults().size();
    }
}
