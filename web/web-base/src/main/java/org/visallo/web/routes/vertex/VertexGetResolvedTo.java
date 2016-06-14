package org.visallo.web.routes.vertex;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Optional;
import com.v5analytics.webster.annotations.Required;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.Property;
import org.vertexium.Vertex;
import org.visallo.core.exception.VisalloResourceNotFoundException;
import org.visallo.core.model.termMention.TermMentionRepository;
import org.visallo.core.util.ClientApiConverter;
import org.visallo.web.clientapi.model.ClientApiTermMentionsResponse;
import org.visallo.web.parameterProviders.ActiveWorkspaceId;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public class VertexGetResolvedTo implements ParameterizedHandler {
    private final Graph graph;
    private final TermMentionRepository termMentionRepository;

    @Inject
    public VertexGetResolvedTo(
            Graph graph,
            TermMentionRepository termMentionRepository
    ) {
        this.graph = graph;
        this.termMentionRepository = termMentionRepository;
    }

    @Handle
    public ClientApiTermMentionsResponse handle(
            @Required(name = "graphVertexId") String graphVertexId,
            @Optional(name = "propertyKey") String propertyKey,
            @Optional(name = "propertyName") String propertyName,
            @ActiveWorkspaceId String workspaceId,
            Authorizations authorizations
    ) throws Exception {
        Vertex vertex = graph.getVertex(graphVertexId, authorizations);
        if (vertex == null) {
            throw new VisalloResourceNotFoundException(String.format("vertex %s not found", graphVertexId));
        }

        Stream<Vertex> termMentions;
        if (propertyKey != null || propertyName != null) {
            Property property = vertex.getProperty(propertyKey, propertyName);
            if (property == null) {
                throw new VisalloResourceNotFoundException(String.format(
                        "property %s:%s not found on vertex %s",
                        propertyKey,
                        propertyName,
                        vertex.getId()
                ));
            }
            termMentions = termMentionRepository.findResolvedToForRef(
                    graphVertexId,
                    propertyKey,
                    propertyName,
                    authorizations
            );
        } else {
            termMentions = termMentionRepository.findResolvedToForRefElement(graphVertexId, authorizations);
        }

        return ClientApiConverter.toTermMentionsResponse(
                termMentions.collect(Collectors.toList()),
                workspaceId,
                authorizations
        );
    }
}
