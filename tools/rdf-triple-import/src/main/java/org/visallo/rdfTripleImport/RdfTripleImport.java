package org.visallo.rdfTripleImport;

import org.vertexium.*;
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

public class RdfTripleImport {
    public static final String MULTI_KEY = RdfTripleImport.class.getSimpleName();
    public static final String LABEL_CONCEPT_TYPE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
    public static final String PROPERTY_TYPE_GEOLOCATION = "http://visallo.org#geolocation";
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
    private final Visibility visibility;
    private final Metadata metadata;
    private final Authorizations authorizations;
    private final TimeZone timeZone;

    public RdfTripleImport(Graph graph, Metadata metadata, TimeZone timeZone, Visibility visibility, Authorizations authorizations) {
        this.graph = graph;
        this.visibility = visibility;
        this.metadata = metadata;
        this.timeZone = timeZone;
        this.authorizations = authorizations;
    }

    public void importRdfLine(String line) {
        if (line.length() == 0 || line.charAt(0) == '#') {
            return;
        }
        RdfTriple rdfTriple = RdfTripleParser.parseLine(line);
        if (importRdfTriple(rdfTriple)) {
            return;
        }

        throw new VisalloException("Unhandled combination of RDF triples");
    }

    public boolean importRdfTriple(RdfTriple rdfTriple) {
        if (!(rdfTriple.getFirst() instanceof RdfTriple.UriPart)) {
            return true;
        }
        if (!(rdfTriple.getSecond() instanceof RdfTriple.UriPart)) {
            return true;
        }

        String vertexId = ((RdfTriple.UriPart) rdfTriple.getFirst()).getUri();
        String label = ((RdfTriple.UriPart) rdfTriple.getSecond()).getUri();
        RdfTriple.Part third = rdfTriple.getThird();

        if (label.equals(LABEL_CONCEPT_TYPE)) {
            setConceptType(vertexId, third);
            return true;
        }

        if (third instanceof RdfTriple.LiteralPart) {
            setProperty(vertexId, label, (RdfTriple.LiteralPart) third);
            return true;
        }

        if (third instanceof RdfTriple.UriPart) {
            addEdge(vertexId, label, ((RdfTriple.UriPart) third).getUri());
            return true;
        }

        return false;
    }

    private void addEdge(String outVertexId, String label, String inVertexId) {
        String edgeId = outVertexId + "_" + label + "_" + inVertexId;
        graph.addEdge(edgeId, outVertexId, inVertexId, label, visibility, authorizations);
    }

    private void setProperty(String vertexId, String label, RdfTriple.LiteralPart propertyValuePart) {
        VertexBuilder m = graph.prepareVertex(vertexId, visibility);
        String propertyKey = MULTI_KEY;
        String propertyName = label;
        int lastHash = label.lastIndexOf('#');
        if (lastHash > 0) {
            int colonAfterHash = label.indexOf(':', lastHash);
            if (colonAfterHash > 0) {
                propertyName = label.substring(0, colonAfterHash);
                propertyKey = label.substring(colonAfterHash + 1);
            }
        }

        Object propertyValue = getPropertyValue(propertyValuePart);
        m.addPropertyValue(propertyKey, propertyName, propertyValue, metadata, visibility);
        m.save(authorizations);
    }

    private Object getPropertyValue(RdfTriple.LiteralPart propertyValuePart) {
        if (propertyValuePart.getType() == null) {
            return propertyValuePart.getString();
        }
        String typeUri = propertyValuePart.getType().getUri();
        switch (typeUri) {
            case PROPERTY_TYPE_DATE:
                return parseDate(propertyValuePart.getString());
            case PROPERTY_TYPE_DATE_TIME:
                return parseDateTime(propertyValuePart.getString());
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
            case PROPERTY_TYPE_STREAMING_PROPERTY_VALUE:
                File file = new File(propertyValuePart.getString());
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

    private Date parseDate(String dateString) {
        VisalloDate visalloDate = VisalloDate.create(dateString);
        return visalloDate.toDate();
    }

    private Date parseDateTime(String dateTimeString) {
        VisalloDateTime visalloDateTime = VisalloDateTime.create(dateTimeString, timeZone);
        return visalloDateTime.toDateGMT();
    }

    private void setConceptType(String vertexId, RdfTriple.Part third) {
        VertexBuilder m = graph.prepareVertex(vertexId, visibility);
        String conceptType = getConceptType(third);
        VisalloProperties.CONCEPT_TYPE.setProperty(m, conceptType, metadata, visibility);
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
}
