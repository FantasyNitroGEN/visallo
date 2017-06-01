package org.visallo.vertexium.model.ontology;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import org.apache.commons.io.IOUtils;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.io.ReaderDocumentSource;
import org.semanticweb.owlapi.model.*;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.TextIndexHint;
import org.vertexium.inmemory.InMemoryAuthorizations;
import org.vertexium.util.ConvertingIterable;
import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.lock.LockRepository;
import org.visallo.core.model.ontology.*;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.clientapi.model.PropertyType;

import java.io.*;
import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.vertexium.util.IterableUtils.toList;

// FIXME: all "return null" or empty methods
public class InMemoryOntologyRepository extends OntologyRepositoryBase {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(InMemoryOntologyRepository.class);
    private final Graph graph;
    private final OWLOntologyLoaderConfiguration owlConfig = new OWLOntologyLoaderConfiguration();
    private final Map<String, InMemoryConcept> conceptsCache = new HashMap<>();
    private final Map<String, InMemoryOntologyProperty> propertiesCache = new HashMap<>();
    private final Map<String, InMemoryRelationship> relationshipsCache = new HashMap<>();
    private final List<OwlData> fileCache = new ArrayList<>();
    private Authorizations authorizations;

    @Inject
    public InMemoryOntologyRepository(
            final Graph graph,
            final Configuration configuration,
            final LockRepository lockRepository
    ) throws Exception {
        super(configuration, lockRepository);
        this.graph = graph;

        clearCache();
        this.authorizations = new InMemoryAuthorizations(VISIBILITY_STRING);
        owlConfig.setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT);

        loadOntologies(getConfiguration(), authorizations);
    }

    @Override
    protected Concept importOntologyClass(
            OWLOntology o,
            OWLClass ontologyClass,
            File inDir,
            Authorizations authorizations
    ) throws IOException {
        InMemoryConcept concept = (InMemoryConcept) super.importOntologyClass(o, ontologyClass, inDir, authorizations);
        conceptsCache.put(concept.getIRI(), concept);
        return concept;
    }

    @Override
    protected Relationship importObjectProperty(
            OWLOntology o,
            OWLObjectProperty objectProperty,
            Authorizations authorizations
    ) {
        InMemoryRelationship relationship = (InMemoryRelationship) super.importObjectProperty(
                o,
                objectProperty,
                authorizations
        );
        relationshipsCache.put(relationship.getIRI(), relationship);
        return relationship;
    }

    @Override
    protected void setIconProperty(
            Concept concept,
            File inDir,
            String glyphIconFileName,
            String propertyKey,
            Authorizations authorizations
    ) throws IOException {
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
            synchronized (fileCache) {
                fileCache.add(new OwlData(documentIRI.toString(), inFileData));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isOntologyDefined(String iri) {
        synchronized (fileCache) {
            for (OwlData owlData : fileCache) {
                if (owlData.iri.equals(iri)) {
                    return true;
                }
            }
            return false;
        }
    }

    @Override
    public void updatePropertyDependentIris(OntologyProperty property, Collection<String> dependentPropertyIris) {
        InMemoryOntologyProperty inMemoryOntologyProperty = (InMemoryOntologyProperty) property;
        inMemoryOntologyProperty.setDependentPropertyIris(dependentPropertyIris);
    }

    @Override
    public void updatePropertyDependentIris(OntologyProperty property, Collection<String> dependentPropertyIris, User user, String workspaceId) {

    }


    @Override
    protected List<OWLOntology> loadOntologyFiles(
            OWLOntologyManager m,
            OWLOntologyLoaderConfiguration config,
            IRI excludedIRI
    ) throws Exception {
        List<OWLOntology> loadedOntologies = new ArrayList<>();
        List<OwlData> fileCacheCopy;
        synchronized (fileCache) {
            fileCacheCopy = ImmutableList.copyOf(fileCache);
        }
        for (OwlData owlData : fileCacheCopy) {
            IRI visalloBaseOntologyIRI = IRI.create(owlData.iri);
            if (excludedIRI != null && excludedIRI.equals(visalloBaseOntologyIRI)) {
                continue;
            }
            try (InputStream visalloBaseOntologyIn = new ByteArrayInputStream(owlData.data)) {
                Reader visalloBaseOntologyReader = new InputStreamReader(visalloBaseOntologyIn);
                LOGGER.info("Loading existing ontology: %s", owlData.iri);
                OWLOntologyDocumentSource visalloBaseOntologySource = new ReaderDocumentSource(
                        visalloBaseOntologyReader,
                        visalloBaseOntologyIRI
                );
                OWLOntology o = m.loadOntologyFromOntologyDocument(visalloBaseOntologySource, config);
                loadedOntologies.add(o);
            }
        }
        return loadedOntologies;
    }

    @Override
    protected OntologyProperty addPropertyTo(
            List<Concept> concepts,
            List<Relationship> relationships,
            String propertyIri,
            String displayName,
            PropertyType dataType,
            Map<String, String> possibleValues,
            Collection<TextIndexHint> textIndexHints,
            boolean userVisible,
            boolean searchable,
            boolean addable,
            boolean sortable,
            String displayType,
            String propertyGroup,
            Double boost,
            String validationFormula,
            String displayFormula,
            ImmutableList<String> dependentPropertyIris,
            String[] intents,
            boolean deleteable,
            boolean updateable
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
                sortable,
                displayType,
                propertyGroup,
                boost,
                validationFormula,
                displayFormula,
                dependentPropertyIris,
                intents,
                deleteable,
                updateable
        );
        for (Concept concept : concepts) {
            concept.getProperties().add(property);
        }
        for (Relationship relationship : relationships) {
            relationship.getProperties().add(property);
        }
        checkNotNull(property, "Could not find property: " + propertyIri);
        return property;
    }

    @Override
    protected OntologyProperty addPropertyTo(List<Concept> concepts, List<Relationship> relationships, String propertyIri, String displayName, PropertyType dataType, Map<String, String> possibleValues, Collection<TextIndexHint> textIndexHints, boolean userVisible, boolean searchable, boolean addable, boolean sortable, String displayType, String propertyGroup, Double boost, String validationFormula, String displayFormula, ImmutableList<String> dependentPropertyIris, String[] intents, boolean deleteable, boolean updateable, User user, String workspaceId) {
        return null;
    }


    @Override
    public void updatePropertyDomainIris(OntologyProperty property, Set<String> domainIris) {
        for (Concept concept : getConceptsWithProperties()) {
            if (concept.getProperties().contains(property)) {
                if (!domainIris.remove(concept.getIRI())) {
                    concept.getProperties().remove(property);
                }
            }
        }

        for (String domainIri : domainIris) {
            getConceptByIRI(domainIri).getProperties().add(property);
        }
    }

    @Override
    public void updatePropertyDomainIris(OntologyProperty property, Set<String> domainIris, User user, String workspaceId) {

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
            boolean sortable,
            String displayType,
            String propertyGroup,
            Double boost,
            String validationFormula,
            String displayFormula,
            ImmutableList<String> dependentPropertyIris,
            String[] intents,
            boolean deleteable,
            boolean updateabale
    ) {
        InMemoryOntologyProperty property = getPropertyByIRI(propertyIri);
        if (property == null) {
            searchable = determineSearchable(propertyIri, dataType, textIndexHints, searchable);
            definePropertyOnGraph(graph, propertyIri, dataType, textIndexHints, boost, sortable);

            if (dataType.equals(PropertyType.EXTENDED_DATA_TABLE)) {
                property = new InMemoryExtendedDataTableOntologyProperty();
            } else {
                property = new InMemoryOntologyProperty();
            }
            property.setDataType(dataType);
            property.setUserVisible(userVisible);
            property.setSearchable(searchable);
            property.setAddable(addable);
            property.setSortable(sortable);
            property.setTitle(propertyIri);
            property.setBoost(boost);
            property.setDisplayType(displayType);
            property.setPropertyGroup(propertyGroup);
            property.setValidationFormula(validationFormula);
            property.setDisplayFormula(displayFormula);
            property.setDependentPropertyIris(dependentPropertyIris);
            property.setDeleteable(deleteable);
            property.setUpdateable(updateabale);
            if (intents != null) {
                for (String intent : intents) {
                    property.addIntent(intent);
                }
            }
            if (displayName != null && !displayName.trim().isEmpty()) {
                property.setDisplayName(displayName);
            }
            if (textIndexHints != null && textIndexHints.size() > 0) {
                for (TextIndexHint textIndexHint : textIndexHints) {
                    property.addTextIndexHints(textIndexHint.toString());
                }
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
    public void clearCache(String workspaceId) {
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
    public Iterable<Relationship> getRelationships(Iterable<String> ids, User user, String workspaceId) {
        return null;
    }

    @Override
    public Iterable<Relationship> getRelationships(User user, String workspaceId) {
        return null;
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
    public Iterable<OntologyProperty> getProperties(Iterable<String> ids, User user, String workspaceId) {
        return null;
    }

    @Override
    public Iterable<OntologyProperty> getProperties(User user, String workspaceId) {
        return null;
    }

    @Override
    public String getDisplayNameForLabel(String relationshipIRI) {
        InMemoryRelationship relationship = relationshipsCache.get(relationshipIRI);
        checkNotNull(relationship, "Could not find relationship " + relationshipIRI);
        return relationship.getDisplayName();
    }

    @Override
    public String getDisplayNameForLabel(String relationshipIRI, User user, String workspaceId) {
        return null;
    }

    @Override
    public InMemoryOntologyProperty getPropertyByIRI(String propertyIRI) {
        return propertiesCache.get(propertyIRI);
    }

    @Override
    public InMemoryRelationship getRelationshipByIRI(String relationshipIRI) {
        return relationshipsCache.get(relationshipIRI);
    }

    @Override
    public InMemoryConcept getConceptByIRI(String conceptIRI) {
        return getConceptByIRI(conceptIRI, null, null);
    }

    @Override
    public InMemoryConcept getConceptByIRI(String conceptIRI, User user, String workspaceId) {
        return (InMemoryConcept) super.getConceptByIRI(conceptIRI, user, workspaceId);
    }

    @Override
    public boolean hasRelationshipByIRI(String relationshipIRI) {
        return getRelationshipByIRI(relationshipIRI) != null;
    }

    @Override
    public Iterable<Concept> getConceptsWithProperties(User user, String workspaceId) {
        return null;
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
    public Concept getRootConcept(User user, String workspaceId) {
        return null;
    }

    @Override
    public Concept getEntityConcept() {
        return conceptsCache.get(InMemoryOntologyRepository.ENTITY_CONCEPT_IRI);
    }

    @Override
    public Concept getEntityConcept(User user, String workspaceId) {
        return null;
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
    public Concept getParentConcept(Concept concept, User user, String workspaceId) {
        return null;
    }

    @Override
    public Iterable<Concept> getConcepts(Iterable<String> ids, User user, String workspaceId) {
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
    protected List<Concept> getChildConcepts(Concept concept, User user, String workspaceId) {
        return null;
    }

    @Override
    protected List<Relationship> getChildRelationships(Relationship relationship) {
        List<Relationship> results = new ArrayList<>();
        for (Relationship childRelationship : relationshipsCache.values()) {
            if (relationship.getIRI().equals(childRelationship.getParentIRI())) {
                results.add(childRelationship);
            }
        }
        return results;
    }

    @Override
    protected List<Relationship> getChildRelationships(Relationship relationship, User user, String workspaceId) {
        return null;
    }

    @Override
    public Concept getOrCreateConcept(Concept parent, String conceptIRI, String displayName, File inDir) {
        return getOrCreateConcept(parent, conceptIRI, displayName, inDir, true);
    }

    @Override
    public Concept getOrCreateConcept(Concept parent, String conceptIRI, String displayName, File inDir, User user, String workspaceId) {
        return getOrCreateConcept(parent, conceptIRI, displayName, inDir, true, user, workspaceId);
    }

    @Override
    public Concept getOrCreateConcept(
            Concept parent,
            String conceptIRI,
            String displayName,
            File inDir,
            boolean isDeclaredInOntology
    ) {
        return getOrCreateConcept(parent, conceptIRI, displayName, inDir, true, null,null);
    }

    @Override
    public Concept getOrCreateConcept(Concept parent, String conceptIRI, String displayName, File inDir, boolean deleteChangeableProperties, User user, String workspaceId) {
        InMemoryConcept concept = getConceptByIRI(conceptIRI, user, workspaceId);

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
            Relationship parent,
            Iterable<Concept> domainConcepts,
            Iterable<Concept> rangeConcepts,
            String relationshipIRI
    ) {
        return getOrCreateRelationshipType(parent,domainConcepts, rangeConcepts, relationshipIRI, true);
    }

    @Override
    public Relationship getOrCreateRelationshipType(
            Relationship parent,
            Iterable<Concept> domainConcepts,
            Iterable<Concept> rangeConcepts,
            String relationshipIRI,
            boolean isDeclaredInOntology
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

        String parentIRI = parent == null ? null : parent.getIRI();
        Collection<OntologyProperty> properties = new ArrayList<>();
        InMemoryRelationship inMemRelationship = new InMemoryRelationship(
                parentIRI,
                relationshipIRI,
                domainConceptIris,
                rangeConceptIris,
                properties
        );
        relationshipsCache.put(relationshipIRI, inMemRelationship);
        return inMemRelationship;
    }

    @Override
    public Relationship getOrCreateRelationshipType(Relationship parent, Iterable<Concept> domainConcepts, Iterable<Concept> rangeConcepts, String relationshipIRI, boolean deleteChangeableProperties, User user, String workspaceId) {
        return null;
    }

    protected void addExtendedDataTableProperty(OntologyProperty tableProperty, OntologyProperty property, User user, String workspaceId) {
        if (!(tableProperty instanceof InMemoryExtendedDataTableOntologyProperty)) {
            throw new VisalloException("Invalid property type to add extended data table property to: " + tableProperty.getDataType());
        }
        InMemoryExtendedDataTableOntologyProperty edtp = (InMemoryExtendedDataTableOntologyProperty) tableProperty;
        edtp.addTableProperty(property.getIri());
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
