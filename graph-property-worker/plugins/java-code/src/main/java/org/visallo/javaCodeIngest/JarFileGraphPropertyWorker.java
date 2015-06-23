package org.visallo.javaCodeIngest;

import org.visallo.core.ingest.graphProperty.GraphPropertyWorkData;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorker;
import org.visallo.core.model.Description;
import org.visallo.core.model.Name;
import org.visallo.core.model.properties.VisalloProperties;
import org.vertexium.*;
import org.vertexium.property.StreamingPropertyValue;

import java.io.InputStream;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import static org.vertexium.util.IterableUtils.toList;

@Name("Java - .jar")
@Description("Extracts data from Java .jar files")
public class JarFileGraphPropertyWorker extends GraphPropertyWorker {
    private static final String MULTI_VALUE_KEY = JarFileGraphPropertyWorker.class.getName();

    @Override
    public void execute(InputStream in, GraphPropertyWorkData data) throws Exception {
        List<Vertex> existingFileVertices = toList(((Vertex) data.getElement()).getVertices(Direction.BOTH, JavaCodeIngestOntology.EDGE_LABEL_JAR_CONTAINS, getAuthorizations()));

        JarInputStream jarInputStream = new JarInputStream(in);
        JarEntry jarEntry;
        while ((jarEntry = jarInputStream.getNextJarEntry()) != null) {
            if (jarEntry.isDirectory()) {
                continue;
            }

            if (fileAlreadyExists(existingFileVertices, jarEntry.getName())) {
                continue;
            }

            StreamingPropertyValue rawValue = new StreamingPropertyValue(jarInputStream, byte[].class);
            rawValue.searchIndex(false);

            Vertex jarEntryVertex = createFileVertex(jarEntry, rawValue, data);

            createJarContainsFileEdge(jarEntryVertex, data);

            getGraph().flush();

            getWorkQueueRepository().pushGraphPropertyQueue(jarEntryVertex, VisalloProperties.RAW.getProperty(jarEntryVertex), data.getPriority());
        }
    }

    private boolean fileAlreadyExists(List<Vertex> existingFileVerticies, String fileName) {
        for (Vertex v : existingFileVerticies) {
            String existingFileName = VisalloProperties.FILE_NAME.getOnlyPropertyValue(v);
            if (existingFileName == null) {
                return false;
            }
            if (existingFileName.equals(fileName)) {
                return true;
            }
        }
        return false;
    }

    private void createJarContainsFileEdge(Vertex jarEntryVertex, GraphPropertyWorkData data) {
        EdgeBuilder jarContainsEdgeBuilder = getGraph().prepareEdge((Vertex) data.getElement(), jarEntryVertex, JavaCodeIngestOntology.EDGE_LABEL_JAR_CONTAINS, data.getProperty().getVisibility());
        jarContainsEdgeBuilder.save(getAuthorizations());
    }

    private Vertex createFileVertex(JarEntry jarEntry, StreamingPropertyValue rawValue, GraphPropertyWorkData data) {
        VertexBuilder jarEntryVertexBuilder = getGraph().prepareVertex(data.getProperty().getVisibility());
        VisalloProperties.CONCEPT_TYPE.setProperty(jarEntryVertexBuilder, JavaCodeIngestOntology.CONCEPT_TYPE_CLASS_FILE, data.getProperty().getVisibility());
        VisalloProperties.MIME_TYPE.addPropertyValue(jarEntryVertexBuilder, MULTI_VALUE_KEY, "application/octet-stream", data.getProperty().getVisibility());
        JavaCodeIngestOntology.JAR_ENTRY_NAME.addPropertyValue(jarEntryVertexBuilder, MULTI_VALUE_KEY, jarEntry.getName(), data.getProperty().getVisibility());
        VisalloProperties.RAW.setProperty(jarEntryVertexBuilder, rawValue, data.getProperty().getVisibility());
        return jarEntryVertexBuilder.save(getAuthorizations());
    }

    @Override
    public boolean isHandled(Element element, Property property) {
        if (property == null) {
            return false;
        }

        if (!property.getName().equals(VisalloProperties.RAW.getPropertyName())) {
            return false;
        }

        String fileName = VisalloProperties.FILE_NAME.getOnlyPropertyValue(element);
        if (fileName == null || !fileName.endsWith(".jar")) {
            return false;
        }

        return true;
    }
}
