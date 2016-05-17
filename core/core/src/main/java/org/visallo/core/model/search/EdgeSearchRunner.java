package org.visallo.core.model.search;

import com.google.inject.Inject;
import org.vertexium.ElementType;
import org.vertexium.Graph;
import org.visallo.core.config.Configuration;
import org.visallo.core.model.directory.DirectoryRepository;
import org.visallo.core.model.ontology.OntologyRepository;

import java.util.EnumSet;

public class EdgeSearchRunner extends ElementSearchRunnerWithRelatedBase {
    public static final String URI = "/edge/search";

    @Inject
    public EdgeSearchRunner(
            OntologyRepository ontologyRepository,
            Graph graph,
            Configuration configuration,
            DirectoryRepository directoryRepository
    ) {
        super(ontologyRepository, graph, configuration, directoryRepository);
    }

    @Override
    protected EnumSet<ElementType> getResultType() {
        return EnumSet.of(ElementType.EDGE);
    }

    @Override
    public String getUri() {
        return URI;
    }
}
