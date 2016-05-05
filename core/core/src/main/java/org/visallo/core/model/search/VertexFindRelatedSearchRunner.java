package org.visallo.core.model.search;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import org.vertexium.*;
import org.visallo.core.model.ontology.Concept;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.user.User;
import org.visallo.core.util.ClientApiConverter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class VertexFindRelatedSearchRunner extends SearchRunner {
    public static final String URI = "/vertex/find-related";
    private final OntologyRepository ontologyRepository;
    private final Graph graph;

    @Inject
    public VertexFindRelatedSearchRunner(
            Graph graph,
            OntologyRepository ontologyRepository
    ) {
        this.graph = graph;
        this.ontologyRepository = ontologyRepository;
    }

    @Override
    public String getUri() {
        return URI;
    }

    @Override
    public VertexFindRelatedSearchResults run(SearchOptions searchOptions, User user, Authorizations authorizations) {
        String[] graphVertexIds = searchOptions.getRequiredParameter("graphVertexIds[]", String[].class);
        String limitParentConceptId = searchOptions.getOptionalParameter("limitParentConceptId", String.class);
        String limitEdgeLabel = searchOptions.getOptionalParameter("limitEdgeLabel", String.class);
        long maxVerticesToReturn = searchOptions.getOptionalParameter("maxVerticesToReturn", 250L);

        Set<String> limitConceptIds = new HashSet<>();

        if (limitParentConceptId != null) {
            Set<Concept> limitConcepts = ontologyRepository.getConceptAndAllChildrenByIri(limitParentConceptId);
            if (limitConcepts == null) {
                throw new RuntimeException("Bad 'limitParentConceptId', no concept found for id: " +
                        limitParentConceptId);
            }
            for (Concept con : limitConcepts) {
                limitConceptIds.add(con.getIRI());
            }
        }

        return getSearchResults(
                searchOptions.getWorkspaceId(),
                graphVertexIds,
                limitEdgeLabel,
                limitConceptIds,
                maxVerticesToReturn,
                authorizations
        );
    }

    private VertexFindRelatedSearchResults getSearchResults(
            String workspaceId,
            String[] graphVertexIds,
            String limitEdgeLabel,
            Set<String> limitConceptIds,
            long maxVerticesToReturn,
            Authorizations authorizations
    ) {
        Set<String> visitedIds = new HashSet<>();
        long count = visitedIds.size();
        Iterable<Vertex> vertices = graph.getVertices(Lists.newArrayList(graphVertexIds), FetchHint.EDGE_REFS, authorizations);
        List<Vertex> elements = new ArrayList<>();
        for (Vertex v : vertices) {
            Iterable<Vertex> relatedVertices = v.getVertices(Direction.BOTH, limitEdgeLabel, ClientApiConverter.SEARCH_FETCH_HINTS, authorizations);
            for (Vertex vertex : relatedVertices) {
                if (!visitedIds.add(vertex.getId())) {
                    continue;
                }
                if (limitConceptIds.size() == 0 || !isLimited(vertex, limitConceptIds)) {
                    if (count < maxVerticesToReturn) {
                        elements.add(vertex);
                    }
                    count++;
                }
            }
        }
        return new VertexFindRelatedSearchResults(elements, count);
    }

    private boolean isLimited(Vertex vertex, Set<String> limitConceptIds) {
        String conceptId = VisalloProperties.CONCEPT_TYPE.getPropertyValue(vertex);
        return !limitConceptIds.contains(conceptId);
    }
}

