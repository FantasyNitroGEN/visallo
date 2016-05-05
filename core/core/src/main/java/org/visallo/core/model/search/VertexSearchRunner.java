package org.visallo.core.model.search;

import com.google.inject.Inject;
import org.vertexium.ElementType;
import org.vertexium.Graph;
import org.visallo.core.config.Configuration;
import org.visallo.core.model.directory.DirectoryRepository;
import org.visallo.core.model.ontology.OntologyRepository;

import java.util.EnumSet;

public class VertexSearchRunner extends ElementSearchRunnerWithRelatedBase {
    public static final String URI = "/vertex/search";

    @Inject
    public VertexSearchRunner(
            OntologyRepository ontologyRepository,
            Graph graph,
            Configuration configuration,
            DirectoryRepository directoryRepository
    ) {
        super(ontologyRepository, graph, configuration, directoryRepository);
    }

    @Override
    protected EnumSet<ElementType> getResultType() {
        return EnumSet.of(ElementType.VERTEX);
    }

    @Override
    public String getUri() {
        return URI;
    }
}
