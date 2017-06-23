package org.visallo.web.routes.ontology;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Optional;
import com.v5analytics.webster.annotations.Required;
import org.vertexium.TextIndexHint;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.ontology.*;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.user.User;
import org.visallo.web.clientapi.model.ClientApiOntology;
import org.visallo.web.clientapi.model.PropertyType;
import org.visallo.web.parameterProviders.ActiveWorkspaceId;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class OntologyPropertySave implements ParameterizedHandler {
    private final OntologyRepository ontologyRepository;
    private final WorkQueueRepository workQueueRepository;

    @Inject
    public OntologyPropertySave(
            final OntologyRepository ontologyRepository,
            final WorkQueueRepository workQueueRepository
    ) {
        this.ontologyRepository = ontologyRepository;
        this.workQueueRepository = workQueueRepository;
    }

    @Handle
    public ClientApiOntology.Property handle(
            @Required(name = "displayName", allowEmpty = false) String displayName,
            @Required(name = "dataType", allowEmpty = false) String dataType,
            @Optional(name = "propertyIri", allowEmpty = false) String propertyIri,
            @Optional(name = "conceptIris[]") String[] conceptIris,
            @Optional(name = "relationshipIris[]") String[] relationshipIris,
            @ActiveWorkspaceId String workspaceId,
            User user) throws Exception {

        List<Concept> concepts = ontologyIrisToObjects(conceptIris, ontologyRepository::getConceptByIRI, "concept", workspaceId);
        List<Relationship> relationships = ontologyIrisToObjects(relationshipIris, ontologyRepository::getRelationshipByIRI, "relationship", workspaceId);

        PropertyType type = convertDataTypeStringToPropertyType(dataType);

        if (propertyIri == null) {
            propertyIri = ontologyRepository.generateDynamicIri(OntologyProperty.class, displayName, workspaceId);
        }

        OntologyPropertyDefinition def = new OntologyPropertyDefinition(concepts, relationships, propertyIri, displayName, type);
        def.setAddable(true);
        def.setDeleteable(true);
        def.setSearchable(true);
        def.setSortable(true);
        def.setUserVisible(true);
        def.setUpdateable(true);
        if (type.equals(PropertyType.STRING)) {
            def.setTextIndexHints(TextIndexHint.ALL);
        }

        OntologyProperty property = ontologyRepository.getOrCreateProperty(def, user, workspaceId);

        ontologyRepository.clearCache(workspaceId);
        workQueueRepository.pushOntologyPropertiesChange(workspaceId, property.getId());

        return property.toClientApi();
    }

    private <T> List<T> ontologyIrisToObjects(
            String[] iris,
            BiFunction<String, String, T> getter,
            String ontologyObjectType,
            String workspaceId
    ) {
        return iris == null ? new ArrayList<>() : Arrays.stream(iris).map(iri -> {
                T ontologyObject = getter.apply(iri, workspaceId);
                if (ontologyObject == null) {
                    throw new VisalloException("Unable to load " + ontologyObjectType + " with IRI: " + iri);
                }
                return ontologyObject;
            })
            .collect(Collectors.toList());
    }

    private PropertyType convertDataTypeStringToPropertyType(String dataType) {
        boolean isValid = Arrays.stream(PropertyType.values())
                .anyMatch(pt -> pt.toString().toLowerCase().equals(dataType.toLowerCase()));
        if (!isValid) {
            throw new VisalloException("Unknown property type: " + dataType);
        }

        return PropertyType.valueOf(dataType.toUpperCase());
    }
}
