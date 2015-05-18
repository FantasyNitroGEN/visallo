package org.visallo.web.clientapi.model;

import java.util.ArrayList;
import java.util.List;

public class ClientApiVertexSearchResponse extends ClientApiSearchResponse {
    private List<ClientApiVertex> vertices = new ArrayList<>();

    public List<ClientApiVertex> getVertices() {
        return vertices;
    }

    @Override
    public int getItemCount() {
        return getVertices().size();
    }
}
