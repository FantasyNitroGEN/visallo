package org.visallo.common.rdf;

import com.google.inject.Inject;
import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.datatypes.xsd.XSDDateTime;
import com.hp.hpl.jena.rdf.model.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.json.JSONObject;
import org.vertexium.*;
import org.vertexium.Property;
import org.vertexium.property.StreamingPropertyValue;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorkData;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.security.VisibilityTranslator;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.clientapi.model.VisibilityJson;

import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class RdfXmlImportHelper {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(RdfXmlImportHelper.class);
    public static final String RDF_TYPE_URI = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
    private static final String MULTI_VALUE_KEY = RdfXmlImportHelper.class.getSimpleName();
    private final Graph graph;
    private final WorkQueueRepository workQueueRepository;
    private final String rdfConceptTypeIri;
    private final String hasEntityIri;
    private final WorkspaceRepository workspaceRepository;
    private final VisibilityTranslator visibilityTranslator;

    @Inject
    public RdfXmlImportHelper(
            Graph graph,
            WorkQueueRepository workQueueRepository,
            OntologyRepository ontologyRepository,
            WorkspaceRepository workspaceRepository,
            VisibilityTranslator visibilityTranslator
    ) {
        this.graph = graph;
        this.workQueueRepository = workQueueRepository;
        this.workspaceRepository = workspaceRepository;
        this.visibilityTranslator = visibilityTranslator;

        hasEntityIri = ontologyRepository.getRequiredRelationshipIRIByIntent("artifactHasEntity");

        // rdfConceptTypeIri is not required because the
        // concept type on the vertex could have been set somewhere else
        rdfConceptTypeIri = ontologyRepository.getConceptIRIByIntent("rdf");
    }

    public void importRdfXml(
            File inputFile,
            GraphPropertyWorkData data,
            Priority priority,
            String visibilitySource,
            User user,
            Authorizations authorizations
    ) throws IOException {
        try (InputStream in = new FileInputStream(inputFile)) {
            File baseDir = inputFile.getParentFile();
            importRdfXml(in, baseDir, data, priority, visibilitySource, user, authorizations);
        }
    }

    public void importRdfXml(
            InputStream in,
            File baseDir,
            GraphPropertyWorkData data,
            Priority priority,
            String visibilitySource,
            User user,
            Authorizations authorizations
    ) {
        Visibility visibility = visibilityTranslator.toVisibility(visibilitySource).getVisibility();
        VisibilityJson visibilityJson = new VisibilityJson(visibilitySource);
        String workspaceId = null;
        if (data != null) {
            workspaceId = data.getWorkspaceId();
            visibilityJson.addWorkspace(workspaceId);
        }
        if (rdfConceptTypeIri != null && data != null) {
            VisalloProperties.CONCEPT_TYPE.setProperty(data.getElement(), rdfConceptTypeIri, visibilityTranslator.getDefaultVisibility(), authorizations);
        }

        Model model = ModelFactory.createDefaultModel();
        model.read(in, null);

        Results results = new Results();
        importRdfModel(results, model, baseDir, data, visibilityJson, visibility, user, authorizations);

        graph.flush();

        LOGGER.debug("pushing vertices from RDF import on to work queue");
        for (Vertex vertex : results.getVertices()) {
            workQueueRepository.broadcastElement(vertex, workspaceId);
            for (Property prop : vertex.getProperties()) {
                workQueueRepository.pushGraphPropertyQueue(vertex, prop, priority);
            }
        }

        LOGGER.debug("pushing edges from RDF import on to work queue");
        for (Edge edge : results.getEdges()) {
            workQueueRepository.broadcastElement(edge, workspaceId);
            for (Property prop : edge.getProperties()) {
                workQueueRepository.pushGraphPropertyQueue(edge, prop, priority);
            }
        }
    }

    private void importRdfModel(Results results, Model model, File baseDir, GraphPropertyWorkData data, VisibilityJson visibilityJson, Visibility visibility, User user, Authorizations authorizations) {
        ResIterator subjects = model.listSubjects();
        while (subjects.hasNext()) {
            Resource subject = subjects.next();
            importSubject(results, graph, subject, baseDir, data, visibilityJson, visibility, user, authorizations);
        }
    }

    private void importSubject(Results results, Graph graph, Resource subject, File baseDir, GraphPropertyWorkData data, VisibilityJson visibilityJson, Visibility visibility, User user, Authorizations authorizations) {
        LOGGER.info("importSubject: %s", subject.toString());
        String graphVertexId = getGraphVertexId(subject);
        VertexBuilder vertexBuilder = graph.prepareVertex(graphVertexId, visibility);
        if (data != null) {
            data.setVisibilityJsonOnElement(vertexBuilder);
        } else {
            VisalloProperties.VISIBILITY_JSON.setProperty(vertexBuilder, visibilityJson, visibility);
        }
        VisalloProperties.MODIFIED_DATE.setProperty(vertexBuilder, new Date(), visibility);
        VisalloProperties.MODIFIED_BY.setProperty(vertexBuilder, user.getUserId(), visibility);

        StmtIterator statements = subject.listProperties();
        while (statements.hasNext()) {
            Statement statement = statements.next();
            RDFNode obj = statement.getObject();
            if (obj instanceof Resource) {
                if (isConceptTypeResource(statement)) {
                    String value = statement.getResource().toString();
                    VisalloProperties.CONCEPT_TYPE.setProperty(vertexBuilder, value, visibilityTranslator.getDefaultVisibility());
                }
            } else if (obj instanceof Literal) {
                LOGGER.info("set property on %s to %s", subject.toString(), statement.toString());
                importLiteral(vertexBuilder, statement, baseDir, data, visibilityJson, visibility, user);
            } else {
                throw new VisalloException("Unhandled object type: " + obj.getClass().getName());
            }
        }

        Vertex v = vertexBuilder.save(authorizations);
        results.addVertex(v);

        if (data != null) {
            String edgeId = data.getElement().getId() + "_hasEntity_" + v.getId();
            EdgeBuilder e = graph.prepareEdge(edgeId, (Vertex) data.getElement(), v, hasEntityIri, visibility);
            data.setVisibilityJsonOnElement(e);
            results.addEdge(e.save(authorizations));

            addVertexToWorkspaceIfNeeded(data, v, user);
        }

        statements = subject.listProperties();
        while (statements.hasNext()) {
            Statement statement = statements.next();
            RDFNode obj = statement.getObject();
            if (obj instanceof Resource) {
                if (isConceptTypeResource(statement)) {
                    continue;
                }
                importResource(results, graph, v, statement, data, visibilityJson, visibility, user, authorizations);
            }
        }
    }

    private boolean isConceptTypeResource(Statement statement) {
        String label = statement.getPredicate().toString();
        return label.equals(RDF_TYPE_URI);
    }

    private void importLiteral(VertexBuilder v, Statement statement, File baseDir, GraphPropertyWorkData data, VisibilityJson visibilityJson, Visibility visibility, User user) {
        String propertyName = statement.getPredicate().toString();

        RDFDatatype datatype = statement.getLiteral().getDatatype();
        Object literalValue = statement.getLiteral().getValue();
        Object value = literalValue;
        if (datatype == null || XSDDatatype.XSDstring.equals(datatype)) {
            String valueString = statement.getLiteral().toString();
            if (valueString.startsWith("streamingValue:")) {
                value = convertStreamingValueJsonToValueObject(baseDir, valueString);
            }
        } else if (literalValue instanceof XSDDateTime) {
            XSDDateTime xsdDateTime = (XSDDateTime) literalValue;
            value = xsdDateTime.asCalendar().getTime();
        } else {
            value = literalValue;
        }

        Metadata metadata = new Metadata();
        if (data != null) {
            metadata = data.createPropertyMetadata(user);
        } else {
            VisalloProperties.VISIBILITY_JSON_METADATA.setMetadata(metadata, visibilityJson, visibility);
            VisalloProperties.MODIFIED_BY_METADATA.setMetadata(metadata, user.getUserId(), visibility);
            VisalloProperties.MODIFIED_DATE_METADATA.setMetadata(metadata, new Date(), visibility);
        }
        v.addPropertyValue(MULTI_VALUE_KEY, propertyName, value, metadata, visibility);
    }

    private String hashValue(String valueString) {
        // we need a unique value but it's a bit silly to store a whole md5 hash
        return DigestUtils.md5Hex(valueString).substring(0, 10);
    }

    private Object convertStreamingValueJsonToValueObject(File baseDir, String valueString) {
        JSONObject streamingValueJson = new JSONObject(valueString.substring("streamingValue:".length()));
        String fileName = streamingValueJson.getString("fileName");
        if (baseDir == null) {
            throw new VisalloException("Could not import streamingValue. No baseDir specified.");
        }
        File file = new File(baseDir, fileName);
        InputStream in;
        try {
            if (!file.exists()) {
                throw new VisalloException("File " + file.getAbsolutePath() + " does not exist.");
            }
            in = new FileInputStream(file);
        } catch (FileNotFoundException ex) {
            throw new VisalloException("File " + file.getAbsolutePath() + " does not exist.");
        }
        StreamingPropertyValue spv = new StreamingPropertyValue(in, byte[].class);
        spv.searchIndex(false);
        spv.store(true);
        return spv;
    }

    private void importResource(
            Results results,
            Graph graph,
            Vertex outVertex,
            Statement statement,
            GraphPropertyWorkData data,
            VisibilityJson visibilityJson,
            Visibility visibility,
            User user,
            Authorizations authorizations
    ) {
        String label = statement.getPredicate().toString();
        String vertexId = getGraphVertexId(statement.getResource());
        VertexBuilder inVertexBuilder = graph.prepareVertex(vertexId, visibility);
        if (data != null) {
            data.setVisibilityJsonOnElement(inVertexBuilder);
        }
        Vertex inVertex = inVertexBuilder.save(authorizations);
        results.addVertex(inVertex);
        if (data != null) {
            addVertexToWorkspaceIfNeeded(data, inVertex, user);
        }
        String edgeId = outVertex.getId() + "_" + label + "_" + inVertex.getId();

        EdgeBuilder e = graph.prepareEdge(edgeId, outVertex, inVertex, label, visibility);
        if (data != null) {
            data.setVisibilityJsonOnElement(e);
        } else {
            VisalloProperties.VISIBILITY_JSON.setProperty(e, visibilityJson, visibility);
        }
        VisalloProperties.MODIFIED_BY.setProperty(e, user.getUserId(), visibility);
        VisalloProperties.MODIFIED_DATE.setProperty(e, new Date(), visibility);
        results.addEdge(e.save(authorizations));
        LOGGER.info("importResource: %s = %s", label, vertexId);
    }

    /**
     * RDF requires that all subjects are URIs. To create more portable ids,
     * this method will look for the last '#' character and return everything after that.
     */
    private String getGraphVertexId(Resource subject) {
        String subjectUri = subject.getURI();
        checkNotNull(subjectUri, "could not get uri of subject: " + subject);
        int lastPound = subjectUri.lastIndexOf('#');
        checkArgument(lastPound >= 1, "Could not find '#' in subject uri: " + subjectUri);
        return subjectUri.substring(lastPound + 1);
    }

    protected void addVertexToWorkspaceIfNeeded(GraphPropertyWorkData data, Vertex vertex, User user) {
        if (data.getWorkspaceId() == null) {
            return;
        }
        graph.flush();
        workspaceRepository.updateEntityOnWorkspace(data.getWorkspaceId(), vertex.getId(), false, null, user);
    }

    private static class Results {
        private final List<Vertex> vertices = new ArrayList<>();
        private final List<Edge> edges = new ArrayList<>();

        public void addEdge(Edge edge) {
            this.edges.add(edge);
        }

        public void addVertex(Vertex vertex) {
            this.vertices.add(vertex);
        }

        public Iterable<Edge> getEdges() {
            return edges;
        }

        public Iterable<Vertex> getVertices() {
            return vertices;
        }
    }
}
