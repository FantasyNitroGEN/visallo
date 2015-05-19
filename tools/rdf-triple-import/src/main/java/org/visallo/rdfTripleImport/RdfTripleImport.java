package org.visallo.rdfTripleImport;

import org.vertexium.*;
import org.vertexium.property.StreamingPropertyValue;
import org.vertexium.type.GeoPoint;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.properties.VisalloProperties;

import java.io.*;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class RdfTripleImport {
    public static final String MULTI_KEY = RdfTripleImport.class.getSimpleName();
    public static final String PROPERTY_TYPE_GEOLOCATION = "http://visallo.org#geolocation";
    public static final String PROPERTY_TYPE_STREAMING_PROPERTY_VALUE = "http://visallo.org#streamingPropertyValue";
    public static final String PROPERTY_TYPE_STREAMING_PROPERTY_VALUE_INLINE = "http://visallo.org#streamingPropertyValueInline";
    public static final String LABEL_CONCEPT_TYPE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
    private final Graph graph;
    private final Visibility visibility;
    private final Metadata metadata;
    private final Authorizations authorizations;

    public RdfTripleImport(Graph graph, Metadata metadata, Visibility visibility, Authorizations authorizations) {
        this.graph = graph;
        this.visibility = visibility;
        this.metadata = metadata;
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

    private void setProperty(String vertexId, String propertyName, RdfTriple.LiteralPart propertyValuePart) {
        VertexBuilder m = graph.prepareVertex(vertexId, visibility);
        Object propertyValue = getPropertyValue(propertyValuePart);
        m.addPropertyValue(MULTI_KEY, propertyName, propertyValue, metadata, visibility);
        m.save(authorizations);
    }

    private Object getPropertyValue(RdfTriple.LiteralPart propertyValuePart) {
        if (propertyValuePart.getType() == null) {
            return propertyValuePart.getString();
        }
        String typeUri = propertyValuePart.getType().getUri();
        switch (typeUri) {
            case "http://www.w3.org/2001/XMLSchema#date":
                return parseDate(propertyValuePart.getString());
            case "http://www.w3.org/2001/XMLSchema#dateTime":
                return parseDateTime(propertyValuePart.getString());
            case "http://www.w3.org/2001/XMLSchema#gYear":
                return Integer.parseInt(propertyValuePart.getString());
            case "http://www.w3.org/2001/XMLSchema#gMonthDay":
                return propertyValuePart.getString(); // TODO: is there a better format for this.
            case "http://www.w3.org/2001/XMLSchema#string":
                return propertyValuePart.getString();
            case "http://www.w3.org/2001/XMLSchema#boolean":
                return Boolean.parseBoolean(propertyValuePart.getString());
            case "http://www.w3.org/2001/XMLSchema#double":
                return Double.parseDouble(propertyValuePart.getString());
            case "http://visallo.org#currency":
                return new BigDecimal(propertyValuePart.getString());
            case "http://www.w3.org/2001/XMLSchema#int":
            case "http://www.w3.org/2001/XMLSchema#integer":
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
                    return new StreamingPropertyValue(in, byte[].class);
                } catch (FileNotFoundException ex) {
                    throw new VisalloException("Could not read file: " + file.getAbsolutePath(), ex);
                }
            case PROPERTY_TYPE_STREAMING_PROPERTY_VALUE_INLINE:
                InputStream in = new ByteArrayInputStream(propertyValuePart.getString().getBytes());
                return new StreamingPropertyValue(in, byte[].class);
            default:
                throw new VisalloException("Unhandled property type: " + propertyValuePart.getType().getUri() + " (value: " + propertyValuePart.getString() + ")");
        }
    }

    private Date parseDate(String dateString) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd").parse(dateString);
        } catch (ParseException e) {
            throw new VisalloException("Could not parse date: " + dateString, e);
        }
    }

    private Date parseDateTime(String dateString) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(dateString);
        } catch (ParseException e) {
            throw new VisalloException("Could not parse date: " + dateString, e);
        }
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
