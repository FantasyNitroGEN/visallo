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
import org.visallo.core.model.user.PrivilegeRepository;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.clientapi.model.PropertyType;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.vertexium.util.IterableUtils.toList;

public class InMemoryOntologyRepository extends OntologyRepositoryBase {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(InMemoryOntologyRepository.class);
    private static final String PUBLIC_ONTOLOGY_CACHE_KEY = "InMemoryOntologyRepository.PUBLIC";

    private final Graph graph;

    private final Map<String, Map<String, InMemoryConcept>> conceptsCache = Collections.synchronizedMap(new HashMap<>());
    private final Map<String, Map<String, InMemoryRelationship>> relationshipsCache = Collections.synchronizedMap(new HashMap<>());
    private final Map<String, Map<String, InMemoryOntologyProperty>> propertiesCache = Collections.synchronizedMap(new HashMap<>());

    private final List<OwlData> fileCache = new ArrayList<>();

    @Inject
    public InMemoryOntologyRepository(
            final Graph graph,
            final Configuration configuration,
            final LockRepository lockRepository
    ) throws Exception {
        super(configuration, lockRepository);
        this.graph = graph;

        clearCache();

        conceptsCache.put(PUBLIC_ONTOLOGY_CACHE_KEY, new HashMap<>());
        relationshipsCache.put(PUBLIC_ONTOLOGY_CACHE_KEY, new HashMap<>());
        propertiesCache.put(PUBLIC_ONTOLOGY_CACHE_KEY, new HashMap<>());

        loadOntologies(getConfiguration(), new InMemoryAuthorizations(VISIBILITY_STRING));
    }

    private <T> Map<String, T> computeCacheForWorkspace(Map<String, Map<String, T>> cache, User user, String workspaceId) {
        Map<String, T> result = new HashMap<>();
        result.putAll(cache.compute(PUBLIC_ONTOLOGY_CACHE_KEY, (k, v) -> v == null ? new HashMap<>() : v));
        if (user != null && workspaceId != null && cache.containsKey(workspaceId)) {
            result.putAll(cache.get(workspaceId));
        }
        return result;
    }

    @Override
    protected Concept importOntologyClass(
            OWLOntology o,
            OWLClass ontologyClass,
            File inDir,
            Authorizations authorizations
    ) throws IOException {
        InMemoryConcept concept = (InMemoryConcept) super.importOntologyClass(o, ontologyClass, inDir, authorizations);
        conceptsCache.get(PUBLIC_ONTOLOGY_CACHE_KEY).put(concept.getIRI(), concept);
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
        relationshipsCache.get(PUBLIC_ONTOLOGY_CACHE_KEY).put(relationship.getIRI(), relationship);
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
    protected void addEntityGlyphIconToEntityConcept(Concept entityConcept, byte[] rawImg, Authorizations authorizations) {
        entityConcept.setProperty(OntologyProperties.GLYPH_ICON.getPropertyName(), rawImg, authorizations);
    }

    @Override
    protected void storeOntologyFile(InputStream inputStream, IRI documentIRI, Authorizations authorizations) {
        try {
            byte[] inFileData = IOUtils.toByteArray(inputStream);
            synchronized (fileCache) {
                fileCache.add(new OwlData(documentIRI.toString(), inFileData));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Deprecated
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
    public void updatePropertyDependentIris(OntologyProperty property, Collection<String> dependentPropertyIris, User user, String workspaceId) {
        InMemoryOntologyProperty inMemoryOntologyProperty = (InMemoryOntologyProperty) property;
        inMemoryOntologyProperty.setDependentPropertyIris(dependentPropertyIris);
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
            boolean updateable,
            User user,
            String workspaceId) {
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
                updateable,
                user,
                workspaceId
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
    public void updatePropertyDomainIris(OntologyProperty property, Set<String> domainIris, User user, String workspaceId) {
        for (Concept concept : getConceptsWithProperties(user, workspaceId)) {
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
    public void internalPublishConcept(Concept concept, User user, String workspaceId) {
        if (conceptsCache.containsKey(workspaceId)) {
            Map<String, InMemoryConcept> sandboxedConcepts = conceptsCache.get(workspaceId);
            if (sandboxedConcepts.containsKey(concept.getIRI())) {
                InMemoryConcept sandboxConcept = sandboxedConcepts.remove(concept.getIRI());
                sandboxConcept.removeWorkspaceId();
                conceptsCache.get(PUBLIC_ONTOLOGY_CACHE_KEY).put(concept.getIRI(), sandboxConcept);
            }
        }
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
            boolean updateabale,
            User user,
            String workspaceId
    ) {
        InMemoryOntologyProperty property = getPropertyByIRI(propertyIri, user, workspaceId);
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

            String cacheKey = workspaceId == null  ? PUBLIC_ONTOLOGY_CACHE_KEY : workspaceId;
            Map<String, InMemoryOntologyProperty> workspaceCache = propertiesCache.compute(cacheKey, (k, v) -> v == null ? new HashMap<>() : v);
            workspaceCache.put(propertyIri, property);
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
    public Iterable<Relationship> getRelationships(User user, String workspaceId) {
        return computeCacheForWorkspace(relationshipsCache, user, workspaceId).values().stream().collect(Collectors.toList());
    }

    @Override
    public Iterable<Relationship> getRelationships(Iterable<String> ids, User user, String workspaceId) {
        if (ids != null) {
            Map<String, InMemoryRelationship> workspaceRelationships = computeCacheForWorkspace(relationshipsCache, user, workspaceId);
            return StreamSupport.stream(ids.spliterator(), true)
                    .map(workspaceRelationships::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    @Override
    public Iterable<OntologyProperty> getProperties(Iterable<String> ids, User user, String workspaceId) {
        if (ids != null) {
            Map<String, InMemoryOntologyProperty> workspacePropsCache = computeCacheForWorkspace(propertiesCache, user, workspaceId);
            return StreamSupport.stream(ids.spliterator(), true)
                    .map(workspacePropsCache::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    @Override
    public Iterable<OntologyProperty> getProperties(User user, String workspaceId) {
        return computeCacheForWorkspace(propertiesCache, user, workspaceId).values().stream().collect(Collectors.toList());
    }

    @Override
    public String getDisplayNameForLabel(String relationshipIRI, User user, String workspaceId) {
        InMemoryRelationship relationship = computeCacheForWorkspace(relationshipsCache, user, workspaceId).get(relationshipIRI);
        checkNotNull(relationship, "Could not find relationship " + relationshipIRI);
        return relationship.getDisplayName();
    }

    @Override
    public InMemoryOntologyProperty getPropertyByIRI(String propertyIRI, User user, String workspaceId) {
        return computeCacheForWorkspace(propertiesCache, user, workspaceId).get(propertyIRI);
    }

    @Override
    public InMemoryRelationship getRelationshipByIRI(String relationshipIRI, User user, String workspaceId) {
        return computeCacheForWorkspace(relationshipsCache, user, workspaceId).get(relationshipIRI);
    }

    @Override
    public InMemoryConcept getConceptByIRI(String conceptIRI, User user, String workspaceId) {
        return computeCacheForWorkspace(conceptsCache, user, workspaceId).get(conceptIRI);
    }

    @Override
    public boolean hasRelationshipByIRI(String relationshipIRI, User user, String workspaceId) {
        return getRelationshipByIRI(relationshipIRI, user, workspaceId) != null;
    }

    @Override
    public Iterable<Concept> getConceptsWithProperties(User user, String workspaceId) {
        return computeCacheForWorkspace(conceptsCache, user, workspaceId).values().stream().collect(Collectors.toList());
    }

    @Override
    public Concept getRootConcept(User user, String workspaceId) {
        return computeCacheForWorkspace(conceptsCache, user, workspaceId).get(InMemoryOntologyRepository.ROOT_CONCEPT_IRI);
    }

    @Override
    public Concept getEntityConcept(User user, String workspaceId) {
        return computeCacheForWorkspace(conceptsCache, user, workspaceId).get(InMemoryOntologyRepository.ENTITY_CONCEPT_IRI);
    }

    @Override
    public Concept getParentConcept(Concept concept, User user, String workspaceId) {
        return computeCacheForWorkspace(conceptsCache, user, workspaceId).get(concept.getParentConceptIRI());
    }

    @Override
    public Iterable<Concept> getConcepts(Iterable<String> ids, User user, String workspaceId) {
        if (ids != null) {
            Map<String, InMemoryConcept> workspaceConcepts = computeCacheForWorkspace(conceptsCache, user, workspaceId);
            return StreamSupport.stream(ids.spliterator(), true)
                    .map(workspaceConcepts::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    @Override
    protected List<Concept> getChildConcepts(Concept concept, User user, String workspaceId) {
        Map<String, InMemoryConcept> workspaceConcepts = computeCacheForWorkspace(conceptsCache, user, workspaceId);
        return workspaceConcepts.values().stream()
                .filter(workspaceConcept -> concept.getIRI().equals(workspaceConcept.getParentConceptIRI()))
                .collect(Collectors.toList());
    }

    @Override
    protected List<Relationship> getChildRelationships(Relationship relationship, User user, String workspaceId) {
        Map<String, InMemoryRelationship> workspaceRelationships = computeCacheForWorkspace(relationshipsCache, user, workspaceId);
        return workspaceRelationships.values().stream()
                .filter(workspaceRelationship -> relationship.getIRI().equals(workspaceRelationship.getParentIRI()))
                .collect(Collectors.toList());
    }

    @Override
    protected Concept internalGetOrCreateConcept(Concept parent, String conceptIRI, String displayName, String glyphIconHref, File inDir, boolean deleteChangeableProperties, User user, String workspaceId) {
        InMemoryConcept concept = getConceptByIRI(conceptIRI, user, workspaceId);

        if (concept != null) {
            return concept;
        }
        if (parent == null) {
            concept = new InMemoryConcept(conceptIRI, null, workspaceId);
        } else {
            concept = new InMemoryConcept(conceptIRI, ((InMemoryConcept) parent).getConceptIRI(), workspaceId);
        }
        concept.setProperty(OntologyProperties.TITLE.getPropertyName(), conceptIRI, null);
        concept.setProperty(OntologyProperties.DISPLAY_NAME.getPropertyName(), displayName, null);

        String cacheKey = workspaceId == null ? PUBLIC_ONTOLOGY_CACHE_KEY : workspaceId;
        Map<String, InMemoryConcept> workspaceCache = conceptsCache.compute(cacheKey, (k, v) -> v == null ? new HashMap<>() : v);
        workspaceCache.put(conceptIRI, concept);

        return concept;
    }

    @Override
    protected Relationship internalGetOrCreateRelationshipType(Relationship parent, Iterable<Concept> domainConcepts, Iterable<Concept> rangeConcepts, String relationshipIRI, boolean deleteChangeableProperties, User user, String workspaceId) {
        Relationship relationship = getRelationshipByIRI(relationshipIRI, user, workspaceId);
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
                properties,
                workspaceId
        );

        String cacheKey = workspaceId == null  ? PUBLIC_ONTOLOGY_CACHE_KEY : workspaceId;
        Map<String, InMemoryRelationship> workspaceCache = relationshipsCache.compute(cacheKey, (k, v) -> v == null ? new HashMap<>() : v);
        workspaceCache.put(relationshipIRI, inMemRelationship);

        return inMemRelationship;
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

    @Override
    protected Authorizations getAuthorizations(User user, String workspaceId) {
        return new InMemoryAuthorizations(VISIBILITY_STRING);
    }

    protected Graph getGraph() {
        return graph;
    }
}
