package org.visallo.common.rdf;

import org.apache.commons.lang.StringUtils;
import org.vertexium.Visibility;
import org.vertexium.property.StreamingPropertyValue;
import org.vertexium.type.GeoPoint;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.security.VisibilityTranslator;
import org.visallo.core.util.VisalloDate;
import org.visallo.core.util.VisalloDateTime;

import java.io.*;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class VisalloRdfTriple {
    public static final String LABEL_CONCEPT_TYPE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
    public static final String MULTI_KEY = RdfTripleImportHelper.class.getSimpleName();
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
    private static final Pattern VISIBILITY_PATTERN = Pattern.compile("(.*)\\[(.*)\\]");
    private static final Pattern METADATA_PATTERN = Pattern.compile("(.*)@(.*)");
    private static final Pattern PROPERTY_KEY_PATTERN = Pattern.compile("(.*#.*):(.*)");
    private static final Map<String, Visibility> visibilityCache = new HashMap<>();

    public static VisalloRdfTriple parse(
            RdfTriple rdfTriple,
            String defaultVisibilitySource,
            VisibilityTranslator visibilityTranslator,
            File workingDir,
            TimeZone timeZone
    ) {
        if (!(rdfTriple.getFirst() instanceof RdfTriple.UriPart)) {
            throw new VisalloException("Unexpected first part of RDF triple. Expected UriPart but was " + rdfTriple.getFirst().getClass().getName());
        }
        if (!(rdfTriple.getSecond() instanceof RdfTriple.UriPart)) {
            throw new VisalloException("Unexpected second part of RDF triple. Expected UriPart but was " + rdfTriple.getSecond().getClass().getName());
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
        Visibility vertexVisibility = getVisibility(vertexVisibilitySource, visibilityTranslator);

        if (label.equals(LABEL_CONCEPT_TYPE)) {
            return parseConceptTypeTriple(vertexId, vertexVisibility, vertexVisibilitySource, third);
        }

        if (third instanceof RdfTriple.LiteralPart) {
            return parsePropertyTriple(
                    vertexId,
                    vertexVisibility,
                    vertexVisibilitySource,
                    label,
                    (RdfTriple.LiteralPart) third,
                    defaultVisibilitySource,
                    workingDir,
                    timeZone,
                    visibilityTranslator
            );
        }

        if (third instanceof RdfTriple.UriPart) {
            return parseAddEdgeTriple(
                    vertexId,
                    label,
                    (RdfTriple.UriPart) third,
                    defaultVisibilitySource,
                    visibilityTranslator
            );
        }

        return null;
    }

    private static VisalloRdfTriple parseConceptTypeTriple(
            String vertexId,
            Visibility vertexVisibility,
            String vertexVisibilitySource,
            RdfTriple.Part third
    ) {
        return new ConceptTypeVisalloRdfTriple(
                vertexId,
                vertexVisibility,
                vertexVisibilitySource,
                getConceptType(third)
        );
    }

    private static VisalloRdfTriple parsePropertyTriple(
            String vertexId,
            Visibility vertexVisibility,
            String vertexVisibilitySource,
            String label,
            RdfTriple.LiteralPart propertyValuePart,
            String defaultVisibilitySource,
            File workingDir,
            TimeZone timeZone,
            VisibilityTranslator visibilityTranslator
    ) {
        String metadataKey = null;
        String propertyKey = MULTI_KEY;

        Matcher metadataMatcher = METADATA_PATTERN.matcher(label);
        if (metadataMatcher.matches()) {
            label = metadataMatcher.group(1);
            metadataKey = metadataMatcher.group(2);
        }

        // visibility
        String propertyVisibilitySource;
        Matcher visibilityMatch = VISIBILITY_PATTERN.matcher(label);
        if (visibilityMatch.matches()) {
            label = visibilityMatch.group(1);
            propertyVisibilitySource = visibilityMatch.group(2);
        } else {
            propertyVisibilitySource = defaultVisibilitySource;
        }
        Visibility propertyVisibility = getVisibility(propertyVisibilitySource, visibilityTranslator);

        // property key
        Matcher keyMatch = PROPERTY_KEY_PATTERN.matcher(label);
        if (keyMatch.matches()) {
            label = keyMatch.group(1);
            propertyKey = keyMatch.group(2);
        }

        String propertyName = label;
        Object value = getPropertyValue(propertyValuePart, workingDir, timeZone);

        if (metadataKey != null) {
            String metadataName;
            Visibility metadataVisibility;

            visibilityMatch = VISIBILITY_PATTERN.matcher(metadataKey);
            if (visibilityMatch.matches()) {
                metadataName = visibilityMatch.group(1);
                metadataVisibility = getVisibility(visibilityMatch.group(2), visibilityTranslator);
            } else {
                metadataName = metadataKey;
                metadataVisibility = propertyVisibility;
            }

            return new SetMetadataVisalloRdfTriple(
                    vertexId,
                    vertexVisibility,
                    vertexVisibilitySource,
                    propertyKey,
                    propertyName,
                    propertyVisibility,
                    propertyVisibilitySource,
                    metadataName,
                    metadataVisibility,
                    value
            );
        } else {
            return new SetPropertyVisalloRdfTriple(
                    vertexId,
                    vertexVisibility,
                    vertexVisibilitySource,
                    propertyKey,
                    propertyName,
                    propertyVisibility,
                    propertyVisibilitySource,
                    value
            );
        }
    }

    private static VisalloRdfTriple parseAddEdgeTriple(String outVertexId, String label, RdfTriple.UriPart third, String defaultVisibilitySource, VisibilityTranslator visibilityTranslator) {
        String inVertexId = third.getUri();
        String edgeId = outVertexId + "_" + label + "_" + inVertexId;

        Visibility visibility;
        Matcher visibilityMatcher = VISIBILITY_PATTERN.matcher(label);
        if (visibilityMatcher.matches()) {
            label = visibilityMatcher.group(1);
            visibility = getVisibility(visibilityMatcher.group(2), visibilityTranslator);
        } else {
            visibility = getVisibility(defaultVisibilitySource, visibilityTranslator);
        }

        return new AddEdgeVisalloRdfTriple(
                edgeId,
                outVertexId,
                inVertexId,
                label,
                visibility
        );
    }

    private static String getConceptType(RdfTriple.Part third) {
        if (third instanceof RdfTriple.UriPart) {
            return ((RdfTriple.UriPart) third).getUri();
        }

        if (third instanceof RdfTriple.LiteralPart) {
            return ((RdfTriple.LiteralPart) third).getString();
        }

        throw new VisalloException("Unhandled part type: " + third.getClass().getName());
    }

    private static Visibility getVisibility(String visibilityString, VisibilityTranslator visibilityTranslator) {
        Visibility visibility = visibilityCache.get(visibilityString);
        if (visibility != null) {
            return visibility;
        }
        visibility = visibilityTranslator.toVisibility(visibilityString).getVisibility();
        visibilityCache.put(visibilityString, visibility);
        return visibility;
    }

    private static Object getPropertyValue(RdfTriple.LiteralPart propertyValuePart, File workingDir, TimeZone timeZone) {
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


    private static boolean isPropertyValueValid(RdfTriple.LiteralPart propertyValuePart) {
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

    private static Date parseDate(String dateString) {
        VisalloDate visalloDate = VisalloDate.create(dateString);
        return visalloDate.toDate();
    }

    private static Date parseDateTime(String dateTimeString, TimeZone timeZone) {
        VisalloDateTime visalloDateTime = VisalloDateTime.create(dateTimeString, timeZone);
        return visalloDateTime.toDateGMT();
    }
}
