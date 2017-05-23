package org.visallo.core.model.ontology;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.google.inject.Inject;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.io.ReaderDocumentSource;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.vertexium.Authorizations;
import org.vertexium.DefinePropertyBuilder;
import org.vertexium.Graph;
import org.vertexium.TextIndexHint;
import org.vertexium.property.StreamingPropertyValue;
import org.vertexium.query.Contains;
import org.vertexium.query.Query;
import org.vertexium.util.CloseableUtils;
import org.vertexium.util.ConvertingIterable;
import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.exception.VisalloResourceNotFoundException;
import org.visallo.core.model.lock.LockRepository;
import org.visallo.core.model.longRunningProcess.LongRunningProcessProperties;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.properties.types.VisalloProperty;
import org.visallo.core.model.search.SearchProperties;
import org.visallo.core.model.termMention.TermMentionRepository;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.ping.PingOntology;
import org.visallo.core.util.ExecutorServiceUtil;
import org.visallo.core.util.OWLOntologyUtil;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.clientapi.model.ClientApiOntology;
import org.visallo.web.clientapi.model.PropertyType;

import java.io.*;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class OntologyRepositoryBase implements OntologyRepository {
    public static final String BASE_OWL_IRI = "http://visallo.org";
    public static final String COMMENT_OWL_IRI = "http://visallo.org/comment";
    public static final String RESOURCE_ENTITY_PNG = "entity.png";
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(OntologyRepositoryBase.class);
    private static final String TOP_OBJECT_PROPERTY_IRI = "http://www.w3.org/2002/07/owl#topObjectProperty";
    private final Configuration configuration;
    private final LockRepository lockRepository;

    @Inject
    protected OntologyRepositoryBase(
            Configuration configuration,
            LockRepository lockRepository
    ) {
        this.configuration = configuration;
        this.lockRepository = lockRepository;
    }

    public void loadOntologies(Configuration config, Authorizations authorizations) throws Exception {
        lockRepository.lock("ontology", () -> {
            Concept rootConcept = getOrCreateConcept(null, ROOT_CONCEPT_IRI, "root", null);
            Concept entityConcept = getOrCreateConcept(rootConcept, ENTITY_CONCEPT_IRI, "thing", null);
            getOrCreateTopObjectPropertyRelationship(authorizations);

            clearCache();
            addEntityGlyphIcon(entityConcept);

            importResourceOwl("base.owl", BASE_OWL_IRI, authorizations);
            importResourceOwl("user.owl", UserRepository.OWL_IRI, authorizations);
            importResourceOwl("termMention.owl", TermMentionRepository.OWL_IRI, authorizations);
            importResourceOwl("workspace.owl", WorkspaceRepository.OWL_IRI, authorizations);
            importResourceOwl("comment.owl", COMMENT_OWL_IRI, authorizations);
            importResourceOwl("search.owl", SearchProperties.IRI, authorizations);
            importResourceOwl("longRunningProcess.owl", LongRunningProcessProperties.OWL_IRI, authorizations);
            importResourceOwl("ping.owl", PingOntology.BASE_IRI, authorizations);

            for (Map.Entry<String, Map<String, String>> owlGroup : config.getMultiValue(Configuration.ONTOLOGY_REPOSITORY_OWL).entrySet()) {
                String iri = owlGroup.getValue().get("iri");
                String dir = owlGroup.getValue().get("dir");
                String file = owlGroup.getValue().get("file");

                if (iri == null) {
                    throw new VisalloException("iri is required for group " + Configuration.ONTOLOGY_REPOSITORY_OWL + "." + owlGroup.getKey());
                }
                if (dir == null && file == null) {
                    throw new VisalloException("dir or file is required for " + Configuration.ONTOLOGY_REPOSITORY_OWL + "." + owlGroup.getKey());
                }
                if (dir != null && file != null) {
                    throw new VisalloException("you cannot specify both dir and file for " + Configuration.ONTOLOGY_REPOSITORY_OWL + "." + owlGroup.getKey());
                }

                if (dir != null) {
                    File owlFile = findOwlFile(new File(dir));
                    if (owlFile == null) {
                        throw new VisalloResourceNotFoundException(
                                "could not find owl file in directory " + new File(dir).getAbsolutePath()
                        );
                    }
                    importFile(owlFile, IRI.create(iri), authorizations);
                } else {
                    writePackage(new File(file), IRI.create(iri), authorizations);
                }
            }
            return true;
        });
    }

    private Relationship getOrCreateTopObjectPropertyRelationship(Authorizations authorizations) {
        Relationship topObjectProperty = getOrCreateRelationshipType(
                null,
                Collections.emptyList(),
                Collections.emptyList(),
                TOP_OBJECT_PROPERTY_IRI
        );
        if (topObjectProperty.getUserVisible()) {
            topObjectProperty.setProperty(OntologyProperties.USER_VISIBLE.getPropertyName(), false, authorizations);
        }
        return topObjectProperty;
    }

    private void importResourceOwl(String fileName, String iri, Authorizations authorizations) {
        importResourceOwl(OntologyRepositoryBase.class, fileName, iri, authorizations);
    }

    @Override
    public void importResourceOwl(Class baseClass, String fileName, String iri, Authorizations authorizations) {
        LOGGER.debug("importResourceOwl %s (iri: %s)", fileName, iri);
        InputStream owlFileIn = baseClass.getResourceAsStream(fileName);
        checkNotNull(owlFileIn, "Could not load resource " + baseClass.getResource(fileName) + " [" + fileName + "]");

        try {
            IRI documentIRI = IRI.create(iri);
            byte[] inFileData = IOUtils.toByteArray(owlFileIn);
            try {
                importFileData(inFileData, documentIRI, null, authorizations);
            } catch (OWLOntologyAlreadyExistsException ex) {
                LOGGER.warn("Ontology was already defined but not stored: " + fileName + " (iri: " + iri + ")", ex);
                storeOntologyFile(new ByteArrayInputStream(inFileData), documentIRI);
            }
        } catch (Exception ex) {
            throw new VisalloException("Could not import ontology file: " + fileName + " (iri: " + iri + ")", ex);
        } finally {
            CloseableUtils.closeQuietly(owlFileIn);
        }
    }

    public abstract boolean isOntologyDefined(String iri);

    private void addEntityGlyphIcon(Concept entityConcept) {
        if (entityConcept.getGlyphIcon() != null) {
            LOGGER.debug("entityConcept GlyphIcon already set. skipping addEntityGlyphIcon.");
            return;
        }
        LOGGER.debug("addEntityGlyphIcon");
        InputStream entityGlyphIconInputStream = OntologyRepositoryBase.class.getResourceAsStream(RESOURCE_ENTITY_PNG);
        checkNotNull(entityGlyphIconInputStream, "Could not load resource " + RESOURCE_ENTITY_PNG);

        try {
            ByteArrayOutputStream imgOut = new ByteArrayOutputStream();
            IOUtils.copy(entityGlyphIconInputStream, imgOut);

            byte[] rawImg = imgOut.toByteArray();

            addEntityGlyphIconToEntityConcept(entityConcept, rawImg);
        } catch (IOException e) {
            throw new VisalloException("invalid stream for glyph icon");
        }
    }

    protected abstract void addEntityGlyphIconToEntityConcept(Concept entityConcept, byte[] rawImg);

    @Override
    public String guessDocumentIRIFromPackage(File file) throws IOException, ZipException {
        ZipFile zipped = new ZipFile(file);
        if (zipped.isValidZipFile()) {
            File tempDir = Files.createTempDir();
            try {
                LOGGER.info("Extracting: %s to %s", file.getAbsoluteFile(), tempDir.getAbsolutePath());
                zipped.extractAll(tempDir.getAbsolutePath());

                File owlFile = findOwlFile(tempDir);
                return guessDocumentIRIFromFile(owlFile);
            } finally {
                FileUtils.deleteDirectory(tempDir);
            }
        } else {
            if (file.isDirectory()) {
                file = findOwlFile(file);
            }
            return guessDocumentIRIFromFile(file);
        }
    }

    public String guessDocumentIRIFromFile(File owlFile) throws IOException {
        try (FileInputStream owlFileIn = new FileInputStream(owlFile)) {
            String owlContents = IOUtils.toString(owlFileIn);

            Pattern iriRegex = Pattern.compile("<owl:Ontology rdf:about=\"(.*?)\">");
            Matcher m = iriRegex.matcher(owlContents);
            if (m.find()) {
                return m.group(1);
            }
            return null;
        }
    }

    @Override
    public void importFile(File inFile, IRI documentIRI, Authorizations authorizations) throws Exception {
        checkNotNull(inFile, "inFile cannot be null");
        if (!inFile.exists()) {
            throw new VisalloException("File " + inFile + " does not exist");
        }
        File inDir = inFile.getParentFile();

        try (FileInputStream inFileIn = new FileInputStream(inFile)) {
            LOGGER.debug("importing %s", inFile.getAbsolutePath());
            byte[] inFileData = IOUtils.toByteArray(inFileIn);
            importFileData(inFileData, documentIRI, inDir, authorizations);
        }
    }

    @Override
    public void importFileData(
            byte[] inFileData,
            IRI documentIRI,
            File inDir,
            Authorizations authorizations
    ) throws Exception {
        if (!hasFileChanged(documentIRI, inFileData)) {
            LOGGER.info("skipping %s, file has not changed", documentIRI);
            return;
        }
        Reader inFileReader = new InputStreamReader(new ByteArrayInputStream(inFileData));

        OWLOntologyLoaderConfiguration config = new OWLOntologyLoaderConfiguration();
        OWLOntologyManager m = createOwlOntologyManager(config, documentIRI);

        OWLOntologyDocumentSource documentSource = new ReaderDocumentSource(inFileReader, documentIRI);
        OWLOntology o = m.loadOntologyFromOntologyDocument(documentSource, config);

        long totalStartTime = System.currentTimeMillis();

        long startTime = System.currentTimeMillis();
        importOntologyAnnotationProperties(o, inDir, authorizations);
        clearCache(); // this is required to cause a new lookup of classes for data and object properties.
        long endTime = System.currentTimeMillis();
        long importAnnotationPropertiesTime = endTime - startTime;

        startTime = System.currentTimeMillis();
        importOntologyClasses(o, inDir, authorizations);
        clearCache(); // this is required to cause a new lookup of classes for data and object properties.
        endTime = System.currentTimeMillis();
        long importConceptsTime = endTime - startTime;

        startTime = System.currentTimeMillis();
        importObjectProperties(o, authorizations);
        clearCache(); // needed to find the relationship for inverse of
        endTime = System.currentTimeMillis();
        long importObjectPropertiesTime = endTime - startTime;

        startTime = System.currentTimeMillis();
        importInverseOfObjectProperties(o);
        endTime = System.currentTimeMillis();
        long importInverseOfObjectPropertiesTime = endTime - startTime;
        long totalEndTime = System.currentTimeMillis();

        startTime = System.currentTimeMillis();
        importDataProperties(o, authorizations);
        endTime = System.currentTimeMillis();
        long importDataPropertiesTime = endTime - startTime;

        LOGGER.debug("import annotation properties time: %dms", importAnnotationPropertiesTime);
        LOGGER.debug("import concepts time: %dms", importConceptsTime);
        LOGGER.debug("import data properties time: %dms", importDataPropertiesTime);
        LOGGER.debug("import object properties time: %dms", importObjectPropertiesTime);
        LOGGER.debug("import inverse of object properties time: %dms", importInverseOfObjectPropertiesTime);
        LOGGER.debug("import total time: %dms", totalEndTime - totalStartTime);

        // do this last after everything was successful so that isOntologyDefined can be used
        storeOntologyFile(new ByteArrayInputStream(inFileData), documentIRI);

        clearCache();
    }

    protected boolean hasFileChanged(IRI documentIRI, byte[] inFileData) {
        return true;
    }

    private void importInverseOfObjectProperties(OWLOntology o) {
        for (OWLObjectProperty objectProperty : o.getObjectPropertiesInSignature()) {
            if (!o.isDeclared(objectProperty, Imports.EXCLUDED)) {
                continue;
            }
            importInverseOf(o, objectProperty);
        }
    }

    private void importObjectProperties(OWLOntology o, Authorizations authorizations) {
        for (OWLObjectProperty objectProperty : o.getObjectPropertiesInSignature()) {
            importObjectProperty(o, objectProperty, authorizations);
        }
    }

    private void importDataProperties(OWLOntology o, Authorizations authorizations) {
        for (OWLDataProperty dataTypeProperty : o.getDataPropertiesInSignature()) {
            importDataProperty(o, dataTypeProperty, authorizations);
        }
        clearCache();
        for (OWLDataProperty dataTypeProperty : o.getDataPropertiesInSignature()) {
            importDataPropertyExtendedDataTableDomains(o, dataTypeProperty);
        }
    }

    protected void importOntologyAnnotationProperties(OWLOntology o, File inDir, Authorizations authorizations) {
        for (OWLAnnotationProperty annotation : o.getAnnotationPropertiesInSignature()) {
            importOntologyAnnotationProperty(o, annotation, inDir, authorizations);
        }
    }

    protected void importOntologyAnnotationProperty(
            OWLOntology o,
            OWLAnnotationProperty annotationProperty,
            File inDir,
            Authorizations authorizations
    ) {

    }

    private void importOntologyClasses(OWLOntology o, File inDir, Authorizations authorizations) throws IOException {
        for (OWLClass ontologyClass : o.getClassesInSignature()) {
            importOntologyClass(o, ontologyClass, inDir, authorizations);
        }
    }

    public OWLOntologyManager createOwlOntologyManager(
            OWLOntologyLoaderConfiguration config,
            IRI excludeDocumentIRI
    ) throws Exception {
        OWLOntologyManager m = OWLManager.createOWLOntologyManager();
        config.setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT);
        loadOntologyFiles(m, config, excludeDocumentIRI);
        return m;
    }

    protected abstract void storeOntologyFile(InputStream inputStream, IRI documentIRI);

    protected abstract List<OWLOntology> loadOntologyFiles(
            OWLOntologyManager m,
            OWLOntologyLoaderConfiguration config,
            IRI excludedIRI
    ) throws Exception;

    protected Concept importOntologyClass(
            OWLOntology o,
            OWLClass ontologyClass,
            File inDir,
            Authorizations authorizations
    ) throws IOException {
        String uri = ontologyClass.getIRI().toString();
        if ("http://www.w3.org/2002/07/owl#Thing".equals(uri)) {
            return getEntityConcept();
        }

        String label = OWLOntologyUtil.getLabel(o, ontologyClass);
        checkNotNull(label, "label cannot be null or empty: " + uri);
        LOGGER.info("Importing ontology class " + uri + " (label: " + label + ")");

        boolean isDeclaredInOntology = o.isDeclared(ontologyClass);

        Concept parent = getParentConcept(o, ontologyClass, inDir, authorizations);
        Concept result = getOrCreateConcept(parent, uri, label, inDir, isDeclaredInOntology);

        for (OWLAnnotation annotation : EntitySearcher.getAnnotations(ontologyClass, o)) {
            String annotationIri = annotation.getProperty().getIRI().toString();
            OWLLiteral valueLiteral = (OWLLiteral) annotation.getValue();
            String valueString = valueLiteral.getLiteral();

            if (annotationIri.equals(OntologyProperties.INTENT.getPropertyName())) {
                result.addIntent(valueString, authorizations);
                continue;
            }

            if (annotationIri.equals(OntologyProperties.SEARCHABLE.getPropertyName())) {
                boolean searchable = Boolean.parseBoolean(valueString);
                result.setProperty(OntologyProperties.SEARCHABLE.getPropertyName(), searchable, authorizations);
                continue;
            }

            if (annotationIri.equals(OntologyProperties.SORTABLE.getPropertyName())) {
                boolean sortable = Boolean.parseBoolean(valueString);
                result.setProperty(OntologyProperties.SORTABLE.getPropertyName(), sortable, authorizations);
                continue;
            }

            if (annotationIri.equals(OntologyProperties.ADDABLE.getPropertyName())) {
                boolean searchable = Boolean.parseBoolean(valueString);
                result.setProperty(OntologyProperties.ADDABLE.getPropertyName(), searchable, authorizations);
                continue;
            }

            if (annotationIri.equals(OntologyProperties.USER_VISIBLE.getPropertyName())) {
                boolean userVisible = Boolean.parseBoolean(valueString);
                result.setProperty(OntologyProperties.USER_VISIBLE.getPropertyName(), userVisible, authorizations);
                continue;
            }

            if (annotationIri.equals(OntologyProperties.GLYPH_ICON_FILE_NAME.getPropertyName())) {
                setIconProperty(
                        result,
                        inDir,
                        valueString,
                        OntologyProperties.GLYPH_ICON.getPropertyName(),
                        authorizations
                );
                continue;
            }

            if (annotationIri.equals(OntologyProperties.GLYPH_ICON_SELECTED_FILE_NAME.getPropertyName())) {
                setIconProperty(
                        result,
                        inDir,
                        valueString,
                        OntologyProperties.GLYPH_ICON_SELECTED.getPropertyName(),
                        authorizations
                );
                continue;
            }

            if (annotationIri.equals(OntologyProperties.MAP_GLYPH_ICON_FILE_NAME.getPropertyName())) {
                setIconProperty(
                        result,
                        inDir,
                        valueString,
                        OntologyProperties.MAP_GLYPH_ICON.getPropertyName(),
                        authorizations
                );
                continue;
            }

            if (annotationIri.equals(OntologyProperties.ADD_RELATED_CONCEPT_WHITE_LIST.getPropertyName())) {
                if (valueString.trim().length() == 0) {
                    continue;
                }
                result.setProperty(
                        OntologyProperties.ADD_RELATED_CONCEPT_WHITE_LIST.getPropertyName(),
                        valueString.trim(),
                        authorizations
                );
                continue;
            }

            if (annotationIri.equals("http://www.w3.org/2000/01/rdf-schema#label")) {
                result.setProperty(OntologyProperties.DISPLAY_NAME.getPropertyName(), valueString, authorizations);
                continue;
            }

            if (annotationIri.equals(OntologyProperties.UPDATEABLE.getPropertyName())) {
                boolean updateable = Boolean.parseBoolean(valueString);
                result.setProperty(OntologyProperties.UPDATEABLE.getPropertyName(), updateable, authorizations);
                continue;
            }

            if (annotationIri.equals(OntologyProperties.DELETEABLE.getPropertyName())) {
                boolean deleteable = Boolean.parseBoolean(valueString);
                result.setProperty(OntologyProperties.DELETEABLE.getPropertyName(), deleteable, authorizations);
                continue;
            }

            result.setProperty(annotationIri, valueString, authorizations);
        }

        return result;
    }

    protected void setIconProperty(
            Concept concept,
            File inDir,
            String glyphIconFileName,
            String propertyKey,
            Authorizations authorizations
    ) throws IOException {
        if (glyphIconFileName != null) {
            File iconFile = new File(inDir, glyphIconFileName);
            if (!iconFile.exists()) {
                throw new RuntimeException("Could not find icon file: " + iconFile.toString());
            }
            try (InputStream iconFileIn = new FileInputStream(iconFile)) {
                StreamingPropertyValue value = new StreamingPropertyValue(iconFileIn, byte[].class);
                value.searchIndex(false);
                value.store(true);
                concept.setProperty(propertyKey, value, authorizations);
            }
        }
    }

    protected Concept getParentConcept(
            OWLOntology o,
            OWLClass ontologyClass,
            File inDir,
            Authorizations authorizations
    ) throws IOException {
        Collection<OWLClassExpression> superClasses = EntitySearcher.getSuperClasses(ontologyClass, o);
        if (superClasses.size() == 0) {
            return getEntityConcept();
        } else if (superClasses.size() == 1) {
            OWLClassExpression superClassExpr = superClasses.iterator().next();
            OWLClass superClass = superClassExpr.asOWLClass();
            String superClassUri = superClass.getIRI().toString();
            Concept parent = getConceptByIRI(superClassUri);
            if (parent != null) {
                return parent;
            }

            parent = importOntologyClass(o, superClass, inDir, authorizations);
            if (parent == null) {
                throw new VisalloException("Could not find or create parent: " + superClass);
            }
            return parent;
        } else {
            throw new VisalloException("Unhandled multiple super classes. Found " + superClasses.size() + ", expected 0 or 1.");
        }
    }

    protected void importDataProperty(OWLOntology o, OWLDataProperty dataTypeProperty, Authorizations authorizations) {
        String propertyIRI = dataTypeProperty.getIRI().toString();
        try {
            String propertyDisplayName = OWLOntologyUtil.getLabel(o, dataTypeProperty);
            PropertyType propertyType = OWLOntologyUtil.getPropertyType(o, dataTypeProperty);
            checkNotNull(propertyType, "Failed to decode property type of: " + propertyIRI);
            boolean userVisible = OWLOntologyUtil.getUserVisible(o, dataTypeProperty);
            boolean searchable = OWLOntologyUtil.getSearchable(o, dataTypeProperty);
            boolean addable = OWLOntologyUtil.getAddable(o, dataTypeProperty);
            boolean sortable = !propertyType.equals(PropertyType.GEO_LOCATION) && OWLOntologyUtil.getSortable(o, dataTypeProperty);
            String displayType = OWLOntologyUtil.getDisplayType(o, dataTypeProperty);
            String propertyGroup = OWLOntologyUtil.getPropertyGroup(o, dataTypeProperty);
            String validationFormula = OWLOntologyUtil.getValidationFormula(o, dataTypeProperty);
            String displayFormula = OWLOntologyUtil.getDisplayFormula(o, dataTypeProperty);
            ImmutableList<String> dependentPropertyIris = OWLOntologyUtil.getDependentPropertyIris(o, dataTypeProperty);
            Double boost = OWLOntologyUtil.getBoost(o, dataTypeProperty);
            String[] intents = OWLOntologyUtil.getIntents(o, dataTypeProperty);
            boolean deleteable = OWLOntologyUtil.getDeleteable(o, dataTypeProperty);
            boolean updateable = OWLOntologyUtil.getUpdateable(o, dataTypeProperty);

            List<Concept> domainConcepts = new ArrayList<>();
            for (OWLClassExpression domainClassExpr : EntitySearcher.getDomains(dataTypeProperty, o)) {
                OWLClass domainClass = domainClassExpr.asOWLClass();
                String domainClassIri = domainClass.getIRI().toString();
                Concept domainConcept = getConceptByIRI(domainClassIri);
                if (domainConcept == null) {
                    LOGGER.error("Could not find class with IRI \"%s\" for data type property \"%s\"", domainClassIri, dataTypeProperty.getIRI());
                } else {
                    LOGGER.info("Adding data property " + propertyIRI + " to class " + domainConcept.getIRI());
                    domainConcepts.add(domainConcept);
                }
            }

            List<Relationship> domainRelationships = new ArrayList<>();
            for (OWLAnnotation domainAnnotation : OWLOntologyUtil.getObjectPropertyDomains(o, dataTypeProperty)) {
                String domainClassIri = OWLOntologyUtil.getOWLAnnotationValueAsString(domainAnnotation);
                Relationship domainRelationship = getRelationshipByIRI(domainClassIri);
                if (domainRelationship == null) {
                    LOGGER.error("Could not find relationship with IRI \"%s\" for data type property \"%s\"", domainClassIri, dataTypeProperty.getIRI());
                } else {
                    LOGGER.info("Adding data property " + propertyIRI + " to relationship " + domainRelationship.getIRI());
                    domainRelationships.add(domainRelationship);
                }
            }

            Map<String, String> possibleValues = OWLOntologyUtil.getPossibleValues(o, dataTypeProperty);
            Collection<TextIndexHint> textIndexHints = OWLOntologyUtil.getTextIndexHints(o, dataTypeProperty);
            OntologyProperty property = addPropertyTo(
                    domainConcepts,
                    domainRelationships,
                    propertyIRI,
                    propertyDisplayName,
                    propertyType,
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

            for (OWLAnnotation annotation : EntitySearcher.getAnnotations(dataTypeProperty, o)) {
                String annotationIri = annotation.getProperty().getIRI().toString();
                String valueString = annotation.getValue() instanceof OWLLiteral
                        ? ((OWLLiteral) annotation.getValue()).getLiteral()
                        : annotation.getValue().toString();

                if (annotationIri.equals(OntologyProperties.TITLE_FORMULA.getPropertyName())) {
                    property.setProperty(
                            OntologyProperties.TITLE_FORMULA.getPropertyName(),
                            valueString,
                            authorizations
                    );
                    continue;
                }

                if (annotationIri.equals(OntologyProperties.SUBTITLE_FORMULA.getPropertyName())) {
                    property.setProperty(
                            OntologyProperties.SUBTITLE_FORMULA.getPropertyName(),
                            valueString,
                            authorizations
                    );
                    continue;
                }

                if (annotationIri.equals(OntologyProperties.TIME_FORMULA.getPropertyName())) {
                    property.setProperty(
                            OntologyProperties.TIME_FORMULA.getPropertyName(),
                            valueString,
                            authorizations
                    );
                    continue;
                }
            }
        } catch (Throwable ex) {
            throw new VisalloException("Failed to load data property: " + propertyIRI, ex);
        }
    }

    private void importDataPropertyExtendedDataTableDomains(OWLOntology o, OWLDataProperty dataTypeProperty) {
        String propertyIri = dataTypeProperty.getIRI().toString();
        for (OWLAnnotation edtDomainAnnotation : OWLOntologyUtil.getExtendedDataTableDomains(o, dataTypeProperty)) {
            String tablePropertyIri = OWLOntologyUtil.getOWLAnnotationValueAsString(edtDomainAnnotation);
            addExtendedDataTableProperty(tablePropertyIri, propertyIri);
        }
    }

    protected void addExtendedDataTableProperty(String tablePropertyIri, String propertyIri) {
        OntologyProperty tableProperty = getPropertyByIRI(tablePropertyIri);
        checkNotNull(tableProperty, "Could not find table property: " + tablePropertyIri);

        OntologyProperty property = getPropertyByIRI(propertyIri);
        checkNotNull(property, "Could not find property: " + propertyIri);

        addExtendedDataTableProperty(tableProperty, property);
    }

    protected abstract void addExtendedDataTableProperty(OntologyProperty tableProperty, OntologyProperty property);

    @Override
    public OntologyProperty getOrCreateProperty(OntologyPropertyDefinition ontologyPropertyDefinition) {
        OntologyProperty property = getPropertyByIRI(ontologyPropertyDefinition.getPropertyIri());
        if (property != null) {
            return property;
        }
        OntologyProperty prop = addPropertyTo(
                ontologyPropertyDefinition.getConcepts(),
                ontologyPropertyDefinition.getRelationships(),
                ontologyPropertyDefinition.getPropertyIri(),
                ontologyPropertyDefinition.getDisplayName(),
                ontologyPropertyDefinition.getDataType(),
                ontologyPropertyDefinition.getPossibleValues(),
                ontologyPropertyDefinition.getTextIndexHints(),
                ontologyPropertyDefinition.isUserVisible(),
                ontologyPropertyDefinition.isSearchable(),
                ontologyPropertyDefinition.isAddable(),
                ontologyPropertyDefinition.isSortable(),
                ontologyPropertyDefinition.getDisplayType(),
                ontologyPropertyDefinition.getPropertyGroup(),
                ontologyPropertyDefinition.getBoost(),
                ontologyPropertyDefinition.getValidationFormula(),
                ontologyPropertyDefinition.getDisplayFormula(),
                ontologyPropertyDefinition.getDependentPropertyIris(),
                ontologyPropertyDefinition.getIntents(),
                ontologyPropertyDefinition.getDeleteable(),
                ontologyPropertyDefinition.getUpdateable()
        );
        if (ontologyPropertyDefinition.getExtendedDataTableDomains() != null) {
            for (String extendedDataTableDomain : ontologyPropertyDefinition.getExtendedDataTableDomains()) {
                OntologyProperty tableProperty = getPropertyByIRI(extendedDataTableDomain);
                checkNotNull(tableProperty, "Could not find table property: " + extendedDataTableDomain);
                addExtendedDataTableProperty(tableProperty, prop);
            }
        }
        return prop;
    }

    protected abstract OntologyProperty addPropertyTo(
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
    );

    protected Relationship importObjectProperty(
            OWLOntology o,
            OWLObjectProperty objectProperty,
            Authorizations authorizations
    ) {
        String iri = objectProperty.getIRI().toString();
        String label = OWLOntologyUtil.getLabel(o, objectProperty);

        checkNotNull(label, "label cannot be null or empty for " + iri);
        LOGGER.info("Importing ontology object property " + iri + " (label: " + label + ")");

        boolean isDeclaredInOntology = o.isDeclared(objectProperty);

        Relationship parent = getParentObjectProperty(o, objectProperty, authorizations);
        Relationship relationship = getOrCreateRelationshipType(
                parent,
                getDomainsConcepts(o, objectProperty),
                getRangesConcepts(o, objectProperty),
                iri,
                isDeclaredInOntology
        );

        for (OWLAnnotation annotation : EntitySearcher.getAnnotations(objectProperty, o)) {
            String annotationIri = annotation.getProperty().getIRI().toString();
            OWLLiteral valueLiteral = (OWLLiteral) annotation.getValue();
            String valueString = valueLiteral.getLiteral();

            if (annotationIri.equals(OntologyProperties.INTENT.getPropertyName())) {
                relationship.addIntent(valueString, authorizations);
                continue;
            }

            if (annotationIri.equals(OntologyProperties.USER_VISIBLE.getPropertyName())) {
                relationship.setProperty(
                        OntologyProperties.USER_VISIBLE.getPropertyName(),
                        Boolean.parseBoolean(valueString),
                        authorizations
                );
                continue;
            }

            if (annotationIri.equals(OntologyProperties.DELETEABLE.getPropertyName())) {
                relationship.setProperty(
                        OntologyProperties.DELETEABLE.getPropertyName(),
                        Boolean.parseBoolean(valueString),
                        authorizations
                );
                continue;
            }

            if (annotationIri.equals(OntologyProperties.UPDATEABLE.getPropertyName())) {
                relationship.setProperty(
                        OntologyProperties.UPDATEABLE.getPropertyName(),
                        Boolean.parseBoolean(valueString),
                        authorizations
                );
                continue;
            }

            if (annotationIri.equals("http://www.w3.org/2000/01/rdf-schema#label")) {
                relationship.setProperty(
                        OntologyProperties.DISPLAY_NAME.getPropertyName(),
                        valueString,
                        authorizations
                );
                continue;
            }

            relationship.setProperty(annotationIri, valueString, authorizations);
        }
        return relationship;
    }

    private Relationship getParentObjectProperty(
            OWLOntology o,
            OWLObjectProperty objectProperty,
            Authorizations authorizations
    ) {
        Collection<OWLObjectPropertyExpression> superProperties = EntitySearcher.getSuperProperties(objectProperty, o);
        if (superProperties.size() == 0) {
            return getOrCreateTopObjectPropertyRelationship(authorizations);
        } else if (superProperties.size() == 1) {
            OWLObjectPropertyExpression superPropertyExpr = superProperties.iterator().next();
            OWLObjectProperty superProperty = superPropertyExpr.asOWLObjectProperty();
            String superPropertyUri = superProperty.getIRI().toString();
            Relationship parent = getRelationshipByIRI(superPropertyUri);
            if (parent != null) {
                return parent;
            }

            parent = importObjectProperty(o, superProperty, authorizations);
            if (parent == null) {
                throw new VisalloException("Could not find or create parent: " + superProperty);
            }
            return parent;
        } else {
            throw new VisalloException("Unhandled multiple super properties. Found " + superProperties.size() + ", expected 0 or 1.");
        }
    }

    protected void importInverseOf(OWLOntology o, OWLObjectProperty objectProperty) {
        String iri = objectProperty.getIRI().toString();
        Relationship fromRelationship = null;

        for (OWLObjectPropertyExpression inverseOf : EntitySearcher.getInverses(objectProperty, o)) {
            if (inverseOf instanceof OWLObjectProperty) {
                if (fromRelationship == null) {
                    fromRelationship = getRelationshipByIRI(iri);
                    checkNotNull(fromRelationship, "could not find from relationship: " + iri);
                }

                OWLObjectProperty inverseOfOWLObjectProperty = (OWLObjectProperty) inverseOf;
                String inverseOfIri = inverseOfOWLObjectProperty.getIRI().toString();
                Relationship inverseOfRelationship = getRelationshipByIRI(inverseOfIri);
                getOrCreateInverseOfRelationship(fromRelationship, inverseOfRelationship);
            }
        }
    }

    protected abstract void getOrCreateInverseOfRelationship(
            Relationship fromRelationship,
            Relationship inverseOfRelationship
    );

    private Iterable<Concept> getRangesConcepts(OWLOntology o, OWLObjectProperty objectProperty) {
        List<Concept> ranges = new ArrayList<>();
        for (OWLClassExpression rangeClassExpr : EntitySearcher.getRanges(objectProperty, o)) {
            OWLClass rangeClass = rangeClassExpr.asOWLClass();
            String rangeClassIri = rangeClass.getIRI().toString();
            Concept ontologyClass = getConceptByIRI(rangeClassIri);
            if (ontologyClass == null) {
                LOGGER.error("Could not find class with IRI \"%s\" for object property \"%s\"", rangeClassIri, objectProperty.getIRI());
            } else {
                ranges.add(ontologyClass);
            }
        }
        return ranges;
    }

    private Iterable<Concept> getDomainsConcepts(OWLOntology o, OWLObjectProperty objectProperty) {
        List<Concept> domains = new ArrayList<>();
        for (OWLClassExpression domainClassExpr : EntitySearcher.getDomains(objectProperty, o)) {
            OWLClass rangeClass = domainClassExpr.asOWLClass();
            String rangeClassIri = rangeClass.getIRI().toString();
            Concept ontologyClass = getConceptByIRI(rangeClassIri);
            if (ontologyClass == null) {
                LOGGER.error("Could not find class with IRI \"%s\" for object property \"%s\"", rangeClassIri, objectProperty.getIRI());
            } else {
                domains.add(ontologyClass);
            }
        }
        return domains;
    }

    @Override
    public void writePackage(File file, IRI documentIRI, Authorizations authorizations) throws Exception {
        if (!file.exists()) {
            throw new VisalloException("OWL file does not exist: " + file.getAbsolutePath());
        }
        if (!file.isFile()) {
            throw new VisalloException("OWL file is not a file: " + file.getAbsolutePath());
        }
        ZipFile zipped = new ZipFile(file);
        if (zipped.isValidZipFile()) {
            File tempDir = Files.createTempDir();
            try {
                LOGGER.info("Extracting: %s to %s", file.getAbsoluteFile(), tempDir.getAbsolutePath());
                zipped.extractAll(tempDir.getAbsolutePath());

                File owlFile = findOwlFile(tempDir);
                importFile(owlFile, documentIRI, authorizations);
            } finally {
                FileUtils.deleteDirectory(tempDir);
            }
        } else {
            importFile(file, documentIRI, authorizations);
        }
    }

    protected File findOwlFile(File fileOrDir) {
        if (fileOrDir.isFile()) {
            return fileOrDir;
        }
        File[] files = fileOrDir.listFiles();
        if (files == null) {
            return null;
        }
        for (File child : files) {
            if (child.isDirectory()) {
                File found = findOwlFile(child);
                if (found != null) {
                    return found;
                }
            } else if (child.getName().toLowerCase().endsWith(".owl")) {
                return child;
            }
        }
        return null;
    }

    @Override
    public Set<Concept> getConceptAndAllChildrenByIri(String conceptIRI) {
        Concept concept = getConceptByIRI(conceptIRI);
        if (concept == null) {
            return null;
        }
        return getConceptAndAllChildren(concept);
    }

    @Override
    public Set<Concept> getConceptAndAllChildren(Concept concept) {
        List<Concept> childConcepts = getChildConcepts(concept);
        Set<Concept> result = Sets.newHashSet(concept);
        if (childConcepts.size() > 0) {
            List<Concept> childrenList = new ArrayList<>();
            for (Concept childConcept : childConcepts) {
                Set<Concept> child = getConceptAndAllChildren(childConcept);
                childrenList.addAll(child);
            }
            result.addAll(childrenList);
        }
        return result;
    }

    protected abstract List<Concept> getChildConcepts(Concept concept);

    @Override
    public Set<Relationship> getRelationshipAndAllChildren(Relationship relationship) {
        List<Relationship> childRelationships = getChildRelationships(relationship);
        Set<Relationship> result = Sets.newHashSet(relationship);
        if (childRelationships.size() > 0) {
            List<Relationship> childrenList = new ArrayList<>();
            for (Relationship childRelationship : childRelationships) {
                Set<Relationship> child = getRelationshipAndAllChildren(childRelationship);
                childrenList.addAll(child);
            }
            result.addAll(childrenList);
        }
        return result;
    }

    protected abstract List<Relationship> getChildRelationships(Relationship relationship);

    @Override
    public void resolvePropertyIds(JSONArray filterJson) throws JSONException {
        for (int i = 0; i < filterJson.length(); i++) {
            JSONObject filter = filterJson.getJSONObject(i);
            if (filter.has("propertyId") && !filter.has("propertyName")) {
                String propertyVertexId = filter.getString("propertyId");
                OntologyProperty property = getPropertyByIRI(propertyVertexId);
                if (property == null) {
                    throw new RuntimeException("Could not find property with id: " + propertyVertexId);
                }
                filter.put("propertyName", property.getTitle());
                filter.put("propertyDataType", property.getDataType());
            }
        }
    }

    @Override
    public Concept getConceptByIRI(String conceptIRI) {
        for (Concept concept : getConceptsWithProperties()) {
            if (concept.getIRI().equals(conceptIRI)) {
                return concept;
            }
        }
        return null;
    }

    @Override
    public OntologyProperty getPropertyByIRI(String propertyIRI) {
        for (OntologyProperty prop : getProperties()) {
            if (prop.getTitle().equals(propertyIRI)) {
                return prop;
            }
        }
        return null;
    }

    @Override
    public OntologyProperty getRequiredPropertyByIRI(String propertyIRI) {
        OntologyProperty property = getPropertyByIRI(propertyIRI);
        if (property == null) {
            throw new VisalloException("Could not find property by IRI: " + propertyIRI);
        }
        return property;
    }

    public Relationship getRelationshipByIRI(String relationshipIRI) {
        for (Relationship rel : getRelationships()) {
            if (rel.getIRI().equals(relationshipIRI)) {
                return rel;
            }
        }
        return null;
    }

    public Concept getConceptByIntent(String intent) {
        String configurationKey = CONFIG_INTENT_CONCEPT_PREFIX + intent;
        String conceptIri = getConfiguration().get(configurationKey, null);
        if (conceptIri != null) {
            Concept concept = getConceptByIRI(conceptIri);
            if (concept == null) {
                throw new VisalloException("Could not find concept by configuration key: " + configurationKey);
            }
            return concept;
        }

        List<Concept> concepts = findLoadedConceptsByIntent(intent);
        if (concepts.size() == 0) {
            return null;
        }
        if (concepts.size() == 1) {
            return concepts.get(0);
        }

        String iris = Joiner.on(',').join(new ConvertingIterable<Concept, String>(concepts) {
            @Override
            protected String convert(Concept o) {
                return o.getIRI();
            }
        });
        throw new VisalloException("Found multiple concepts for intent: " + intent + " (" + iris + ")");
    }

    public String getConceptIRIByIntent(String intent) {
        Concept concept = getConceptByIntent(intent);
        if (concept != null) {
            return concept.getIRI();
        }
        return null;
    }

    @Override
    public Concept getRequiredConceptByIntent(String intent) {
        Concept concept = getConceptByIntent(intent);
        if (concept == null) {
            throw new VisalloException("Could not find concept by intent: " + intent);
        }
        return concept;
    }

    @Override
    public String getRequiredConceptIRIByIntent(String intent) {
        return getRequiredConceptByIntent(intent).getIRI();
    }

    @Override
    public Concept getRequiredConceptByIRI(String iri) {
        Concept concept = getConceptByIRI(iri);
        if (concept == null) {
            throw new VisalloException("Could not find concept by IRI: " + iri);
        }
        return concept;
    }

    private List<Concept> findLoadedConceptsByIntent(String intent) {
        List<Concept> results = new ArrayList<>();
        for (Concept concept : getConceptsWithProperties()) {
            String[] conceptIntents = concept.getIntents();
            if (Arrays.asList(conceptIntents).contains(intent)) {
                results.add(concept);
            }
        }
        return results;
    }

    public Relationship getRelationshipByIntent(String intent) {
        String configurationKey = CONFIG_INTENT_RELATIONSHIP_PREFIX + intent;
        String relationshipIri = getConfiguration().get(configurationKey, null);
        if (relationshipIri != null) {
            Relationship relationship = getRelationshipByIRI(relationshipIri);
            if (relationship == null) {
                throw new VisalloException("Could not find relationship by configuration key: " + configurationKey);
            }
            return relationship;
        }

        List<Relationship> relationships = findLoadedRelationshipsByIntent(intent);
        if (relationships.size() == 0) {
            return null;
        }
        if (relationships.size() == 1) {
            return relationships.get(0);
        }

        String iris = Joiner.on(',').join(new ConvertingIterable<Relationship, String>(relationships) {
            @Override
            protected String convert(Relationship o) {
                return o.getIRI();
            }
        });
        throw new VisalloException("Found multiple relationships for intent: " + intent + " (" + iris + ")");
    }

    public String getRelationshipIRIByIntent(String intent) {
        Relationship relationship = getRelationshipByIntent(intent);
        if (relationship != null) {
            return relationship.getIRI();
        }
        return null;
    }

    @Override
    public Relationship getRequiredRelationshipByIntent(String intent) {
        Relationship relationship = getRelationshipByIntent(intent);
        if (relationship == null) {
            throw new VisalloException("Could not find relationship by intent: " + intent);
        }
        return relationship;
    }

    @Override
    public String getRequiredRelationshipIRIByIntent(String intent) {
        return getRequiredRelationshipByIntent(intent).getIRI();
    }

    private List<Relationship> findLoadedRelationshipsByIntent(String intent) {
        List<Relationship> results = new ArrayList<>();
        for (Relationship relationship : getRelationships()) {
            String[] relationshipIntents = relationship.getIntents();
            if (Arrays.asList(relationshipIntents).contains(intent)) {
                results.add(relationship);
            }
        }
        return results;
    }

    public OntologyProperty getPropertyByIntent(String intent) {
        String configurationKey = CONFIG_INTENT_PROPERTY_PREFIX + intent;
        String propertyIri = getConfiguration().get(configurationKey, null);
        if (propertyIri != null) {
            OntologyProperty property = getPropertyByIRI(propertyIri);
            if (property == null) {
                throw new VisalloException("Could not find property by configuration key: " + configurationKey);
            }
            return property;
        }

        List<OntologyProperty> properties = getPropertiesByIntent(intent);
        if (properties.size() == 0) {
            return null;
        }
        if (properties.size() == 1) {
            return properties.get(0);
        }

        String iris = Joiner.on(',').join(new ConvertingIterable<OntologyProperty, String>(properties) {
            @Override
            protected String convert(OntologyProperty o) {
                return o.getTitle();
            }
        });
        throw new VisalloException("Found multiple properties for intent: " + intent + " (" + iris + ")");
    }

    public String getPropertyIRIByIntent(String intent) {
        OntologyProperty prop = getPropertyByIntent(intent);
        if (prop != null) {
            return prop.getTitle();
        }
        return null;
    }

    @Override
    public OntologyProperty getRequiredPropertyByIntent(String intent) {
        OntologyProperty property = getPropertyByIntent(intent);
        if (property == null) {
            throw new VisalloException("Could not find property by intent: " + intent);
        }
        return property;
    }

    @Override
    public String getRequiredPropertyIRIByIntent(String intent) {
        return getRequiredPropertyByIntent(intent).getTitle();
    }

    @Override
    public OntologyProperty getDependentPropertyParent(String iri) {
        for (OntologyProperty property : getProperties()) {
            if (property.getDependentPropertyIris().contains(iri)) {
                return property;
            }
        }
        return null;
    }

    @Override
    public <T extends VisalloProperty> T getVisalloPropertyByIntent(String intent, Class<T> visalloPropertyType) {
        String propertyIri = getPropertyIRIByIntent(intent);
        if (propertyIri == null) {
            LOGGER.warn("No property found for intent: %s", intent);
            return null;
        }
        try {
            Constructor<T> constructor = visalloPropertyType.getConstructor(String.class);
            return constructor.newInstance(propertyIri);
        } catch (Exception ex) {
            throw new VisalloException("Could not create property for intent: " + intent + " (propertyIri: " + propertyIri + ")");
        }
    }

    @Override
    public <T extends VisalloProperty> T getRequiredVisalloPropertyByIntent(
            String intent,
            Class<T> visalloPropertyType
    ) {
        T result = getVisalloPropertyByIntent(intent, visalloPropertyType);
        if (result == null) {
            throw new VisalloException("Could not find property by intent: " + intent);
        }
        return result;
    }

    public List<OntologyProperty> getPropertiesByIntent(String intent) {
        List<OntologyProperty> results = new ArrayList<>();
        for (OntologyProperty property : getProperties()) {
            String[] propertyIntents = property.getIntents();
            if (Arrays.asList(propertyIntents).contains(intent)) {
                results.add(property);
            }
        }
        return results;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ClientApiOntology getClientApiObject() {
        Object[] results = ExecutorServiceUtil.runAllAndWait(
                () -> {
                    Iterable<Concept> concepts = getConceptsWithProperties();
                    return Concept.toClientApiConcepts(concepts);
                },
                () -> {
                    Iterable<OntologyProperty> properties = getProperties();
                    return OntologyProperty.toClientApiProperties(properties);
                },
                () -> {
                    Iterable<Relationship> relationships = getRelationships();
                    return Relationship.toClientApiRelationships(relationships);
                }
        );

        ClientApiOntology ontology = new ClientApiOntology();
        ontology.addAllConcepts((Collection<ClientApiOntology.Concept>) results[0]);
        ontology.addAllProperties((Collection<ClientApiOntology.Property>) results[1]);
        ontology.addAllRelationships((Collection<ClientApiOntology.Relationship>) results[2]);

        return ontology;
    }

    public final Configuration getConfiguration() {
        return configuration;
    }

    protected void definePropertyOnGraph(
            Graph graph,
            String propertyIri,
            PropertyType dataType,
            Collection<TextIndexHint> textIndexHints,
            Double boost,
            boolean sortable
    ) {
        DefinePropertyBuilder definePropertyBuilder = graph.defineProperty(propertyIri).sortable(sortable);
        definePropertyBuilder.dataType(PropertyType.getTypeClass(dataType));
        if (dataType == PropertyType.DIRECTORY_ENTITY) {
            definePropertyBuilder.textIndexHint(TextIndexHint.EXACT_MATCH);
        } else if (dataType == PropertyType.STRING) {
            definePropertyBuilder.textIndexHint(textIndexHints);
        }
        if (boost != null) {
            if (graph.isFieldBoostSupported()) {
                definePropertyBuilder.boost(boost);
            } else {
                LOGGER.warn("Field boosting is not support by the graph");
            }
        }
        definePropertyBuilder.define();
    }

    protected boolean determineSearchable(
            String propertyIri,
            PropertyType dataType,
            Collection<TextIndexHint> textIndexHints,
            boolean searchable
    ) {
        if (dataType == PropertyType.EXTENDED_DATA_TABLE) {
            return false;
        }
        if (dataType == PropertyType.STRING) {
            checkNotNull(textIndexHints, "textIndexHints are required for string properties");
            if (searchable && (textIndexHints.isEmpty() || textIndexHints.equals(TextIndexHint.NONE))) {
                searchable = false;
            } else if (!searchable && (!textIndexHints.isEmpty() || !textIndexHints.equals(TextIndexHint.NONE))) {
                LOGGER.info("textIndexHints was specified for non-UI-searchable string property:: " + propertyIri);
            }
        }
        return searchable;
    }

    @Override
    public void addConceptTypeFilterToQuery(Query query, String conceptTypeIri, boolean includeChildNodes) {
        checkNotNull(conceptTypeIri, "conceptTypeIri cannot be null");
        List<ElementTypeFilter> filters = new ArrayList<>();
        filters.add(new ElementTypeFilter(conceptTypeIri, includeChildNodes));
        addConceptTypeFilterToQuery(query, filters);
    }

    @Override
    public void addConceptTypeFilterToQuery(Query query, Collection<ElementTypeFilter> filters) {
        checkNotNull(query, "query cannot be null");
        checkNotNull(filters, "filters cannot be null");

        if (filters.isEmpty()) {
            return;
        }

        Set<String> conceptIds = new HashSet<>(filters.size());

        for (ElementTypeFilter filter : filters) {
            Concept concept = getConceptByIRI(filter.iri);
            checkNotNull(concept, "Could not find concept with IRI: " + filter.iri);

            conceptIds.add(concept.getIRI());

            if (filter.includeChildNodes) {
                Set<Concept> childConcepts = getConceptAndAllChildren(concept);
                conceptIds.addAll(childConcepts.stream().map(Concept::getIRI).collect(Collectors.toSet()));
            }
        }

        query.has(VisalloProperties.CONCEPT_TYPE.getPropertyName(), Contains.IN, conceptIds);
    }

    @Override
    public void addEdgeLabelFilterToQuery(Query query, String edgeLabel, boolean includeChildNodes) {
        checkNotNull(edgeLabel, "edgeLabel cannot be null");
        List<ElementTypeFilter> filters = new ArrayList<>();
        filters.add(new ElementTypeFilter(edgeLabel, includeChildNodes));
        addEdgeLabelFilterToQuery(query, filters);
    }

    @Override
    public void addEdgeLabelFilterToQuery(Query query, Collection<ElementTypeFilter> filters) {
        checkNotNull(filters, "filters cannot be null");

        if (filters.isEmpty()) {
            return;
        }

        Set<String> edgeIds = new HashSet<>(filters.size());

        for (ElementTypeFilter filter : filters) {
            Relationship relationship = getRelationshipByIRI(filter.iri);
            checkNotNull(relationship, "Could not find edge with IRI: " + filter.iri);

            edgeIds.add(relationship.getIRI());

            if (filter.includeChildNodes) {
                Set<Relationship> childRelations = getRelationshipAndAllChildren(relationship);
                edgeIds.addAll(childRelations.stream().map(Relationship::getIRI).collect(Collectors.toSet()));
            }
        }

        query.hasEdgeLabel(edgeIds);
    }
}
