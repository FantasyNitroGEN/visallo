package org.visallo.web.routes.ontology;

import com.google.inject.Inject;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Optional;
import org.visallo.core.model.ontology.Concept;
import org.visallo.core.model.ontology.OntologyProperty;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.ontology.Relationship;
import org.visallo.web.clientapi.model.ClientApiOntology;
import org.visallo.web.parameterProviders.ActiveWorkspaceId;

import java.util.List;
import java.util.stream.Collectors;

public class OntologyGet extends OntologyBase {
    @Inject
    public OntologyGet(final OntologyRepository ontologyRepository) {
        super(ontologyRepository);
    }

    @Handle
    public ClientApiOntology handle(
            @Optional(name = "propertyIds[]") String[] propertyIds,
            @Optional(name = "conceptIds[]") String[] conceptIds,
            @Optional(name = "relationshipIds[]") String[] relationshipIds,
            @ActiveWorkspaceId String workspaceId
    ) throws Exception {
        ClientApiOntology clientApiOntology = new ClientApiOntology();

        List<Concept> concepts = ontologyIrisToConcepts(conceptIds, workspaceId);
        List<ClientApiOntology.Concept> clientConcepts = concepts.stream()
                .map(Concept::toClientApi)
                .collect(Collectors.toList());
        clientApiOntology.addAllConcepts(clientConcepts);

        List<Relationship> relationships = ontologyIrisToRelationships(relationshipIds, workspaceId);
        List<ClientApiOntology.Relationship> clientRelationships = relationships.stream()
                .map(Relationship::toClientApi)
                .collect(Collectors.toList());
        clientApiOntology.addAllRelationships(clientRelationships);

        List<OntologyProperty> properties = ontologyIrisToProperties(propertyIds, workspaceId);
        List<ClientApiOntology.Property> clientProperties = properties.stream()
                .map(OntologyProperty::toClientApi)
                .collect(Collectors.toList());
        clientApiOntology.addAllProperties(clientProperties);

        return clientApiOntology;
    }
}
