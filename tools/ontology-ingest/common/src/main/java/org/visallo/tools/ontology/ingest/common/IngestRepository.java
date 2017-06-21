package org.visallo.tools.ontology.ingest.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.inject.Inject;
import org.vertexium.*;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.graph.GraphRepository;
import org.visallo.core.model.ontology.*;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.user.AuthorizationRepository;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.security.VisibilityTranslator;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.clientapi.model.PropertyType;
import org.visallo.web.clientapi.model.VisibilityJson;

import java.math.BigDecimal;
import java.util.*;

public class IngestRepository {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(IngestRepository.class);
    private final Graph graph;
    private final AuthorizationRepository authorizationRepository;
    private final VisibilityTranslator visibilityTranslator;
    private final OntologyRepository ontologyRepository;

    private final Set<Class> verifiedClasses = new HashSet<>();
    private final Set<String> verifiedRelationshipConcepts = new HashSet<>();
    private final Set<String> verifiedClassProperties = new HashSet<>();

    private IngestOptions defaultIngestOptions;

    @Inject
    public IngestRepository(
            Graph graph,
            UserRepository userRepository,
            AuthorizationRepository authorizationRepository,
            VisibilityTranslator visibilityTranslator,
            OntologyRepository ontologyRepository
    ) throws JsonProcessingException {
        this.graph = graph;
        this.authorizationRepository = authorizationRepository;
        this.visibilityTranslator = visibilityTranslator;
        this.ontologyRepository = ontologyRepository;
        defaultIngestOptions = new IngestOptions(userRepository.getSystemUser());
    }

    public List<Element> save(EntityBuilder... builders) {
        return save(defaultIngestOptions, Arrays.asList(builders));
    }

    public List<Element> save(Collection<EntityBuilder> builders) {
        return save(defaultIngestOptions, builders);
    }

    public List<Element> save(IngestOptions ingestOptions, EntityBuilder... builders) {
        return save(ingestOptions, Arrays.asList(builders));
    }

    public List<Element> save(IngestOptions ingestOptions, Collection<EntityBuilder> builders) {
        LOGGER.debug("Saving %d entities", builders.size());

        // Validate the entities
        if (ingestOptions.isValidateOntologyWhenSaving()) {
            for (EntityBuilder builder : builders) {
                ValidationResult validationResult = (builder instanceof ConceptBuilder) ?
                        validateConceptBuilder((ConceptBuilder) builder) :
                        validateRelationshipBuilder((RelationshipBuilder) builder);
                if (!validationResult.isValid()) {
                    throw new VisalloException(validationResult.getValidationError());
                }
            }
        }

        // If we got to here, everything must be valid. Save the entities.
        List<Element> elements = new ArrayList<>();
        for (EntityBuilder builder : builders) {
            if (builder instanceof ConceptBuilder) {
                elements.add(save(ingestOptions, (ConceptBuilder) builder));
            } else {
                elements.add(save(ingestOptions, (RelationshipBuilder) builder));
            }
        }
        return elements;
    }

    public void flush() {
        graph.flush();
    }

    private Vertex save(IngestOptions ingestOptions, ConceptBuilder conceptBuilder) {
        Visibility conceptVisibility = getVisibility(ingestOptions, conceptBuilder.getVisibility());
        VertexBuilder vertexBuilder = graph.prepareVertex(
                conceptBuilder.getId(),
                getTimestamp(ingestOptions, conceptBuilder.getTimestamp()),
                conceptVisibility
        );
        vertexBuilder.setProperty(
                VisalloProperties.CONCEPT_TYPE.getPropertyName(),
                conceptBuilder.getIri(),
                visibilityTranslator.getDefaultVisibility()
        );

        addProperties(ingestOptions, vertexBuilder, conceptBuilder);

        LOGGER.trace("Saving vertex: %s", vertexBuilder.getVertexId());
        return vertexBuilder.save(getAuthorizations(ingestOptions));
    }

    private Edge save(IngestOptions ingestOptions, RelationshipBuilder relationshipBuilder) {
        Visibility relationshipVisibility = getVisibility(ingestOptions, relationshipBuilder.getVisibility());
        EdgeBuilderByVertexId edgeBuilder = graph.prepareEdge(
                relationshipBuilder.getId(),
                relationshipBuilder.getOutVertexId(),
                relationshipBuilder.getInVertexId(),
                relationshipBuilder.getIri(),
                getTimestamp(ingestOptions, relationshipBuilder.getTimestamp()),
                relationshipVisibility
        );

        addProperties(ingestOptions, edgeBuilder, relationshipBuilder);

        LOGGER.trace("Saving edge: %s", edgeBuilder.getEdgeId());
        return edgeBuilder.save(getAuthorizations(ingestOptions));
    }

    private void addProperties(
            IngestOptions ingestOptions,
            ElementBuilder elementBuilder,
            EntityBuilder entityBuilder
    ) {
        for (PropertyAddition<?> propertyAddition : entityBuilder.getPropertyAdditions()) {
            if (propertyAddition.getValue() != null) {
                elementBuilder.addPropertyValue(
                        propertyAddition.getKey(),
                        propertyAddition.getIri(),
                        propertyAddition.getValue(),
                        buildMetadata(ingestOptions, propertyAddition.getMetadata(), propertyAddition.getVisibility()),
                        getTimestamp(ingestOptions, propertyAddition.getTimestamp()),
                        getVisibility(ingestOptions, propertyAddition.getVisibility())
                );
            }
        }
    }

    public ValidationResult validateConceptBuilder(ConceptBuilder builder) {
        if (!verifiedClasses.contains(builder.getClass())) {
            LOGGER.trace("Validating Concept: %s", builder.getIri());
            Concept concept = ontologyRepository.getConceptByIRI(builder.getIri(), null);
            if (concept == null) {
                return new ValidationResult("Concept class: " + builder.getClass().getName() + " IRI: " + builder.getIri() + " is invalid");
            }

            verifiedClasses.add(builder.getClass());
        }

        return validateProperties(builder, builder.getPropertyAdditions());
    }

    public ValidationResult validateRelationshipBuilder(RelationshipBuilder builder) {
        if (!verifiedClasses.contains(builder.getClass())) {
            LOGGER.trace("Validating Relationship: %s", builder.getIri());
            Relationship relationship = ontologyRepository.getRelationshipByIRI(builder.getIri(), null);
            if (relationship == null) {
                return new ValidationResult("Relationship class: " + builder.getClass().getName() + " IRI: " + builder.getIri() + " is invalid");
            }
            verifiedClasses.add(builder.getClass());
        }

        String cacheKey = getRelationshipConceptCacheKey(builder);
        if (!verifiedRelationshipConcepts.contains(cacheKey)) {
            LOGGER.trace(
                    "Validating Relationship In/Out on %s: %s => %s",
                    builder.getIri(),
                    builder.getOutVertexIri(),
                    builder.getInVertexIri()
            );
            Relationship relationship = ontologyRepository.getRelationshipByIRI(builder.getIri(), null);
            List<String> domainConceptIRIs = relationship.getDomainConceptIRIs();
            List<String> outVertexAndParentIris = getConceptIriWithParents(builder.getOutVertexIri());
            outVertexAndParentIris.retainAll(domainConceptIRIs);
            if (outVertexAndParentIris.size() == 0) {
                return new ValidationResult("Out vertex Concept IRI: " + builder.getOutVertexIri() + " is invalid");
            }

            List<String> rangeConceptIRIs = relationship.getRangeConceptIRIs();
            List<String> inVertexAndParentIris = getConceptIriWithParents(builder.getInVertexIri());
            inVertexAndParentIris.retainAll(rangeConceptIRIs);
            if (inVertexAndParentIris.size() == 0) {
                return new ValidationResult("In vertex Concept IRI: " + builder.getInVertexIri() + " is invalid");
            }

            verifiedRelationshipConcepts.add(cacheKey);
        }

        return validateProperties(builder, builder.getPropertyAdditions());
    }

    private List<String> getConceptIriWithParents(String conceptIri) {
        List<String> conceptIriWithParents = new ArrayList<>();
        String parentConceptIRI = conceptIri;
        while (parentConceptIRI != null) {
            Concept parentConcept = ontologyRepository.getConceptByIRI(parentConceptIRI, null);
            parentConceptIRI = null;
            if (parentConcept != null) {
                conceptIriWithParents.add(parentConcept.getIRI());
                parentConceptIRI = parentConcept.getParentConceptIRI();
            }
        }
        return conceptIriWithParents;
    }

    private ValidationResult validateProperties(
            EntityBuilder entityBuilder,
            Set<PropertyAddition<?>> propertyAdditions
    ) {
        return propertyAdditions.stream()
                .map(propertyAddition -> validateProperty(entityBuilder, propertyAddition))
                .filter(validationResult -> !validationResult.isValid())
                .findFirst().orElse(ValidationResult.VALID_RESULT);
    }

    private ValidationResult validateProperty(EntityBuilder entityBuilder, PropertyAddition propertyAddition) {
        if (propertyAddition.getValue() == null) {
            return ValidationResult.VALID_RESULT;
        }

        String cacheKey = getClassPropertyCacheKey(entityBuilder, propertyAddition);
        if (!verifiedClassProperties.contains(cacheKey)) {
            LOGGER.trace("Validating Property on %s: %s", entityBuilder.getIri(), propertyAddition.getIri());
            HasOntologyProperties conceptOrRelationship;
            if (entityBuilder instanceof ConceptBuilder) {
                conceptOrRelationship = ontologyRepository.getConceptByIRI(entityBuilder.getIri(), null);
            } else if (entityBuilder instanceof RelationshipBuilder) {
                conceptOrRelationship = ontologyRepository.getRelationshipByIRI(entityBuilder.getIri(), null);
            } else {
                return new ValidationResult("Unexpected type: " + entityBuilder.getClass().getName());
            }

            if (conceptOrRelationship == null) {
                return new ValidationResult("Entity: " + entityBuilder.getIri() + " does not exist in the ontology");
            }

            OntologyProperty property = ontologyRepository.getPropertyByIRI(propertyAddition.getIri(), null);
            if (property == null) {
                return new ValidationResult("Property: " + propertyAddition.getIri() + " does not exist in the ontology");
            }

            if (!isPropertyValidForEntity(conceptOrRelationship, property)) {
                return new ValidationResult("Property: " + propertyAddition.getIri() + " is invalid for class (or its ancestors): " + entityBuilder.getClass().getName());
            }

            Class<?> valueType = propertyAddition.getValue().getClass();
            Class propertyType = PropertyType.getTypeClass(property.getDataType());
            if (propertyType.equals(BigDecimal.class)) {
                propertyType = Double.class;
            }
            if (!valueType.isAssignableFrom(propertyType)) {
                return new ValidationResult("Property: " + propertyAddition.getIri() + " type: " + valueType.getSimpleName() + " is invalid for Relationship class: " + entityBuilder.getClass().getName());
            }

            verifiedClassProperties.add(cacheKey);
        }

        return ValidationResult.VALID_RESULT;
    }

    private boolean isPropertyValidForEntity(HasOntologyProperties entity, OntologyProperty property) {
        if (entity.getProperties() == null || !entity.getProperties().contains(property)) {
            if (entity instanceof Concept) {
                Concept parentConcept = ontologyRepository.getParentConcept((Concept) entity, null);
                if (parentConcept != null) {
                    return isPropertyValidForEntity(parentConcept, property);
                }
            }
            return false;
        }
        return true;
    }

    private String getClassPropertyCacheKey(EntityBuilder entityBuilder, PropertyAddition propertyAddition) {
        return entityBuilder.getIri() + ":" + propertyAddition.getIri();
    }

    private String getRelationshipConceptCacheKey(RelationshipBuilder relationshipBuilder) {
        return relationshipBuilder.getIri() + ":" + relationshipBuilder.getOutVertexIri() + ":" + relationshipBuilder.getInVertexIri();
    }

    private Long getTimestamp(IngestOptions ingestOptions, Long timestamp) {
        return timestamp != null ? timestamp : ingestOptions.getDefaultTimestamp();
    }

    private Visibility getVisibility(IngestOptions ingestOptions, String visibilitySource) {
        if (visibilitySource != null) {
            return visibilityTranslator.toVisibility(visibilitySource).getVisibility();
        } else if (ingestOptions.getDefaultVisibility() != null) {
            return ingestOptions.getDefaultVisibility();
        }
        return visibilityTranslator.getDefaultVisibility();
    }

    private Metadata buildMetadata(IngestOptions ingestOptions, Map<String, Object> map, String visibilitySource) {
        Metadata metadata = new Metadata();

        Visibility defaultVisibility = visibilityTranslator.getDefaultVisibility();
        VisalloProperties.MODIFIED_DATE_METADATA.setMetadata(metadata, new Date(), defaultVisibility);
        VisalloProperties.MODIFIED_BY_METADATA.setMetadata(
                metadata,
                ingestOptions.getIngestUser().getUserId(),
                defaultVisibility
        );
        VisalloProperties.CONFIDENCE_METADATA.setMetadata(
                metadata,
                GraphRepository.SET_PROPERTY_CONFIDENCE,
                defaultVisibility
        );
        VisalloProperties.VISIBILITY_JSON_METADATA.setMetadata(
                metadata,
                new VisibilityJson(visibilitySource),
                defaultVisibility
        );

        if (ingestOptions.getDefaultMetadata() != null) {
            ingestOptions.getDefaultMetadata().forEach((k, v) -> metadata.add(k, v, defaultVisibility));
        }

        if (map != null) {
            map.forEach((k, v) -> metadata.add(k, v, defaultVisibility));
        }

        return metadata;
    }

    private Authorizations getAuthorizations(IngestOptions ingestOptions) {
        return authorizationRepository.getGraphAuthorizations(ingestOptions.getIngestUser(), ingestOptions.getAdditionalAuthorizations());
    }

    public static class ValidationResult {
        public static final ValidationResult VALID_RESULT = new ValidationResult(true, null);

        private boolean valid = false;
        private String validationError;

        public ValidationResult(String validationError) {
            this.validationError = validationError;
        }

        public ValidationResult(boolean valid, String validationError) {
            this.valid = valid;
            this.validationError = validationError;
        }

        public boolean isValid() {
            return valid;
        }

        public String getValidationError() {
            return validationError;
        }
    }
}
