package org.visallo.csv;

import org.visallo.core.exception.VisalloException;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorkData;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorker;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorkerPrepareData;
import org.visallo.core.model.Description;
import org.visallo.core.model.Name;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.csv.model.Mapping;
import org.visallo.web.clientapi.model.VisibilityJson;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.vertexium.*;
import org.vertexium.property.StreamingPropertyValue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

@Name("CSV")
@Description("Parses a CSV using a mapping JSON file")
public class CsvGraphPropertyWorker extends GraphPropertyWorker {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(CsvGraphPropertyWorker.class);
    private static final String MULTI_KEY = CsvGraphPropertyWorker.class.getName();
    public static final String VERTEX_ID_PREFIX = "CSV_";
    private String hasEntityIri;
    private String csvConceptTypeIri;

    @Override
    public void prepare(GraphPropertyWorkerPrepareData workerPrepareData) throws Exception {
        super.prepare(workerPrepareData);

        hasEntityIri = getOntologyRepository().getRequiredRelationshipIRIByIntent("artifactHasEntity");
        csvConceptTypeIri = getOntologyRepository().getConceptIRIByIntent("csv");
    }

    @Override
    public void execute(InputStream in, GraphPropertyWorkData data) throws Exception {
        if (csvConceptTypeIri != null) {
            Metadata metadata = data.createPropertyMetadata();
            VisalloProperties.CONCEPT_TYPE.setProperty(data.getElement(), csvConceptTypeIri, metadata, data.getVisibility(), getAuthorizations());
        }

        Mapping mapping = CsvOntology.MAPPING_JSON.getPropertyValue(data.getProperty());
        StreamingPropertyValue raw = VisalloProperties.RAW.getPropertyValue(data.getElement());
        try (InputStream rawIn = raw.getInputStream()) {
            processCsvStream(rawIn, mapping, data);
        }
    }

    public void processCsvStream(InputStream rawIn, Mapping mapping, GraphPropertyWorkData data) throws IOException {
        Reader reader = new InputStreamReader(rawIn);
        Iterable<CSVRecord> records = CSVFormat.EXCEL.parse(reader);
        State state = new State(mapping, data);
        for (CSVRecord record : records) {
            if (record.getRecordNumber() <= mapping.getLinesToSkip()) {
                continue;
            }
            state.setRecord(record);
            processCsvLine(state);
        }
        getGraph().flush();
    }

    private void processCsvLine(State state) {
        LOGGER.debug("line %d: %s", state.getRecord().getRecordNumber(), state.getRecord().toString());

        List<Vertex> vertices = new ArrayList<>();
        for (Mapping.Vertex mappingVertex : state.getMapping().getVertices()) {
            try {
                String hash = getHash(state, mappingVertex);
                Vertex vertex = state.getVertex(getGraph(), hash, getAuthorizations());
                if (vertex == null) {
                    vertex = createEntity(state, mappingVertex, hash);
                }
                vertices.add(vertex);
                state.addCachedVertex(hash, vertex);
            } catch (Exception ex) {
                throw new VisalloCsvException(state, mappingVertex, ex);
            }
        }

        for (Mapping.Edge mappingEdge : state.getMapping().getEdges()) {
            try {
                createEdge(state, mappingEdge, vertices);
            } catch (Exception ex) {
                throw new VisalloCsvException(state, mappingEdge, ex);
            }
        }
    }

    private void createEdge(State state, Mapping.Edge mappingEdge, List<Vertex> vertices) {
        String label = mappingEdge.getLabel();
        checkNotNull(label, "label is required");

        checkArgument(mappingEdge.getOut() < vertices.size(), "out vertex index " + mappingEdge.getOut() + " must be less than " + vertices.size());
        Vertex outVertex = vertices.get(mappingEdge.getOut());
        checkNotNull(outVertex, "out vertex cannot be null");

        checkArgument(mappingEdge.getIn() < vertices.size(), "in vertex index " + mappingEdge.getIn() + " must be less than " + vertices.size());
        Vertex inVertex = vertices.get(mappingEdge.getIn());
        checkNotNull(inVertex, "in vertex cannot be null");

        String edgeId = outVertex.getId() + "_" + label + "_" + inVertex.getId();

        EdgeBuilder e = getGraph().prepareEdge(edgeId, outVertex, inVertex, label, state.getData().getVisibility());
        state.getData().setVisibilityJsonOnElement(e);
        e.save(getAuthorizations());
    }

    private Vertex createEntity(State state, Mapping.Vertex mappingVertex, String hash) {
        Visibility visibility = state.getData().getVisibility();
        String vertexId = createVertexId(hash);
        VertexBuilder v = getGraph().prepareVertex(vertexId, visibility);
        boolean foundVisibilityJson = false;
        for (Mapping.Property property : mappingVertex.getProperties()) {
            try {
                String propertyKey = property.getKey();
                if (propertyKey == null) {
                    propertyKey = MULTI_KEY;
                }
                String propertyName = property.getName();
                checkNotNull(propertyName, "property 'name' is required.");
                if (VisalloProperties.VISIBILITY_JSON.getPropertyName().equals(propertyName)) {
                    foundVisibilityJson = true;
                }

                Metadata metadata = state.getData().createPropertyMetadata();
                Object value = getPropertyValue(state, property);
                if (value != null) {
                    v.addPropertyValue(propertyKey, propertyName, value, metadata, visibility);
                }
            } catch (Exception ex) {
                throw new VisalloCsvException(state, mappingVertex, property, ex);
            }
        }
        if (!foundVisibilityJson) {
            VisibilityJson visibilityJson = state.getData().getVisibilityJson();
            VisalloProperties.VISIBILITY_JSON.setProperty(v, visibilityJson, visibility);
        }
        Vertex vertex = v.save(getAuthorizations());
        getGraph().flush();
        createHasEntityEdge(state, vertex, visibility);
        addVertexToWorkspaceIfNeeded(state.getData(), vertex);
        return vertex;
    }

    private void createHasEntityEdge(State state, Vertex entityVertex, Visibility visibility) {
        Vertex artifactVertex = (Vertex) state.getData().getElement();
        String edgeId = artifactVertex.getId() + "_hasEntity_" + entityVertex.getId();
        EdgeBuilder e = getGraph().prepareEdge(edgeId, artifactVertex, entityVertex, hasEntityIri, visibility);
        state.getData().setVisibilityJsonOnElement(e);
        e.save(getAuthorizations());
    }

    private String createVertexId(String hash) {
        return VERTEX_ID_PREFIX + hash;
    }

    private String getHash(State state, Mapping.Vertex mappingVertex) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            for (Mapping.Property property : mappingVertex.getProperties()) {
                Object value = getPropertyValue(state, property);
                if (value != null) {
                    md5.update(value.toString().getBytes());
                }
            }
            return Hex.encodeHexString(md5.digest());
        } catch (NoSuchAlgorithmException ex) {
            throw new VisalloException("Could not find MD5", ex);
        }
    }

    private Object getPropertyValue(State state, Mapping.Property property) {
        if (property.getValue() != null) {
            return property.getValue();
        } else if (property.getColumn() != null) {
            return state.getRecord().get(property.getColumn());
        } else {
            throw new VisalloException("Either 'value' or 'column' is required.");
        }
    }

    @Override
    public boolean isHandled(Element element, Property property) {
        if (property == null) {
            return false;
        }

        return property.getName().equals(CsvOntology.MAPPING_JSON.getPropertyName());
    }
}
