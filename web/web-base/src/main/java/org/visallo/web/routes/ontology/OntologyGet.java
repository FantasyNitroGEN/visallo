package org.visallo.web.routes.ontology;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Optional;
import org.vertexium.util.IterableUtils;
import org.visallo.core.model.ontology.*;
import org.visallo.web.clientapi.model.ClientApiOntology;
import org.visallo.web.parameterProviders.ActiveWorkspaceId;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class OntologyGet implements ParameterizedHandler {
    private final OntologyRepository ontologyRepository;

    @Inject
    public OntologyGet(final OntologyRepository ontologyRepository) {
        this.ontologyRepository = ontologyRepository;
    }

    @Handle
    public ClientApiOntology handle(
            @Optional(name = "propertyIds[]") String[] propertyIds,
            @Optional(name = "conceptIds[]") String[] conceptIds,
            @Optional(name = "relationshipIds[]") String[] relationshipIds,
            @ActiveWorkspaceId String workspaceId
    ) throws Exception {
        ClientApiOntology clientApiOntology = new ClientApiOntology();

        if (conceptIds != null) {
            Iterable<Concept> concepts = ontologyRepository.getConcepts(Lists.newArrayList(conceptIds), workspaceId);
            List<ClientApiOntology.Concept> clientConcepts = StreamSupport.stream(concepts.spliterator(), false)
                    .map(Concept::toClientApi)
                    .collect(Collectors.toList());
            clientApiOntology.addAllConcepts(clientConcepts);
        }

        if (relationshipIds != null) {
            Iterable<Relationship> relationships = ontologyRepository.getRelationships(Lists.newArrayList(relationshipIds), workspaceId);
            List<ClientApiOntology.Relationship> clientRelationships = StreamSupport.stream(relationships.spliterator(), false)
                    .map(Relationship::toClientApi)
                    .collect(Collectors.toList());
            clientApiOntology.addAllRelationships(clientRelationships);
        }

        if (propertyIds != null) {
            Iterable<OntologyProperty> properties = ontologyRepository.getProperties(Lists.newArrayList(propertyIds), workspaceId);
            List<ClientApiOntology.Property> clientProperties = StreamSupport.stream(properties.spliterator(), false)
                    .map(OntologyProperty::toClientApi)
                    .collect(Collectors.toList());
            clientApiOntology.addAllProperties(clientProperties);
        }

        return clientApiOntology;
    }
}
