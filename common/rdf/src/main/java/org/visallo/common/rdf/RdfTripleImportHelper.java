package org.visallo.common.rdf;

import com.google.inject.Inject;
import org.apache.commons.lang.StringUtils;
import org.vertexium.*;
import org.vertexium.mutation.ElementMutation;
import org.vertexium.mutation.ExistingElementMutation;
import org.vertexium.property.StreamingPropertyValue;
import org.vertexium.type.GeoPoint;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.graph.GraphRepository;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.security.VisibilityTranslator;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloDate;
import org.visallo.core.util.VisalloDateTime;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.clientapi.model.VisibilityJson;

import java.io.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;

public class RdfTripleImportHelper {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(RdfTripleImportHelper.class);
    public static final String MULTI_KEY = RdfTripleImportHelper.class.getSimpleName();
    public static final String LABEL_CONCEPT_TYPE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
    public static final String PROPERTY_TYPE_GEOLOCATION = "http://visallo.org#geolocation";
    public static final String PROPERTY_TYPE_DIRECTORY_ENTITY = "http://visallo.org#directory/entity";
    public static final String PROPERTY_TYPE_STREAMING_PROPERTY_VALUE = "http://visallo.org#streamingPropertyValue";
    public static final String PROPERTY_TYPE_STREAMING_PROPERTY_VALUE_INLINE = "http://visallo.org#streamingPropertyValueInline";
    public static final String PROPERTY_TYPE_DATE = "http://www.w3.org/2001/XMLSchema#date";
    public static final String PROPERTY_TYPE_DATE_TIME = "http://www.w3.org/2001/XMLSchema#dateTime";
    public static final String PROPERTY_TYPE_YEAR = "http://www.w3.org/2001/XMLSchema#gYear";
    public static final String PROPERTY_TYPE_MONTH_DAY = "http://www.w3.org/2001/XMLSchema#gMonthDay";
    public static final String PROPERTY_TYPE_STRING = "http://www.w3.org/2001/XMLSchema#string";
    public static final String PROPERTY_TYPE_BOOLEAN = "http://www.w3.org/2001/XMLSchema#boolean";
    public static final String PROPERTY_TYPE_DOUBLE = "http://www.w3.org/2001/XMLSchema#double";
    public static final String PROPERTY_TYPE_CURRENCY = "http://visallo.org#currency";
    public static final String PROPERTY_TYPE_INT = "http://www.w3.org/2001/XMLSchema#int";
    public static final String PROPERTY_TYPE_INTEGER = "http://www.w3.org/2001/XMLSchema#integer";
    private final Graph graph;
    private final VisibilityTranslator visibilityTranslator;
    private final Pattern METADATA_PATTERN = Pattern.compile("(.*)@(.*)");
    private final Pattern VISIBILITY_PATTERN = Pattern.compile("(.*)\\[(.*)\\]");
    private final Pattern PROPERTY_KEY_PATTERN = Pattern.compile("(.*#.*):(.*)");
    private final Map<String, Visibility> visibilityCache = new HashMap<>();
    private WorkQueueRepository workQueueRepository;

    public void setFailOnFirstError(boolean failOnFirstError) {
        this.failOnFirstError = failOnFirstError;
    }

    private boolean failOnFirstError = false;

    @Inject
    public RdfTripleImportHelper(
            Graph graph,
            VisibilityTranslator visibilityTranslator,
            WorkQueueRepository workQueueRepository
    ) {
        this.graph = graph;
        this.visibilityTranslator = visibilityTranslator;
        this.workQueueRepository = workQueueRepository;
    }

    @Deprecated
    public void importRdfTriple(
            File inputFile,
            TimeZone timeZone,
            String defaultVisibilitySource,
            User user,
            Authorizations authorizations
    ) throws IOException {
        importRdfTriple(
                inputFile.getName(),
                new FileInputStream(inputFile),
                inputFile.getParentFile(),
                timeZone,
                Priority.NORMAL,
                defaultVisibilitySource,
                user,
                authorizations
        );
    }

    public void importRdfTriple(
            File inputFile,
            TimeZone timeZone,
            Priority priority,
            String defaultVisibilitySource,
            User user,
            Authorizations authorizations
    ) throws IOException {
        importRdfTriple(
                inputFile.getName(),
                new FileInputStream(inputFile),
                inputFile.getParentFile(),
                timeZone,
                priority,
                defaultVisibilitySource,
                user,
                authorizations
        );
    }

    private void importRdfTriple(
            String sourceFileName,
            InputStream inputStream,
            File workingDir,
            TimeZone timeZone,
            Priority priority,
            String defaultVisibilitySource,
            User user,
            Authorizations authorizations
    ) throws IOException {
        Set<Element> elements = new HashSet<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        int lineNum = 1;
        String line;
        while ((line = reader.readLine()) != null) {
            LOGGER.debug("Importing RDF triple on line: %d", lineNum);
            try {
                importRdfLine(elements, sourceFileName, line, workingDir, timeZone, defaultVisibilitySource, user, authorizations);
            } catch (Exception e) {
                String errMsg = String.format("Error importing RDF triple on line: %d. %s", lineNum, e.getMessage());
                if (failOnFirstError) {
                    throw new VisalloException(errMsg);
                } else {
                    // log the error and continue processing
                    LOGGER.error(errMsg, e);
                }
            }

            ++lineNum;
        }

        graph.flush();
        LOGGER.info("pushing %d elements from RDF import on to work queue", elements.size());
        workQueueRepository.pushElements(elements, priority);
    }

    public void importRdfLine(
            Set<Element> elements,
            String sourceFileName,
            String line,
            File workingDir,
            TimeZone timeZone,
            String defaultVisibilitySource,
            User user,
            Authorizations authorizations
    ) {
        if (line.length() == 0 || line.charAt(0) == '#') {
            return;
        }
        RdfTriple rdfTriple = RdfTripleParser.parseLine(line);
        if (importRdfTriple(elements, rdfTriple, sourceFileName, workingDir, timeZone, defaultVisibilitySource, user, authorizations)) {
            return;
        }

        throw new VisalloException("Unhandled combination of RDF triples");
    }

    public boolean importRdfTriple(
            Set<Element> elements,
            RdfTriple rdfTriple,
            String sourceFileName,
            File workingDir,
            TimeZone timeZone,
            String defaultVisibilitySource,
            User user,
            Authorizations authorizations
    ) {
        if (!(rdfTriple.getFirst() instanceof RdfTriple.UriPart)) {
            return true;
        }
        if (!(rdfTriple.getSecond() instanceof RdfTriple.UriPart)) {
            return true;
        }

        String vertexId = ((RdfTriple.UriPart) rdfTriple.getFirst()).getUri();
        String label = ((RdfTriple.UriPart) rdfTriple.getSecond()).getUri();
        RdfTriple.Part third = rdfTriple.getThird();

        String vertexVisibilitySource;
        Matcher visibilityMatcher = VISIBILITY_PATTERN.matcher(vertexId);
        if (visibilityMatcher.matches()) {
            vertexId = visibilityMatcher.group(1);
            vertexVisibilitySource = visibilityMatcher.group(2);
        } else {
            vertexVisibilitySource = defaultVisibilitySource;
        }
        Visibility vertexVisibility = getVisibility(vertexVisibilitySource);

        if (label.equals(LABEL_CONCEPT_TYPE)) {
            setConceptType(elements, sourceFileName, vertexId, third, vertexVisibility, vertexVisibilitySource, user, authorizations);
            return true;
        }

        if (third instanceof RdfTriple.LiteralPart) {
            setProperty(
                    elements,
                    sourceFileName,
                    vertexId,
                    vertexVisibility,
                    label,
                    (RdfTriple.LiteralPart) third,
                    workingDir,
                    timeZone,
                    defaultVisibilitySource,
                    user,
                    authorizations
            );
            return true;
        }

        if (third instanceof RdfTriple.UriPart) {
            addEdge(elements, vertexId, label, ((RdfTriple.UriPart) third).getUri(), defaultVisibilitySource, authorizations);
            return true;
        }

        return false;
    }

    private void addEdge(
            Set<Element> elements,
            String outVertexId,
            String label,
            String inVertexId,
            String defaultVisibilitySource,
            Authorizations authorizations
    ) {
        String edgeId = outVertexId + "_" + label + "_" + inVertexId;

        Visibility visibility;
        Matcher visibilityMatcher = VISIBILITY_PATTERN.matcher(label);
        if (visibilityMatcher.matches()) {
            label = visibilityMatcher.group(1);
            visibility = getVisibility(visibilityMatcher.group(2));
        } else {
            visibility = getVisibility(defaultVisibilitySource);
        }

        Edge edge = graph.addEdge(edgeId, outVertexId, inVertexId, label, visibility, authorizations);
        elements.add(edge);
    }

    private void setProperty(
            Set<Element> elements,
            String sourceFileName,
            String vertexId,
            Visibility elementVisibility,
            String label,
            RdfTriple.LiteralPart propertyValuePart,
            File workingDir,
            TimeZone timeZone,
            String defaultVisibilitySource,
            User user,
            Authorizations authorizations
    ) {
        ElementMutation m;
        String propertyKey = MULTI_KEY;
        String metadataKey = null;

        // metadata
        Date now = new Date();
        Metadata metadata = new Metadata();
        Visibility defaultVisibility = visibilityTranslator.getDefaultVisibility();
        if (sourceFileName != null) {
            VisalloProperties.SOURCE_FILE_NAME_METADATA.setMetadata(metadata, sourceFileName, defaultVisibility);
        }
        VisalloProperties.MODIFIED_DATE_METADATA.setMetadata(metadata, now, defaultVisibility);
        VisalloProperties.MODIFIED_BY_METADATA.setMetadata(metadata, user.getUserId(), defaultVisibility);
        VisalloProperties.CONFIDENCE_METADATA.setMetadata(metadata, GraphRepository.SET_PROPERTY_CONFIDENCE, defaultVisibility);

        // metadata property
        Matcher metadataMatcher = METADATA_PATTERN.matcher(label);
        if (metadataMatcher.matches()) {
            label = metadataMatcher.group(1);
            metadataKey = metadataMatcher.group(2);
            Vertex v = graph.getVertex(vertexId, authorizations);
            if (v == null) {
                graph.flush();
                v = graph.getVertex(vertexId, authorizations);
            }
            checkNotNull(v, "Could not find vertex with id " + vertexId + " to update metadata");
            m = v.prepareMutation();
        } else {
            m = graph.prepareVertex(vertexId, elementVisibility);
        }

        // visibility
        String visibilitySource;
        Matcher visibilityMatch = VISIBILITY_PATTERN.matcher(label);
        if (visibilityMatch.matches()) {
            label = visibilityMatch.group(1);
            visibilitySource = visibilityMatch.group(2);
        } else {
            visibilitySource = defaultVisibilitySource;
        }
        Visibility propertyVisibility = getVisibility(visibilitySource);
        VisalloProperties.VISIBILITY_JSON_METADATA.setMetadata(metadata, new VisibilityJson(visibilitySource), defaultVisibility);

        // property key
        Matcher keyMatch = PROPERTY_KEY_PATTERN.matcher(label);
        if (keyMatch.matches()) {
            label = keyMatch.group(1);
            propertyKey = keyMatch.group(2);
        }

        // save
        String propertyName = label;
        Object propertyValue = getPropertyValue(propertyValuePart, workingDir, timeZone);

        if (metadataKey != null) {
            String metadataName;
            Visibility metadataVisibility;

            visibilityMatch = VISIBILITY_PATTERN.matcher(metadataKey);
            if (visibilityMatch.matches()) {
                metadataName = visibilityMatch.group(1);
                metadataVisibility = getVisibility(visibilityMatch.group(2));
            } else {
                metadataName = metadataKey;
                metadataVisibility = propertyVisibility;
            }

            ((ExistingElementMutation) m).setPropertyMetadata(propertyKey, propertyName, metadataName, propertyValue, metadataVisibility);
        } else {
            m.addPropertyValue(propertyKey, propertyName, propertyValue, metadata, propertyVisibility);
        }
        Element element = m.save(authorizations);
        elements.add(element);
    }

    private Object getPropertyValue(RdfTriple.LiteralPart propertyValuePart, File workingDir, TimeZone timeZone) {
        if (propertyValuePart.getType() == null) {
            return propertyValuePart.getString();
        }

        if (!isPropertyValueValid(propertyValuePart)) {
            throw new VisalloException("Invalid or missing property value. " + propertyValuePart);
        }

        String typeUri = propertyValuePart.getType().getUri();
        switch (typeUri) {
            case PROPERTY_TYPE_DATE:
                return parseDate(propertyValuePart.getString());
            case PROPERTY_TYPE_DATE_TIME:
                return parseDateTime(propertyValuePart.getString(), timeZone);
            case PROPERTY_TYPE_YEAR:
                return Integer.parseInt(propertyValuePart.getString());
            case PROPERTY_TYPE_MONTH_DAY:
                return propertyValuePart.getString(); // TODO: is there a better format for this.
            case PROPERTY_TYPE_STRING:
                return propertyValuePart.getString();
            case PROPERTY_TYPE_BOOLEAN:
                return Boolean.parseBoolean(propertyValuePart.getString());
            case PROPERTY_TYPE_DOUBLE:
                return Double.parseDouble(propertyValuePart.getString());
            case PROPERTY_TYPE_CURRENCY:
                return new BigDecimal(propertyValuePart.getString());
            case PROPERTY_TYPE_INT:
            case PROPERTY_TYPE_INTEGER:
                return Integer.parseInt(propertyValuePart.getString());
            case PROPERTY_TYPE_GEOLOCATION:
                return GeoPoint.parse(propertyValuePart.getString());
            case PROPERTY_TYPE_DIRECTORY_ENTITY:
                return propertyValuePart.getString();
            case PROPERTY_TYPE_STREAMING_PROPERTY_VALUE:
                String path = propertyValuePart.getString();
                File file;
                if (new File(path).isAbsolute()) {
                    file = new File(path);
                } else {
                    file = new File(workingDir, path);
                }
                if (!file.exists()) {
                    throw new VisalloException("File not found: " + file.getAbsolutePath());
                }
                try {
                    InputStream in = new FileInputStream(file);
                    StreamingPropertyValue spv = new StreamingPropertyValue(in, byte[].class);
                    spv.store(true);
                    spv.searchIndex(false);
                    return spv;
                } catch (FileNotFoundException ex) {
                    throw new VisalloException("Could not read file: " + file.getAbsolutePath(), ex);
                }
            case PROPERTY_TYPE_STREAMING_PROPERTY_VALUE_INLINE:
                InputStream in = new ByteArrayInputStream(propertyValuePart.getString().getBytes());
                StreamingPropertyValue spv = new StreamingPropertyValue(in, byte[].class);
                spv.store(true);
                spv.searchIndex(false);
                return spv;
            default:
                throw new VisalloException("Unhandled property type: " + propertyValuePart.getType().getUri() + " (value: " + propertyValuePart.getString() + ")");
        }
    }

    private boolean isPropertyValueValid(RdfTriple.LiteralPart propertyValuePart) {
        if (propertyValuePart.getType() == null) {
            return false;
        }

        String typeUri = propertyValuePart.getType().getUri();
        switch (typeUri) {
            case PROPERTY_TYPE_DATE:
            case PROPERTY_TYPE_DATE_TIME:
            case PROPERTY_TYPE_MONTH_DAY:
            case PROPERTY_TYPE_YEAR:
            case PROPERTY_TYPE_CURRENCY:
            case PROPERTY_TYPE_DOUBLE:
            case PROPERTY_TYPE_BOOLEAN:
                // blank values are not valid for these data types
                return !StringUtils.isBlank(propertyValuePart.getString());
            default:
                return true;
        }
    }

    private Date parseDate(String dateString) {
        VisalloDate visalloDate = VisalloDate.create(dateString);
        return visalloDate.toDate();
    }

    private Date parseDateTime(String dateTimeString, TimeZone timeZone) {
        VisalloDateTime visalloDateTime = VisalloDateTime.create(dateTimeString, timeZone);
        return visalloDateTime.toDateGMT();
    }

    private void setConceptType(
            Set<Element> elements,
            String sourceFileName,
            String vertexId,
            RdfTriple.Part third,
            Visibility visibility,
            String vertexVisibilitySource,
            User user,
            Authorizations authorizations
    ) {
        Date now = new Date();
        Metadata metadata = new Metadata();
        Visibility defaultVisibility = visibilityTranslator.getDefaultVisibility();
        if (sourceFileName != null) {
            VisalloProperties.SOURCE_FILE_NAME_METADATA.setMetadata(metadata, sourceFileName, defaultVisibility);
        }
        VisalloProperties.VISIBILITY_JSON_METADATA.setMetadata(metadata, new VisibilityJson(vertexVisibilitySource), defaultVisibility);
        VisalloProperties.MODIFIED_DATE_METADATA.setMetadata(metadata, now, defaultVisibility);
        VisalloProperties.MODIFIED_BY_METADATA.setMetadata(metadata, user.getUserId(), defaultVisibility);
        VisalloProperties.CONFIDENCE_METADATA.setMetadata(metadata, GraphRepository.SET_PROPERTY_CONFIDENCE, defaultVisibility);

        VertexBuilder m = graph.prepareVertex(vertexId, visibility);
        String conceptType = getConceptType(third);
        VisibilityJson visibilityJson = new VisibilityJson(vertexVisibilitySource);
        VisalloProperties.CONCEPT_TYPE.setProperty(m, conceptType, metadata, visibility);
        VisalloProperties.VISIBILITY_JSON.setProperty(m, visibilityJson, metadata, visibility);
        Vertex vertex = m.save(authorizations);
        elements.add(vertex);
    }

    private String getConceptType(RdfTriple.Part third) {
        if (third instanceof RdfTriple.UriPart) {
            return ((RdfTriple.UriPart) third).getUri();
        }

        if (third instanceof RdfTriple.LiteralPart) {
            return ((RdfTriple.LiteralPart) third).getString();
        }

        throw new VisalloException("Unhandled part type: " + third.getClass().getName());
    }

    private Visibility getVisibility(String visibilityString) {
        Visibility visibility = visibilityCache.get(visibilityString);
        if (visibility != null) {
            return visibility;
        }
        visibility = visibilityTranslator.toVisibility(visibilityString).getVisibility();
        visibilityCache.put(visibilityString, visibility);
        return visibility;
    }
}
