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
            List<ClientApiOntology.Concept> concepts = IterableUtils.toList(ontologyRepository.getConcepts(Lists.newArrayList(conceptIds), workspaceId))
                    .stream()
                    .map(Concept::toClientApi)
                    .collect(Collectors.toList());
            clientApiOntology.addAllConcepts(concepts);
        }

        if (relationshipIds != null) {
            List<ClientApiOntology.Relationship> relationships = IterableUtils.toList(ontologyRepository.getRelationships(Lists.newArrayList(relationshipIds), workspaceId))
                    .stream()
                    .map(Relationship::toClientApi)
                    .collect(Collectors.toList());
            clientApiOntology.addAllRelationships(relationships);
        }

        if (propertyIds != null) {
            List<ClientApiOntology.Property> properties = IterableUtils.toList(ontologyRepository.getProperties(Lists.newArrayList(propertyIds), workspaceId))
                    .stream()
                    .map(OntologyProperty::toClientApi)
                    .collect(Collectors.toList());
            clientApiOntology.addAllProperties(properties);
        }

        return clientApiOntology;
    }
}
