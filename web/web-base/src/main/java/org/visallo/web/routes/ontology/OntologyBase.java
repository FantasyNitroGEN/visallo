package org.visallo.web.routes.ontology;

import com.v5analytics.webster.ParameterizedHandler;
import org.vertexium.util.IterableUtils;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.ontology.Concept;
import org.visallo.core.model.ontology.OntologyProperty;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.ontology.Relationship;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class OntologyBase implements ParameterizedHandler {
    private final OntologyRepository ontologyRepository;

    public OntologyBase(OntologyRepository ontologyRepository) {
        this.ontologyRepository = ontologyRepository;
    }

    protected List<Concept> ontologyIrisToConcepts(String[] iris, String workspaceId) {
        return ontologyIrisToObjects(iris, ontologyRepository::getConcepts, Concept::getIRI, "concept", workspaceId);
    }

    protected List<Relationship> ontologyIrisToRelationships(String[] iris, String workspaceId) {
        return ontologyIrisToObjects(iris, ontologyRepository::getRelationships, Relationship::getIRI, "relationship", workspaceId);
    }

    protected List<OntologyProperty> ontologyIrisToProperties(String[] iris, String workspaceId) {
        return ontologyIrisToObjects(iris, ontologyRepository::getProperties, OntologyProperty::getId, "property", workspaceId);
    }

    protected <T> List<T> ontologyIrisToObjects(
            String[] iris,
            BiFunction<Iterable<String>, String, Iterable<T>> getAllByIriFunction,
            Function<T, String> getIriFunction,
            String ontologyObjectType,
            String workspaceId
    ) {
        if (iris == null) {
            return new ArrayList<>();
        }

        List<T> ontologyObjects = IterableUtils.toList(getAllByIriFunction.apply(Arrays.asList(iris), workspaceId));
        if (ontologyObjects.size() != iris.length) {
            List<String> foundIris = ontologyObjects.stream().map(getIriFunction).collect(Collectors.toList());
            String missingIris = Arrays.stream(iris).filter(iri -> !foundIris.contains(iri)).collect(Collectors.joining(", "));
            throw new VisalloException("Unable to load " + ontologyObjectType + " with IRI: " + missingIris);
        }
        return ontologyObjects;
    }
}
