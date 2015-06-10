package org.visallo.vertexium.model.ontology;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.ontology.*;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.clientapi.model.PropertyType;
import org.apache.commons.io.IOUtils;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.io.ReaderDocumentSource;
import org.semanticweb.owlapi.model.*;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.TextIndexHint;
import org.vertexium.inmemory.InMemoryAuthorizations;
import org.vertexium.util.ConvertingIterable;

import java.io.*;
import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.vertexium.util.IterableUtils.toList;

public class InMemoryOntologyRepository extends OntologyRepositoryBase {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(InMemoryOntologyRepository.class);
    private final Graph graph;
    private final OWLOntologyLoaderConfiguration owlConfig = new OWLOntologyLoaderConfiguration();
    private final Map<String, InMemoryConcept> conceptsCache = new HashMap<>();
    private final Map<String, InMemoryOntologyProperty> propertiesCache = new HashMap<>();
    private final Map<String, InMemoryRelationship> relationshipsCache = new HashMap<>();
    private final List<OwlData> fileCache = new ArrayList<>();

    @Inject
    public InMemoryOntologyRepository(
            final Graph graph,
            final Configuration configuration
    ) throws Exception {
        super(configuration);
        this.graph = graph;

        clearCache();
        Authorizations authorizations = new InMemoryAuthorizations(VISIBILITY_STRING);
        owlConfig.setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT);

        loadOntologies(getConfiguration(), authorizations);
    }

    @Override
    protected Concept importOntologyClass(OWLOntology o, OWLClass ontologyClass, File inDir, Authorizations authorizations) throws IOException {
        InMemoryConcept concept = (InMemoryConcept) super.importOntologyClass(o, ontologyClass, inDir, authorizations);
        conceptsCache.put(concept.getIRI(), concept);
        return concept;
    }

    @Override
    protected void setIconProperty(Concept concept, File inDir, String glyphIconFileName, String propertyKey, Authorizations authorizations) throws IOException {
        if (glyphIconFileName == null) {
            concept.setProperty(propertyKey, null, authorizations);
        } else {
            File iconFile = new File(inDir, glyphIconFileName);
            if (!iconFile.exists()) {
                throw new RuntimeException("Could not find icon file: " + iconFile.toString());
            }
            try {
                try (InputStream iconFileIn = new FileInputStream(iconFile)) {
                    concept.setProperty(propertyKey, IOUtils.toByteArray(iconFileIn), authorizations);
                }
            } catch (IOException ex) {
                throw new VisalloException("Failed to set glyph icon to " + iconFile, ex);
            }
        }
    }

    @Override
    protected void addEntityGlyphIconToEntityConcept(Concept entityConcept, byte[] rawImg) {
        entityConcept.setProperty(OntologyProperties.GLYPH_ICON.getPropertyName(), rawImg, null);
    }

    @Override
    protected void storeOntologyFile(InputStream inputStream, IRI documentIRI) {
        try {
            byte[] inFileData = IOUtils.toByteArray(inputStream);
            fileCache.add(new OwlData(documentIRI.toString(), inFileData));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isOntologyDefined(String iri) {
        for (OwlData owlData : fileCache) {
            if (owlData.iri.equals(iri)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected List<OWLOntology> loadOntologyFiles(OWLOntologyManager m, OWLOntologyLoaderConfiguration config, IRI excludedIRI) throws Exception {
        List<OWLOntology> loadedOntologies = new ArrayList<>();
        for (OwlData owlData : fileCache) {
            IRI visalloBaseOntologyIRI = IRI.create(owlData.iri);
            if (excludedIRI != null && excludedIRI.equals(visalloBaseOntologyIRI)) {
                continue;
            }
            try (InputStream visalloBaseOntologyIn = new ByteArrayInputStream(owlData.data)) {
                Reader visalloBaseOntologyReader = new InputStreamReader(visalloBaseOntologyIn);
                LOGGER.info("Loading existing ontology: %s", owlData.iri);
                OWLOntologyDocumentSource visalloBaseOntologySource = new ReaderDocumentSource(visalloBaseOntologyReader, visalloBaseOntologyIRI);
                OWLOntology o = m.loadOntologyFromOntologyDocument(visalloBaseOntologySource, config);
                loadedOntologies.add(o);
            }
        }
        return loadedOntologies;
    }

    @Override
    protected OntologyProperty addPropertyTo(
            List<Concept> concepts,
            String propertyIri,
            String displayName,
            PropertyType dataType,
            Map<String, String> possibleValues,
            Collection<TextIndexHint> textIndexHints,
            boolean userVisible,
            boolean searchable,
            boolean addable,
            String displayType,
            String propertyGroup,
            Double boost,
            String validationFormula,
            String displayFormula,
            ImmutableList<String> dependentPropertyIris,
            String[] intents
    ) {
        checkNotNull(concepts, "concept was null");
        InMemoryOntologyProperty property = getOrCreatePropertyType(
                propertyIri,
                dataType,
                displayName,
                possibleValues,
                textIndexHints,
                userVisible,
                searchable,
                addable,
                displayType,
                propertyGroup,
                boost,
                validationFormula,
                displayFormula,
                dependentPropertyIris,
                intents
        );
        for (Concept concept : concepts) {
            concept.getProperties().add(property);
        }
        checkNotNull(property, "Could not find property: " + propertyIri);
        return property;
    }

    @Override
    protected void getOrCreateInverseOfRelationship(Relationship fromRelationship, Relationship inverseOfRelationship) {
        InMemoryRelationship fromRelationshipMem = (InMemoryRelationship) fromRelationship;
        InMemoryRelationship inverseOfRelationshipMem = (InMemoryRelationship) inverseOfRelationship;

        fromRelationshipMem.addInverseOf(inverseOfRelationshipMem);
        inverseOfRelationshipMem.addInverseOf(fromRelationshipMem);
    }

    private InMemoryOntologyProperty getOrCreatePropertyType(
            final String propertyIri,
            final PropertyType dataType,
            final String displayName,
            Map<String, String> possibleValues,
            Collection<TextIndexHint> textIndexHints,
            boolean userVisible,
            boolean searchable,
            boolean addable,
            String displayType,
            String propertyGroup,
            Double boost,
            String validationFormula,
            String displayFormula,
            ImmutableList<String> dependentPropertyIris,
            String[] intents
    ) {
        InMemoryOntologyProperty property = (InMemoryOntologyProperty) getPropertyByIRI(propertyIri);
        if (property == null) {
            searchable = determineSearchable(propertyIri, dataType, textIndexHints, searchable);
            definePropertyOnGraph(graph, propertyIri, dataType, textIndexHints, boost);

            property = new InMemoryOntologyProperty();
            property.setDataType(dataType);
            property.setUserVisible(userVisible);
            property.setSearchable(searchable);
            property.setAddable(addable);
            property.setTitle(propertyIri);
            property.setBoost(boost);
            property.setDisplayType(displayType);
            property.setPropertyGroup(propertyGroup);
            property.setValidationFormula(validationFormula);
            property.setDisplayFormula(displayFormula);
            property.setDependentPropertyIris(dependentPropertyIris);
            for (String intent : intents) {
                property.addIntent(intent);
            }
            if (displayName != null && !displayName.trim().isEmpty()) {
                property.setDisplayName(displayName);
            }
            property.setPossibleValues(possibleValues);
            propertiesCache.put(propertyIri, property);
        }
        return property;
    }

    @Override
    public void clearCache() {
        // do nothing it's all in memory already.
    }

    @Override
    public Iterable<Relationship> getRelationships() {
        return new ConvertingIterable<InMemoryRelationship, Relationship>(relationshipsCache.values()) {
            @Override
            protected Relationship convert(InMemoryRelationship InMemRelationship) {
                return InMemRelationship;
            }
        };
    }

    @Override
    public Iterable<OntologyProperty> getProperties() {
        return new ConvertingIterable<InMemoryOntologyProperty, OntologyProperty>(propertiesCache.values()) {
            @Override
            protected OntologyProperty convert(InMemoryOntologyProperty ontologyProperty) {
                return ontologyProperty;
            }
        };
    }

    @Override
    public String getDisplayNameForLabel(String relationshipIRI) {
        InMemoryRelationship relationship = relationshipsCache.get(relationshipIRI);
        checkNotNull(relationship, "Could not find relationship " + relationshipIRI);
        return relationship.getDisplayName();
    }

    @Override
    public OntologyProperty getPropertyByIRI(String propertyIRI) {
        return propertiesCache.get(propertyIRI);
    }

    @Override
    public Relationship getRelationshipByIRI(String relationshipIRI) {
        return relationshipsCache.get(relationshipIRI);
    }

    @Override
    public boolean hasRelationshipByIRI(String relationshipIRI) {
        return getRelationshipByIRI(relationshipIRI) != null;
    }

    @Override
    public Iterable<Concept> getConceptsWithProperties() {
        return new ConvertingIterable<InMemoryConcept, Concept>(conceptsCache.values()) {
            @Override
            protected Concept convert(InMemoryConcept concept) {
                return concept;
            }
        };
    }

    @Override
    public Concept getEntityConcept() {
        return conceptsCache.get(InMemoryOntologyRepository.ENTITY_CONCEPT_IRI);
    }

    @Override
    public Concept getParentConcept(Concept concept) {
        for (String key : conceptsCache.keySet()) {
            if (key.equals(concept.getParentConceptIRI())) {
                return conceptsCache.get(key);
            }
        }
        return null;
    }

    @Override
    protected List<Concept> getChildConcepts(Concept concept) {
        List<Concept> results = new ArrayList<>();
        for (Concept childConcept : conceptsCache.values()) {
            if (concept.getIRI().equals(childConcept.getParentConceptIRI())) {
                results.add(childConcept);
            }
        }
        return results;
    }

    @Override
    public Concept getOrCreateConcept(Concept parent, String conceptIRI, String displayName, File inDir) {
        InMemoryConcept concept = (InMemoryConcept) getConceptByIRI(conceptIRI);
        if (concept != null) {
            return concept;
        }
        if (parent == null) {
            concept = new InMemoryConcept(conceptIRI, null);
        } else {
            concept = new InMemoryConcept(conceptIRI, ((InMemoryConcept) parent).getConceptIRI());
        }
        concept.setProperty(OntologyProperties.TITLE.getPropertyName(), conceptIRI, null);
        concept.setProperty(OntologyProperties.DISPLAY_NAME.getPropertyName(), displayName, null);
        conceptsCache.put(conceptIRI, concept);

        return concept;
    }

    @Override
    public Relationship getOrCreateRelationshipType(
            Iterable<Concept> domainConcepts,
            Iterable<Concept> rangeConcepts,
            String relationshipIRI,
            String displayName,
            String[] intents,
            boolean userVisible
    ) {
        Relationship relationship = getRelationshipByIRI(relationshipIRI);
        if (relationship != null) {
            return relationship;
        }

        List<String> domainConceptIris = toList(new ConvertingIterable<Concept, String>(domainConcepts) {
            @Override
            protected String convert(Concept o) {
                return o.getIRI();
            }
        });

        List<String> rangeConceptIris = toList(new ConvertingIterable<Concept, String>(rangeConcepts) {
            @Override
            protected String convert(Concept o) {
                return o.getIRI();
            }
        });

        InMemoryRelationship inMemRelationship = new InMemoryRelationship(relationshipIRI, displayName, domainConceptIris, rangeConceptIris, intents, userVisible);
        relationshipsCache.put(relationshipIRI, inMemRelationship);
        return inMemRelationship;
    }

    private static class OwlData {
        public final String iri;
        public final byte[] data;

        public OwlData(String iri, byte[] data) {
            this.iri = iri;
            this.data = data;
        }
    }

    protected Graph getGraph() {
        return graph;
    }
}
