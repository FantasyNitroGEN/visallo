package org.visallo.common.rdf;

import com.google.inject.Inject;
import org.apache.commons.lang.StringUtils;
import org.vertexium.*;
import org.vertexium.mutation.ElementMutation;
import org.vertexium.mutation.ExistingElementMutation;
import org.vertexium.property.StreamingPropertyValue;
import org.vertexium.type.GeoPoint;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.security.VisalloVisibility;
import org.visallo.core.util.VisalloDate;
import org.visallo.core.util.VisalloDateTime;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.clientapi.model.VisibilityJson;

import java.io.*;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
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
    private final Pattern METADATA_PATTERN = Pattern.compile("(.*)@(.*)");
    private final Pattern VISIBILITY_PATTERN = Pattern.compile("(.*)\\[(.*)\\]");
    private final Pattern PROPERTY_KEY_PATTERN = Pattern.compile("(.*#.*):(.*)");
    private final Map<String, Visibility> visibilityCache = new HashMap<>();

    public void setFailOnFirstError(boolean failOnFirstError) {
        this.failOnFirstError = failOnFirstError;
    }

    private boolean failOnFirstError = false;

    @Inject
    public RdfTripleImportHelper(Graph graph) {
        this.graph = graph;
    }

    public void importRdfTriple(
            File inputFile,
            Metadata metadata,
            TimeZone timeZone,
            Visibility defaultVisibility,
            Authorizations authorizations
    ) throws IOException {
        importRdfTriple(
                new FileInputStream(inputFile),
                metadata,
                inputFile.getParentFile(),
                timeZone,
                defaultVisibility,
                authorizations
        );
    }

    private void importRdfTriple(
            InputStream inputStream,
            Metadata metadata,
            File workingDir,
            TimeZone timeZone,
            Visibility defaultVisibility,
            Authorizations authorizations
    ) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        int lineNum = 1;
        String line;
        while ((line = reader.readLine()) != null) {
            LOGGER.debug("Importing RDF triple on line: %d", lineNum);
            try {
                importRdfLine(line, metadata, workingDir, timeZone, defaultVisibility, authorizations);
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
    }

    public void importRdfLine(
            String line,
            Metadata metadata,
            File workingDir,
            TimeZone timeZone,
            Visibility defaultVisibility,
            Authorizations authorizations
    ) {
        if (line.length() == 0 || line.charAt(0) == '#') {
            return;
        }
        RdfTriple rdfTriple = RdfTripleParser.parseLine(line);
        if (importRdfTriple(rdfTriple, metadata, workingDir, timeZone, defaultVisibility, authorizations)) {
            return;
        }

        throw new VisalloException("Unhandled combination of RDF triples");
    }

    public boolean importRdfTriple(
            RdfTriple rdfTriple,
            Metadata metadata,
            File workingDir,
            TimeZone timeZone,
            Visibility defaultVisibility,
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
        Visibility vertexVisibility = defaultVisibility;

        Matcher visibilityMatcher = VISIBILITY_PATTERN.matcher(vertexId);
        if (visibilityMatcher.matches()) {
            vertexId = visibilityMatcher.group(1);
            vertexVisibility = getVisibility(visibilityMatcher.group(2));
        }

        if (label.equals(LABEL_CONCEPT_TYPE)) {
            setConceptType(vertexId, third, metadata, vertexVisibility, authorizations);
            return true;
        }

        if (third instanceof RdfTriple.LiteralPart) {
            setProperty(vertexId, vertexVisibility, label, (RdfTriple.LiteralPart) third, metadata, workingDir, timeZone, defaultVisibility, authorizations);
            return true;
        }

        if (third instanceof RdfTriple.UriPart) {
            addEdge(vertexId, label, ((RdfTriple.UriPart) third).getUri(), defaultVisibility, authorizations);
            return true;
        }

        return false;
    }

    private void addEdge(
            String outVertexId,
            String label,
            String inVertexId,
            Visibility visibility,
            Authorizations authorizations
    ) {
        String edgeId = outVertexId + "_" + label + "_" + inVertexId;

        Matcher visibilityMatcher = VISIBILITY_PATTERN.matcher(label);
        if (visibilityMatcher.matches()) {
            label = visibilityMatcher.group(1);
            visibility = getVisibility(visibilityMatcher.group(2));
        }

        graph.addEdge(edgeId, outVertexId, inVertexId, label, visibility, authorizations);
    }

    private void setProperty(
            String vertexId,
            Visibility elementVisibility,
            String label,
            RdfTriple.LiteralPart propertyValuePart,
            Metadata metadata,
            File workingDir,
            TimeZone timeZone,
            Visibility visibility,
            Authorizations authorizations
    ) {
        ElementMutation m;
        String propertyKey = MULTI_KEY;
        Visibility propertyVisibility = visibility;
        String metadataKey = null;

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

        Matcher visibilityMatch = VISIBILITY_PATTERN.matcher(label);
        if (visibilityMatch.matches()) {
            label = visibilityMatch.group(1);
            propertyVisibility = getVisibility(visibilityMatch.group(2));
        }

        Matcher keyMatch = PROPERTY_KEY_PATTERN.matcher(label);
        if (keyMatch.matches()) {
            label = keyMatch.group(1);
            propertyKey = keyMatch.group(2);
        }

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
        m.save(authorizations);
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
                if (StringUtils.isBlank(propertyValuePart.getString())) {
                    return false;
                } else {
                    return true;
                }
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

    private void setConceptType(String vertexId, RdfTriple.Part third, Metadata metadata, Visibility visibility, Authorizations authorizations) {
        VertexBuilder m = graph.prepareVertex(vertexId, visibility);
        String conceptType = getConceptType(third);
        VisibilityJson visibilityJson = new VisibilityJson(visibility.getVisibilityString());
        VisalloProperties.CONCEPT_TYPE.setProperty(m, conceptType, metadata, visibility);
        VisalloProperties.VISIBILITY_JSON.setProperty(m, visibilityJson, metadata, visibility);
        m.save(authorizations);
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
        visibility = new VisalloVisibility(visibilityString).getVisibility();
        visibilityCache.put(visibilityString, visibility);
        return visibility;
    }
}
