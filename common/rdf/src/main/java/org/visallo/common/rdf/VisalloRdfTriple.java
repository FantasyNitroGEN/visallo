package org.visallo.common.rdf;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.vertexium.Authorizations;
import org.vertexium.ElementType;
import org.vertexium.property.StreamingPropertyValue;
import org.vertexium.type.GeoPoint;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.util.VisalloDate;
import org.visallo.core.util.VisalloDateTime;

import java.io.*;
import java.math.BigDecimal;
import java.util.Date;
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
    public static final String PROPERTY_TYPE_STREAMING_PROPERTY_VALUE_INLINE_BASE64 = "http://visallo.org#streamingPropertyValueInlineBase64";
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
    private static final Pattern ELEMENT_TYPE_PATTERN = Pattern.compile("(EDGE|VERTEX):(.*)");
    private static final Pattern VISIBILITY_PATTERN = Pattern.compile("(.*)\\[(.*)\\]");
    private static final Pattern METADATA_PATTERN = Pattern.compile("(.*)(?<!\\\\)@(.*)");
    private static final Pattern PROPERTY_KEY_PATTERN = Pattern.compile("(.*#.*)(?<!\\\\):(.*)");
    private static final Pattern EDGE_ID_PATTERN = Pattern.compile("(.*#.*)(?<!\\\\):(.*)");

    public static VisalloRdfTriple parse(
            RdfTriple rdfTriple,
            String defaultVisibilitySource,
            File workingDir,
            TimeZone timeZone
    ) {
        if (!(rdfTriple.getFirst() instanceof RdfTriple.UriPart)) {
            throw new VisalloException("Unexpected first part of RDF triple. Expected UriPart but was " + rdfTriple.getFirst().getClass().getName());
        }
        if (!(rdfTriple.getSecond() instanceof RdfTriple.UriPart)) {
            throw new VisalloException("Unexpected second part of RDF triple. Expected UriPart but was " + rdfTriple.getSecond().getClass().getName());
        }

        String elementId = ((RdfTriple.UriPart) rdfTriple.getFirst()).getUri();
        String label = ((RdfTriple.UriPart) rdfTriple.getSecond()).getUri();
        RdfTriple.Part third = rdfTriple.getThird();

        String elementVisibilitySource;
        Matcher visibilityMatcher = VISIBILITY_PATTERN.matcher(elementId);
        if (visibilityMatcher.matches()) {
            elementId = visibilityMatcher.group(1);
            elementVisibilitySource = visibilityMatcher.group(2);
        } else {
            elementVisibilitySource = defaultVisibilitySource;
        }

        if (label.equals(LABEL_CONCEPT_TYPE) || label.equals(VisalloProperties.CONCEPT_TYPE.getPropertyName())) {
            return parseConceptTypeTriple(elementId, elementVisibilitySource, third);
        }

        if (third instanceof RdfTriple.LiteralPart) {
            return parsePropertyTriple(
                    elementId,
                    elementVisibilitySource,
                    label,
                    (RdfTriple.LiteralPart) third,
                    defaultVisibilitySource,
                    workingDir,
                    timeZone
            );
        }

        if (third instanceof RdfTriple.UriPart) {
            return parseAddEdgeTriple(
                    elementId,
                    label,
                    (RdfTriple.UriPart) third,
                    defaultVisibilitySource
            );
        }

        return null;
    }

    private static VisalloRdfTriple parseConceptTypeTriple(
            String vertexId,
            String vertexVisibilitySource,
            RdfTriple.Part third
    ) {
        return new ConceptTypeVisalloRdfTriple(
                vertexId,
                vertexVisibilitySource,
                getConceptType(third)
        );
    }

    private static VisalloRdfTriple parsePropertyTriple(
            String elementId,
            String elementVisibilitySource,
            String label,
            RdfTriple.LiteralPart propertyValuePart,
            String defaultVisibilitySource,
            File workingDir,
            TimeZone timeZone
    ) {
        String metadataKey = null;
        String propertyKey = MULTI_KEY;

        ElementType elementType;
        Matcher elementTypeMatcher = ELEMENT_TYPE_PATTERN.matcher(elementId);
        if (elementTypeMatcher.matches()) {
            elementType = ElementType.valueOf(elementTypeMatcher.group(1));
            elementId = elementTypeMatcher.group(2);
        } else {
            elementType = ElementType.VERTEX;
        }

        Matcher metadataMatcher = METADATA_PATTERN.matcher(label);
        if (metadataMatcher.matches()) {
            label = metadataMatcher.group(1);
            metadataKey = unescape(metadataMatcher.group(2), '@');
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

        // property key
        Matcher keyMatch = PROPERTY_KEY_PATTERN.matcher(label);
        if (keyMatch.matches()) {
            label = keyMatch.group(1);
            propertyKey = unescape(keyMatch.group(2), ':');
        }

        String propertyName = label;
        Object value = getPropertyValue(propertyValuePart, workingDir, timeZone);

        if (metadataKey != null) {
            String metadataName;
            String metadataVisibilitySource;

            visibilityMatch = VISIBILITY_PATTERN.matcher(metadataKey);
            if (visibilityMatch.matches()) {
                metadataName = visibilityMatch.group(1);
                metadataVisibilitySource = visibilityMatch.group(2);
            } else {
                metadataName = metadataKey;
                metadataVisibilitySource = propertyVisibilitySource;
            }

            return new SetMetadataVisalloRdfTriple(
                    elementType,
                    elementId,
                    elementVisibilitySource,
                    propertyKey,
                    propertyName,
                    propertyVisibilitySource,
                    metadataName,
                    metadataVisibilitySource,
                    value
            );
        } else {
            return new SetPropertyVisalloRdfTriple(
                    elementType,
                    elementId,
                    elementVisibilitySource,
                    propertyKey,
                    propertyName,
                    propertyVisibilitySource,
                    value
            );
        }
    }

    private static String unescape(String str, char characterToUnescape) {
        return str.replace("\\" + characterToUnescape, Character.toString(characterToUnescape));
    }

    private static VisalloRdfTriple parseAddEdgeTriple(
            String outVertexId,
            String label,
            RdfTriple.UriPart third,
            String defaultVisibilitySource
    ) {
        String inVertexId = third.getUri();

        String visibilitySource;
        Matcher visibilityMatcher = VISIBILITY_PATTERN.matcher(label);
        if (visibilityMatcher.matches()) {
            label = visibilityMatcher.group(1);
            visibilitySource = visibilityMatcher.group(2);
        } else {
            visibilitySource = defaultVisibilitySource;
        }

        String edgeId;
        Matcher edgeIdMatcher = EDGE_ID_PATTERN.matcher(label);
        if (edgeIdMatcher.matches()) {
            label = edgeIdMatcher.group(1);
            edgeId = unescape(edgeIdMatcher.group(2), ':');
        } else {
            edgeId = outVertexId + "_" + label + "_" + inVertexId;
        }

        return new AddEdgeVisalloRdfTriple(
                edgeId,
                outVertexId,
                inVertexId,
                label,
                visibilitySource
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

    private static Object getPropertyValue(
            RdfTriple.LiteralPart propertyValuePart,
            File workingDir,
            TimeZone timeZone
    ) {
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
            case PROPERTY_TYPE_STREAMING_PROPERTY_VALUE_INLINE_BASE64:
                byte[] bytes = propertyValuePart.getString().getBytes();
                StreamingPropertyValue spv1 = new StreamingPropertyValue(new ByteArrayInputStream(Base64.decodeBase64(bytes)), byte[].class);
                spv1.store(true);
                spv1.searchIndex(false);
                return spv1;

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

    protected String escape(String str, char characterToEscape) {
        return str.replace(Character.toString(characterToEscape), "\\" + characterToEscape);
    }

    @Override
    public abstract int hashCode();

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        return true;
    }

    public abstract String getElementId();

    public abstract ImportContext createImportContext(
            ImportContext ctx,
            RdfTripleImportHelper rdfTripleImportHelper,
            Authorizations authorizations
    );

    public ImportContext updateImportContext(
            ImportContext ctx,
            RdfTripleImportHelper rdfTripleImportHelper,
            Authorizations authorizations
    ) {
        return ctx;
    }
}
