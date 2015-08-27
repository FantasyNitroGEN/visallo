package org.visallo.core.model.ontology;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
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
import org.semanticweb.owlapi.owlxml.renderer.OWLXMLRenderer;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.vertexium.Authorizations;
import org.vertexium.DefinePropertyBuilder;
import org.vertexium.Graph;
import org.vertexium.TextIndexHint;
import org.vertexium.property.StreamingPropertyValue;
import org.vertexium.util.CloseableUtils;
import org.vertexium.util.ConvertingIterable;
import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.exception.VisalloResourceNotFoundException;
import org.visallo.core.model.longRunningProcess.LongRunningProcessProperties;
import org.visallo.core.model.properties.types.VisalloProperty;
import org.visallo.core.model.termMention.TermMentionRepository;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.util.ExecutorServiceUtil;
import org.visallo.core.util.JSONUtil;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.clientapi.model.ClientApiOntology;
import org.visallo.web.clientapi.model.PropertyType;

import java.io.*;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class OntologyRepositoryBase implements OntologyRepository {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(OntologyRepositoryBase.class);
    public static final String BASE_OWL_IRI = "http://visallo.org";
    public static final String COMMENT_OWL_IRI = "http://visallo.org/comment";
    public static final String RESOURCE_ENTITY_PNG = "entity.png";
    private final Configuration configuration;

    protected OntologyRepositoryBase(Configuration configuration) {
        this.configuration = configuration;
    }

    public void loadOntologies(Configuration config, Authorizations authorizations) throws Exception {
        Concept rootConcept = getOrCreateConcept(null, ROOT_CONCEPT_IRI, "root", null);
        Concept entityConcept = getOrCreateConcept(rootConcept, ENTITY_CONCEPT_IRI, "thing", null);
        clearCache();
        addEntityGlyphIcon(entityConcept);

        importResourceOwl("base.owl", BASE_OWL_IRI, authorizations);
        importResourceOwl("user.owl", UserRepository.OWL_IRI, authorizations);
        importResourceOwl("termMention.owl", TermMentionRepository.OWL_IRI, authorizations);
        importResourceOwl("workspace.owl", WorkspaceRepository.OWL_IRI, authorizations);
        importResourceOwl("comment.owl", COMMENT_OWL_IRI, authorizations);
        importResourceOwl("longRunningProcess.owl", LongRunningProcessProperties.OWL_IRI, authorizations);

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

            if (isOntologyDefined(iri)) {
                LOGGER.debug("Ontology %s (iri: %s) is already defined", dir != null ? dir : file, iri);
                continue;
            }

            if (dir != null) {
                File owlFile = findOwlFile(new File(dir));
                if (owlFile == null) {
                    throw new VisalloResourceNotFoundException("could not find owl file in directory " + new File(dir).getAbsolutePath());
                }
                importFile(owlFile, IRI.create(iri), authorizations);
            } else {
                writePackage(new File(file), IRI.create(iri), authorizations);
            }
        }
    }

    private void importResourceOwl(String fileName, String iri, Authorizations authorizations) {
        importResourceOwl(OntologyRepositoryBase.class, fileName, iri, authorizations);
    }

    @Override
    public void importResourceOwl(Class baseClass, String fileName, String iri, Authorizations authorizations) {
        if (isOntologyDefined(iri)) {
            LOGGER.debug("Ontology %s (iri: %s) is already defined", fileName, iri);
            return;
        }

        LOGGER.debug("importResourceOwl %s (iri: %s)", fileName, iri);
        InputStream owlFileIn = baseClass.getResourceAsStream(fileName);
        checkNotNull(owlFileIn, "Could not load resource " + baseClass.getResource(fileName));

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
    public void importFileData(byte[] inFileData, IRI documentIRI, File inDir, Authorizations authorizations) throws Exception {
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
        importDataProperties(o);
        endTime = System.currentTimeMillis();
        long importDataPropertiesTime = endTime - startTime;

        startTime = System.currentTimeMillis();
        importObjectProperties(o);
        clearCache(); // needed to find the relationship for inverse of
        endTime = System.currentTimeMillis();
        long importObjectPropertiesTime = endTime - startTime;

        startTime = System.currentTimeMillis();
        importInverseOfObjectProperties(o);
        endTime = System.currentTimeMillis();
        long importInverseOfObjectPropertiesTime = endTime - startTime;
        long totalEndTime = System.currentTimeMillis();

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

    private void importInverseOfObjectProperties(OWLOntology o) {
        for (OWLObjectProperty objectProperty : o.getObjectPropertiesInSignature()) {
            if (!o.isDeclared(objectProperty, Imports.EXCLUDED)) {
                continue;
            }
            importInverseOf(o, objectProperty);
        }
    }

    private void importObjectProperties(OWLOntology o) {
        for (OWLObjectProperty objectProperty : o.getObjectPropertiesInSignature()) {
            if (!o.isDeclared(objectProperty, Imports.EXCLUDED)) {
                continue;
            }
            importObjectProperty(o, objectProperty);
        }
    }

    private void importDataProperties(OWLOntology o) {
        for (OWLDataProperty dataTypeProperty : o.getDataPropertiesInSignature()) {
            if (!o.isDeclared(dataTypeProperty, Imports.EXCLUDED)) {
                continue;
            }
            importDataProperty(o, dataTypeProperty);
        }
    }

    protected void importOntologyAnnotationProperties(OWLOntology o, File inDir, Authorizations authorizations) {
        for (OWLAnnotationProperty annotation : o.getAnnotationPropertiesInSignature()) {
            importOntologyAnnotationProperty(o, annotation, inDir, authorizations);
        }
    }

    protected void importOntologyAnnotationProperty(OWLOntology o, OWLAnnotationProperty annotationProperty, File inDir, Authorizations authorizations) {

    }

    private void importOntologyClasses(OWLOntology o, File inDir, Authorizations authorizations) throws IOException {
        for (OWLClass ontologyClass : o.getClassesInSignature()) {
            if (!o.isDeclared(ontologyClass, Imports.EXCLUDED)) {
                continue;
            }
            importOntologyClass(o, ontologyClass, inDir, authorizations);
        }
    }

    public OWLOntologyManager createOwlOntologyManager(OWLOntologyLoaderConfiguration config, IRI excludeDocumentIRI) throws Exception {
        OWLOntologyManager m = OWLManager.createOWLOntologyManager();
        config.setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT);
        loadOntologyFiles(m, config, excludeDocumentIRI);
        return m;
    }

    protected abstract void storeOntologyFile(InputStream inputStream, IRI documentIRI);

    public void exportOntology(OutputStream out, IRI documentIRI) throws Exception {
        OWLOntologyManager m = OWLManager.createOWLOntologyManager();
        OWLOntologyLoaderConfiguration config = new OWLOntologyLoaderConfiguration();
        config.setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT);

        List<OWLOntology> loadedOntologies = loadOntologyFiles(m, config, null);
        OWLOntology o = findOntology(loadedOntologies, documentIRI);
        if (o == null) {
            throw new VisalloException("Could not find ontology with iri " + documentIRI);
        }

        Writer fileWriter = new OutputStreamWriter(out);

        new OWLXMLRenderer().render(o, fileWriter);
    }

    protected abstract List<OWLOntology> loadOntologyFiles(OWLOntologyManager m, OWLOntologyLoaderConfiguration config, IRI excludedIRI) throws Exception;

    private OWLOntology findOntology(List<OWLOntology> loadedOntologies, IRI documentIRI) {
        for (OWLOntology o : loadedOntologies) {
            Optional<IRI> oIRI = o.getOntologyID().getOntologyIRI();
            if (oIRI.isPresent() && documentIRI.equals(oIRI.get())) {
                return o;
            }
        }
        return null;
    }

    protected Concept importOntologyClass(OWLOntology o, OWLClass ontologyClass, File inDir, Authorizations authorizations) throws IOException {
        String uri = ontologyClass.getIRI().toString();
        if ("http://www.w3.org/2002/07/owl#Thing".equals(uri)) {
            return getEntityConcept();
        }

        String label = getLabel(o, ontologyClass);
        checkNotNull(label, "label cannot be null or empty: " + uri);
        LOGGER.info("Importing ontology class " + uri + " (label: " + label + ")");

        Concept parent = getParentConcept(o, ontologyClass, inDir, authorizations);
        Concept result = getOrCreateConcept(parent, uri, label, inDir);

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
                setIconProperty(result, inDir, valueString, OntologyProperties.GLYPH_ICON.getPropertyName(), authorizations);
                continue;
            }

            if (annotationIri.equals(OntologyProperties.MAP_GLYPH_ICON_FILE_NAME.getPropertyName())) {
                setIconProperty(result, inDir, valueString, OntologyProperties.MAP_GLYPH_ICON.getPropertyName(), authorizations);
                continue;
            }

            if (annotationIri.equals(OntologyProperties.ADD_RELATED_CONCEPT_WHITE_LIST.getPropertyName())) {
                if (valueString.trim().length() == 0) {
                    continue;
                }
                result.setProperty(OntologyProperties.ADD_RELATED_CONCEPT_WHITE_LIST.getPropertyName(), valueString.trim(), authorizations);
                continue;
            }

            if (annotationIri.equals("http://www.w3.org/2000/01/rdf-schema#label")) {
                continue;
            }

            result.setProperty(annotationIri, valueString, authorizations);
        }

        return result;
    }

    protected void setIconProperty(Concept concept, File inDir, String glyphIconFileName, String propertyKey, Authorizations authorizations) throws IOException {
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

    protected Concept getParentConcept(OWLOntology o, OWLClass ontologyClass, File inDir, Authorizations authorizations) throws IOException {
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

    protected void importDataProperty(OWLOntology o, OWLDataProperty dataTypeProperty) {
        String propertyIRI = dataTypeProperty.getIRI().toString();
        try {
            String propertyDisplayName = getLabel(o, dataTypeProperty);
            PropertyType propertyType = getPropertyType(o, dataTypeProperty);
            boolean userVisible = getUserVisible(o, dataTypeProperty);
            boolean searchable = getSearchable(o, dataTypeProperty);
            boolean addable = getAddable(o, dataTypeProperty);
            String displayType = getDisplayType(o, dataTypeProperty);
            String propertyGroup = getPropertyGroup(o, dataTypeProperty);
            String validationFormula = getValidationFormula(o, dataTypeProperty);
            String displayFormula = getDisplayFormula(o, dataTypeProperty);
            ImmutableList<String> dependentPropertyIris = getDependentPropertyIris(o, dataTypeProperty);
            Double boost = getBoost(o, dataTypeProperty);
            String[] intents = getIntents(o, dataTypeProperty);
            if (propertyType == null) {
                throw new VisalloException("Could not get property type on data property " + propertyIRI);
            }

            List<Concept> domainConcepts = new ArrayList<>();
            for (OWLClassExpression domainClassExpr : EntitySearcher.getDomains(dataTypeProperty, o)) {
                OWLClass domainClass = domainClassExpr.asOWLClass();
                String domainClassUri = domainClass.getIRI().toString();
                Concept domainConcept = getConceptByIRI(domainClassUri);
                if (domainConcept == null) {
                    LOGGER.error("Could not find class with uri: %s", domainClassUri);
                } else {
                    LOGGER.info("Adding data property " + propertyIRI + " to class " + domainConcept.getIRI());
                    domainConcepts.add(domainConcept);
                }
            }

            Map<String, String> possibleValues = getPossibleValues(o, dataTypeProperty);
            Collection<TextIndexHint> textIndexHints = getTextIndexHints(o, dataTypeProperty);
            addPropertyTo(
                    domainConcepts,
                    propertyIRI,
                    propertyDisplayName,
                    propertyType,
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
        } catch (Throwable ex) {
            throw new VisalloException("Failed to load data property: " + propertyIRI, ex);
        }
    }

    protected abstract OntologyProperty addPropertyTo(
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
            String[] intents);

    protected void importObjectProperty(OWLOntology o, OWLObjectProperty objectProperty) {
        String iri = objectProperty.getIRI().toString();
        String label = getLabel(o, objectProperty);
        String[] intents = getIntents(o, objectProperty);
        boolean userVisible = getUserVisible(o, objectProperty);
        checkNotNull(label, "label cannot be null or empty for " + iri);
        LOGGER.info("Importing ontology object property " + iri + " (label: " + label + ")");

        getOrCreateRelationshipType(getDomainsConcepts(o, objectProperty), getRangesConcepts(o, objectProperty), iri, label, intents, userVisible);
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

    protected abstract void getOrCreateInverseOfRelationship(Relationship fromRelationship, Relationship inverseOfRelationship);

    private Iterable<Concept> getRangesConcepts(OWLOntology o, OWLObjectProperty objectProperty) {
        List<Concept> ranges = new ArrayList<>();
        for (OWLClassExpression rangeClassExpr : EntitySearcher.getRanges(objectProperty, o)) {
            OWLClass rangeClass = rangeClassExpr.asOWLClass();
            String rangeClassUri = rangeClass.getIRI().toString();
            Concept ontologyClass = getConceptByIRI(rangeClassUri);
            if (ontologyClass == null) {
                LOGGER.error("Could not find class with uri: %s", rangeClassUri);
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
            String rangeClassUri = rangeClass.getIRI().toString();
            Concept ontologyClass = getConceptByIRI(rangeClassUri);
            if (ontologyClass == null) {
                LOGGER.error("Could not find class with uri: %s", rangeClassUri);
            } else {
                domains.add(ontologyClass);
            }
        }
        return domains;
    }

    protected PropertyType getPropertyType(OWLOntology o, OWLDataProperty dataTypeProperty) {
        Collection<OWLDataRange> ranges = EntitySearcher.getRanges(dataTypeProperty, o);
        if (ranges.size() == 0) {
            return null;
        }
        if (ranges.size() > 1) {
            throw new VisalloException("Unexpected number of ranges on data property " + dataTypeProperty.getIRI().toString());
        }
        for (OWLDataRange range : ranges) {
            if (range instanceof OWLDatatype) {
                OWLDatatype datatype = (OWLDatatype) range;
                return getPropertyType(datatype.getIRI().toString());
            }
        }
        throw new VisalloException("Could not find range on data property " + dataTypeProperty.getIRI().toString());
    }

    private PropertyType getPropertyType(String iri) {
        if ("http://www.w3.org/2001/XMLSchema#string".equals(iri)) {
            return PropertyType.STRING;
        }
        if ("http://www.w3.org/2001/XMLSchema#dateTime".equals(iri)) {
            return PropertyType.DATE;
        }
        if ("http://www.w3.org/2001/XMLSchema#date".equals(iri)) {
            return PropertyType.DATE;
        }
        if ("http://www.w3.org/2001/XMLSchema#gYear".equals(iri)) {
            return PropertyType.DATE;
        }
        if ("http://www.w3.org/2001/XMLSchema#gYearMonth".equals(iri)) {
            return PropertyType.DATE;
        }
        if ("http://www.w3.org/2001/XMLSchema#int".equals(iri)) {
            return PropertyType.DOUBLE;
        }
        if ("http://www.w3.org/2001/XMLSchema#double".equals(iri)) {
            return PropertyType.DOUBLE;
        }
        if ("http://www.w3.org/2001/XMLSchema#float".equals(iri)) {
            return PropertyType.DOUBLE;
        }
        if ("http://visallo.org#geolocation".equals(iri)) {
            return PropertyType.GEO_LOCATION;
        }
        if ("http://visallo.org#currency".equals(iri)) {
            return PropertyType.CURRENCY;
        }
        if ("http://visallo.org#image".equals(iri)) {
            return PropertyType.IMAGE;
        }
        if ("http://www.w3.org/2001/XMLSchema#hexBinary".equals(iri)) {
            return PropertyType.BINARY;
        }
        if ("http://www.w3.org/2001/XMLSchema#boolean".equals(iri)) {
            return PropertyType.BOOLEAN;
        }
        if ("http://www.w3.org/2001/XMLSchema#integer".equals(iri)) {
            return PropertyType.INTEGER;
        }
        if ("http://www.w3.org/2001/XMLSchema#nonNegativeInteger".equals(iri)) {
            return PropertyType.INTEGER;
        }
        if ("http://www.w3.org/2001/XMLSchema#positiveInteger".equals(iri)) {
            return PropertyType.INTEGER;
        }
        if ("http://www.w3.org/2001/XMLSchema#unsignedLong".equals(iri)) {
            return PropertyType.INTEGER;
        }
        if ("http://www.w3.org/2001/XMLSchema#unsignedByte".equals(iri)) {
            return PropertyType.INTEGER;
        }
        throw new VisalloException("Unhandled property type " + iri);
    }

    public static String getLabel(OWLOntology o, OWLEntity owlEntity) {
        String bestLabel = owlEntity.getIRI().toString();
        for (OWLAnnotation annotation : EntitySearcher.getAnnotations(owlEntity, o)) {
            if (annotation.getProperty().isLabel()) {
                OWLLiteral value = (OWLLiteral) annotation.getValue();
                bestLabel = value.getLiteral();
                if (value.getLang().equals("") || value.getLang().equals("en")) {
                    return bestLabel;
                }
            }
        }
        return bestLabel;
    }

    protected String getDisplayType(OWLOntology o, OWLEntity owlEntity) {
        return getAnnotationValueByUri(o, owlEntity, OntologyProperties.DISPLAY_TYPE.getPropertyName());
    }

    protected String getPropertyGroup(OWLOntology o, OWLEntity owlEntity) {
        return getAnnotationValueByUri(o, owlEntity, OntologyProperties.PROPERTY_GROUP.getPropertyName());
    }

    protected String getValidationFormula(OWLOntology o, OWLEntity owlEntity) {
        return getAnnotationValueByUri(o, owlEntity, OntologyProperties.VALIDATION_FORMULA.getPropertyName());
    }

    protected String getDisplayFormula(OWLOntology o, OWLEntity owlEntity) {
        return getAnnotationValueByUri(o, owlEntity, OntologyProperties.DISPLAY_FORMULA.getPropertyName());
    }

    protected ImmutableList<String> getDependentPropertyIris(OWLOntology o, OWLEntity owlEntity) {
        List<String> results = new ArrayList<>();

        ImmutableList<String> dependentPropertyIris = getAnnotationValuesByUriOrNull(o, owlEntity, "http://visallo.org#dependentPropertyIris");
        addAllDependentPropertyIris(results, dependentPropertyIris);

        ImmutableList<String> dependentPropertyIri = getAnnotationValuesByUriOrNull(o, owlEntity, OntologyProperties.EDGE_LABEL_DEPENDENT_PROPERTY);
        addAllDependentPropertyIris(results, dependentPropertyIri);

        if (results.size() == 0) {
            return null;
        }
        return ImmutableList.copyOf(results);
    }

    private void addAllDependentPropertyIris(List<String> results, ImmutableList<String> dependentPropertyIris) {
        if (dependentPropertyIris == null) {
            return;
        }
        for (String dependentPropertyIri : dependentPropertyIris) {
            dependentPropertyIri = dependentPropertyIri.trim();
            if (dependentPropertyIri.startsWith("[")) {
                JSONArray array = new JSONArray(dependentPropertyIri);
                for (int i = 0; i < array.length(); i++) {
                    results.add(array.getString(i));
                }
            } else {
                results.add(dependentPropertyIri);
            }
        }
    }

    protected Double getBoost(OWLOntology o, OWLEntity owlEntity) {
        String val = getAnnotationValueByUri(o, owlEntity, OntologyProperties.BOOST.getPropertyName());
        if (val == null) {
            return null;
        }
        return Double.parseDouble(val);
    }

    public static String[] getIntents(OWLOntology o, OWLEntity owlEntity) {
        return getAnnotationValuesByUri(o, owlEntity, OntologyProperties.INTENT.getPropertyName());
    }

    protected boolean getUserVisible(OWLOntology o, OWLEntity owlEntity) {
        String val = getAnnotationValueByUri(o, owlEntity, OntologyProperties.USER_VISIBLE.getPropertyName());
        return val == null || Boolean.parseBoolean(val);
    }

    protected boolean getSearchable(OWLOntology o, OWLEntity owlEntity) {
        String val = getAnnotationValueByUri(o, owlEntity, "http://visallo.org#searchable");
        return val == null || Boolean.parseBoolean(val);
    }

    protected boolean getAddable(OWLOntology o, OWLEntity owlEntity) {
        String val = getAnnotationValueByUri(o, owlEntity, "http://visallo.org#addable");
        return val == null || Boolean.parseBoolean(val);
    }

    protected Map<String, String> getPossibleValues(OWLOntology o, OWLEntity owlEntity) {
        String val = getAnnotationValueByUri(o, owlEntity, OntologyProperties.POSSIBLE_VALUES.getPropertyName());
        if (val == null || val.trim().length() == 0) {
            return null;
        }
        return JSONUtil.toMap(new JSONObject(val));
    }

    protected Collection<TextIndexHint> getTextIndexHints(OWLOntology o, OWLDataProperty owlEntity) {
        return TextIndexHint.parse(getAnnotationValueByUri(o, owlEntity, OntologyProperties.TEXT_INDEX_HINTS.getPropertyName()));
    }

    protected String getAnnotationValueByUri(OWLOntology o, OWLEntity owlEntity, String uri) {
        for (OWLAnnotation annotation : EntitySearcher.getAnnotations(owlEntity, o)) {
            if (annotation.getProperty().getIRI().toString().equals(uri)) {
                OWLLiteral value = (OWLLiteral) annotation.getValue();
                return value.getLiteral();
            }
        }
        return null;
    }

    protected ImmutableList<String> getAnnotationValuesByUriOrNull(OWLOntology o, OWLEntity owlEntity, String uri) {
        List<String> values = new ArrayList<>();
        for (OWLAnnotation annotation : EntitySearcher.getAnnotations(owlEntity, o)) {
            if (annotation.getProperty().getIRI().toString().equals(uri)) {
                OWLLiteral value = (OWLLiteral) annotation.getValue();
                values.add(value.getLiteral());
            }
        }
        if (values.size() == 0) {
            return null;
        }
        return ImmutableList.copyOf(values);
    }

    protected static String[] getAnnotationValuesByUri(OWLOntology o, OWLEntity owlEntity, String uri) {
        List<String> results = new ArrayList<>();
        for (OWLAnnotation annotation : EntitySearcher.getAnnotations(owlEntity, o)) {
            if (annotation.getProperty().getIRI().toString().equals(uri)) {
                OWLLiteral value = (OWLLiteral) annotation.getValue();
                results.add(value.getLiteral());
            }
        }
        return results.toArray(new String[results.size()]);
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

        List<OntologyProperty> properties = findLoadedPropertiesByIntent(intent);
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
    public <T extends VisalloProperty> T getRequiredVisalloPropertyByIntent(String intent, Class<T> visalloPropertyType) {
        T result = getVisalloPropertyByIntent(intent, visalloPropertyType);
        if (result == null) {
            throw new VisalloException("Could not find property by intent: " + intent);
        }
        return result;
    }

    private List<OntologyProperty> findLoadedPropertiesByIntent(String intent) {
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
                new Callable<Object>() {
                    @Override
                    public Object call() {
                        Iterable<Concept> concepts = getConceptsWithProperties();
                        return Concept.toClientApiConcepts(concepts);
                    }
                },
                new Callable<Object>() {
                    @Override
                    public Object call() {
                        Iterable<OntologyProperty> properties = getProperties();
                        return OntologyProperty.toClientApiProperties(properties);
                    }
                },
                new Callable<Object>() {
                    @Override
                    public Object call() {
                        Iterable<Relationship> relationships = getRelationships();
                        return Relationship.toClientApiRelationships(relationships);
                    }
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

    protected void definePropertyOnGraph(Graph graph, String propertyIri, PropertyType dataType, Collection<TextIndexHint> textIndexHints, Double boost) {
        DefinePropertyBuilder definePropertyBuilder = graph.defineProperty(propertyIri);
        definePropertyBuilder.dataType(PropertyType.getTypeClass(dataType));
        if (dataType == PropertyType.STRING) {
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

    protected boolean determineSearchable(String propertyIri, PropertyType dataType, Collection<TextIndexHint> textIndexHints, boolean searchable) {
        if (dataType == PropertyType.STRING) {
            if (searchable && (textIndexHints.isEmpty() || textIndexHints.equals(TextIndexHint.NONE))) {
                searchable = false;
            } else if (!searchable && (!textIndexHints.isEmpty() || !textIndexHints.equals(TextIndexHint.NONE))) {
                throw new VisalloException("textIndexHints should not be specified for non-searchable string property: " + propertyIri);
            }
        }
        return searchable;
    }
}
