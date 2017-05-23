package org.visallo.common.rdf;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.vertexium.*;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.web.clientapi.model.VisibilityJson;

import java.io.IOException;
import java.io.OutputStream;

public class RdfExportHelper {
    public String exportVertexiumObjectToRdfTriple(VertexiumObject vertexiumObject) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            exportVertexiumObjectToRdfTriple(vertexiumObject, out);
        } catch (IOException ex) {
            throw new VisalloException("Could not export vertexiumObject", ex);
        }
        return out.toString();
    }

    public void exportVertexiumObjectsToRdfTriple(Iterable<? extends VertexiumObject> vertexiumObjects, OutputStream out) throws IOException {
        boolean first = true;
        for (VertexiumObject vertexiumObject : vertexiumObjects) {
            if (!first) {
                out.write("\n".getBytes());
            }
            exportVertexiumObjectToRdfTriple(vertexiumObject, out);
            first = false;
        }
    }

    public void exportVertexiumObjectToRdfTriple(VertexiumObject vertexiumObject, OutputStream out) throws IOException {
        writeVertexiumObjectRdfTripleComment(vertexiumObject, out);
        if (vertexiumObject instanceof Element) {
            writeElementRdfTriple((Element) vertexiumObject, out);
            for (Property property : vertexiumObject.getProperties()) {
                writePropertyRdfTriples((Element) vertexiumObject, property, out);
            }
        } else {
            throw new VisalloException("Unhandled " + VertexiumObject.class.getName() + ": " + vertexiumObject.getClass().getName());
        }
    }

    private void writeVertexiumObjectRdfTripleComment(VertexiumObject vertexiumObject, OutputStream out) throws IOException {
        String typeString;
        if (vertexiumObject instanceof Vertex) {
            typeString = "Vertex";
        } else if (vertexiumObject instanceof Edge) {
            typeString = "Edge";
        } else if (vertexiumObject instanceof ExtendedDataRow) {
            typeString = "Extended Data Row";
        } else {
            throw new VisalloException("Unhandled " + VertexiumObject.class.getName() + ": " + vertexiumObject.getClass().getName());
        }
        out.write(String.format("# %s: %s", typeString, vertexiumObject.getId()).getBytes());
        out.write("\n".getBytes());
    }

    private void writeElementRdfTriple(Element element, OutputStream out) throws IOException {
        String elementVisibilitySource = getVisibilitySource(element);
        if (element instanceof Vertex) {
            String conceptType = VisalloProperties.CONCEPT_TYPE.getPropertyValue(element, OntologyRepository.ENTITY_CONCEPT_IRI);
            write(new ConceptTypeVisalloRdfTriple(element.getId(), elementVisibilitySource, conceptType), out);
        } else if (element instanceof Edge) {
            Edge edge = (Edge) element;
            write(new AddEdgeVisalloRdfTriple(
                    element.getId(),
                    edge.getVertexId(Direction.OUT),
                    edge.getVertexId(Direction.IN),
                    edge.getLabel(),
                    elementVisibilitySource
            ), out);
        } else {
            throw new VisalloException("Unhandled element type: " + element.getClass().getName());
        }
    }

    private void writePropertyRdfTriples(Element element, Property property, OutputStream out) throws IOException {
        SetPropertyVisalloRdfTriple setPropertyVisalloRdfTriple = new SetPropertyVisalloRdfTriple(
                element instanceof Vertex ? ElementType.VERTEX : ElementType.EDGE,
                element.getId(),
                getVisibilitySource(element),
                property.getKey(),
                property.getName(),
                getVisibilitySource(property),
                property.getValue());
        write(setPropertyVisalloRdfTriple, out);

        if (setPropertyVisalloRdfTriple.getValueRdfString() != null) {
            for (Metadata.Entry entry : property.getMetadata().entrySet()) {
                writeMetadataEntryRdfTriple(element, property, entry, out);
            }
        }
    }

    private void writeMetadataEntryRdfTriple(Element element, Property property, Metadata.Entry entry, OutputStream out) throws IOException {
        write(new SetMetadataVisalloRdfTriple(
                element instanceof Vertex ? ElementType.VERTEX : ElementType.EDGE,
                element.getId(),
                getVisibilitySource(element),
                property.getKey(),
                property.getName(),
                getVisibilitySource(property),
                entry.getKey(),
                entry.getVisibility().toString(),
                entry.getValue()
        ), out);
    }

    private void write(VisalloRdfTriple triple, OutputStream out) throws IOException {
        out.write(triple.toString().getBytes());
        out.write("\n".getBytes());
    }

    private String getVisibilitySource(Element element) {
        VisibilityJson visibilityJson = VisalloProperties.VISIBILITY_JSON.getPropertyValue(element);
        if (visibilityJson != null) {
            return visibilityJson.getSource();
        }
        return null;
    }

    private String getVisibilitySource(Property property) {
        VisibilityJson visibilityJson = VisalloProperties.VISIBILITY_JSON_METADATA.getMetadataValue(property.getMetadata());
        if (visibilityJson != null) {
            return visibilityJson.getSource();
        }
        return null;
    }
}
