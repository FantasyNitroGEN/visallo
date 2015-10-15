package org.visallo.web.clientapi.model;

import java.util.ArrayList;
import java.util.List;

public class ClientApiWorkspaceEdges implements ClientApiObject {
    public List<ClientApiWorkspaceEdge> edges = new ArrayList<ClientApiWorkspaceEdge>();

    public void add(String edgeId, String label, String outVertexId, String inVertexId) {
        edges.add(new ClientApiWorkspaceEdge(edgeId, label, outVertexId, inVertexId));
    }
}
