package org.visallo.web.routes.edge;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Required;
import org.vertexium.Authorizations;
import org.vertexium.Edge;
import org.vertexium.Graph;
import org.visallo.core.exception.VisalloResourceNotFoundException;
import org.visallo.core.model.termMention.TermMentionRepository;
import org.visallo.web.clientapi.model.ClientApiEdgeDetails;
import org.visallo.web.clientapi.model.ClientApiSourceInfo;

public class EdgeDetails implements ParameterizedHandler {
    private final Graph graph;
    private final TermMentionRepository termMentionRepository;

    @Inject
    public EdgeDetails(
            Graph graph,
            TermMentionRepository termMentionRepository
    ) {
        this.graph = graph;
        this.termMentionRepository = termMentionRepository;
    }

    @Handle
    public ClientApiEdgeDetails handle(
            @Required(name = "edgeId") String edgeId,
            Authorizations authorizations
    ) throws Exception {
        Edge edge = this.graph.getEdge(edgeId, authorizations);
        if (edge == null) {
            throw new VisalloResourceNotFoundException("Could not find edge with id: " + edgeId, edgeId);
        }

        ClientApiSourceInfo sourceInfo = termMentionRepository.getSourceInfoForEdge(edge, authorizations);

        ClientApiEdgeDetails result = new ClientApiEdgeDetails();
        result.sourceInfo = sourceInfo;
        return result;
    }
}
