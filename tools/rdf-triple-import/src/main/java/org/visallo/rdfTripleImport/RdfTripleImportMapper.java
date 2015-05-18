package org.visallo.rdfTripleImport;

import org.visallo.core.exception.VisalloException;
import org.visallo.vertexium.mapreduce.VisalloElementMapperBase;
import org.visallo.core.model.properties.VisalloProperties;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.vertexium.Authorizations;
import org.vertexium.Metadata;
import org.vertexium.VertexBuilder;
import org.vertexium.Visibility;
import org.vertexium.type.GeoPoint;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class RdfTripleImportMapper extends VisalloElementMapperBase<LongWritable, Text> {
    private static final String MULTI_KEY = RdfTripleImportMapper.class.getSimpleName();
    private Visibility visibility;
    private Metadata metadata;
    private Authorizations authorizations;

    @Override
    protected void setup(Context context) throws IOException, InterruptedException {
        super.setup(context);
        visibility = new Visibility(context.getConfiguration().get(RdfTripleImportMR.CONFIG_VISIBILITY_STRING, ""));
        metadata = new Metadata();
        authorizations = getGraph().createAuthorizations();
    }

    @Override
    protected void safeMap(LongWritable key, Text lineText, Context context) throws Exception {
        String line = lineText.toString().trim();
        if (line.length() == 0 || line.charAt(0) == '#') {
            return;
        }
        context.setStatus(line);
        RdfTriple rdfTriple = RdfTripleParser.parseLine(line);
        if (!(rdfTriple.getFirst() instanceof RdfTriple.UriPart)) {
            return;
        }
        if (!(rdfTriple.getSecond() instanceof RdfTriple.UriPart)) {
            return;
        }

        String vertexId = ((RdfTriple.UriPart) rdfTriple.getFirst()).getUri();
        String label = ((RdfTriple.UriPart) rdfTriple.getSecond()).getUri();
        RdfTriple.Part third = rdfTriple.getThird();

        if (label.equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")) {
            setConceptType(vertexId, third);
            return;
        }

        if (third instanceof RdfTriple.LiteralPart) {
            setProperty(vertexId, label, (RdfTriple.LiteralPart) third);
            return;
        }

        if (third instanceof RdfTriple.UriPart) {
            addEdge(vertexId, label, ((RdfTriple.UriPart) third).getUri());
            return;
        }

        throw new VisalloException("Unhandled combination of RDF triples");
    }

    private void addEdge(String outVertexId, String label, String inVertexId) {
        String edgeId = outVertexId + "_" + label + "_" + inVertexId;
        getGraph().addEdge(edgeId, outVertexId, inVertexId, label, getVisibility(), getAuthorizations());
    }

    private void setProperty(String vertexId, String propertyName, RdfTriple.LiteralPart propertyValuePart) {
        VertexBuilder m = getGraph().prepareVertex(vertexId, getVisibility());
        Object propertyValue = getPropertyValue(propertyValuePart);
        m.addPropertyValue(MULTI_KEY, propertyName, propertyValue, getMetadata(), getVisibility());
        m.save(getAuthorizations());
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
            case "http://visallo.org#geolocation":
                return GeoPoint.parse(propertyValuePart.getString());
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
        VertexBuilder m = getGraph().prepareVertex(vertexId, getVisibility());
        String conceptType = getConceptType(third);
        VisalloProperties.CONCEPT_TYPE.setProperty(m, conceptType, getMetadata(), getVisibility());
        m.save(getAuthorizations());
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

    private Metadata getMetadata() {
        return metadata;
    }

    private Visibility getVisibility() {
        return visibility;
    }

    public Authorizations getAuthorizations() {
        return authorizations;
    }
}
