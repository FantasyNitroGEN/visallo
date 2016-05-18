package org.visallo.common.rdf;

import com.google.inject.Inject;
import org.vertexium.*;
import org.vertexium.mutation.ElementMutation;
import org.vertexium.mutation.ExistingElementMutation;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.graph.GraphRepository;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.security.VisibilityTranslator;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.clientapi.model.VisibilityJson;

import java.io.*;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;

import static com.google.common.base.Preconditions.checkNotNull;

public class RdfTripleImportHelper {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(RdfTripleImportHelper.class);
    private final Graph graph;
    private final VisibilityTranslator visibilityTranslator;
    private WorkQueueRepository workQueueRepository;

    public void setFailOnFirstError(boolean failOnFirstError) {
        this.failOnFirstError = failOnFirstError;
    }

    private boolean failOnFirstError = false;

    @Inject
    public RdfTripleImportHelper(
            Graph graph,
            VisibilityTranslator visibilityTranslator,
            WorkQueueRepository workQueueRepository
    ) {
        this.graph = graph;
        this.visibilityTranslator = visibilityTranslator;
        this.workQueueRepository = workQueueRepository;
    }

    @Deprecated
    public void importRdfTriple(
            File inputFile,
            TimeZone timeZone,
            String defaultVisibilitySource,
            User user,
            Authorizations authorizations
    ) throws IOException {
        importRdfTriple(
                inputFile.getName(),
                new FileInputStream(inputFile),
                inputFile.getParentFile(),
                timeZone,
                Priority.NORMAL,
                defaultVisibilitySource,
                user,
                authorizations
        );
    }

    public void importRdfTriple(
            File inputFile,
            TimeZone timeZone,
            Priority priority,
            String defaultVisibilitySource,
            User user,
            Authorizations authorizations
    ) throws IOException {
        importRdfTriple(
                inputFile.getName(),
                new FileInputStream(inputFile),
                inputFile.getParentFile(),
                timeZone,
                priority,
                defaultVisibilitySource,
                user,
                authorizations
        );
    }

    private void importRdfTriple(
            String sourceFileName,
            InputStream inputStream,
            File workingDir,
            TimeZone timeZone,
            Priority priority,
            String defaultVisibilitySource,
            User user,
            Authorizations authorizations
    ) throws IOException {
        Set<Element> elements = new HashSet<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        int lineNum = 1;
        String line;
        while ((line = reader.readLine()) != null) {
            LOGGER.debug("Importing RDF triple on line: %d", lineNum);
            try {
                importRdfLine(elements, sourceFileName, line, workingDir, timeZone, defaultVisibilitySource, user, authorizations);
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

        graph.flush();
        LOGGER.info("pushing %d elements from RDF import on to work queue", elements.size());
        workQueueRepository.pushElements(elements, priority);
    }

    public void importRdfLine(
            Set<Element> elements,
            String sourceFileName,
            String line,
            File workingDir,
            TimeZone timeZone,
            String defaultVisibilitySource,
            User user,
            Authorizations authorizations
    ) {
        if (line.length() == 0 || line.charAt(0) == '#') {
            return;
        }
        RdfTriple rdfTriple = RdfTripleParser.parseLine(line);
        if (importRdfTriple(elements, rdfTriple, sourceFileName, workingDir, timeZone, defaultVisibilitySource, user, authorizations)) {
            return;
        }

        throw new VisalloException("Unhandled combination of RDF triples");
    }

    public boolean importRdfTriple(
            Set<Element> elements,
            RdfTriple rdfTriple,
            String sourceFileName,
            File workingDir,
            TimeZone timeZone,
            String defaultVisibilitySource,
            User user,
            Authorizations authorizations
    ) {
        if (!(rdfTriple.getFirst() instanceof RdfTriple.UriPart)) {
            return true;
        }
        if (!(rdfTriple.getSecond() instanceof RdfTriple.UriPart)) {
            return true;
        }

        VisalloRdfTriple triple = VisalloRdfTriple.parse(
                rdfTriple,
                defaultVisibilitySource,
                visibilityTranslator,
                workingDir,
                timeZone
        );
        if (triple == null) {
            return false;
        }

        if (triple instanceof ConceptTypeVisalloRdfTriple) {
            setConceptType(elements, sourceFileName, (ConceptTypeVisalloRdfTriple) triple, user, authorizations);
            return true;
        }

        if (triple instanceof PropertyVisalloRdfTriple) {
            setProperty(
                    elements,
                    sourceFileName,
                    (PropertyVisalloRdfTriple) triple,
                    user,
                    authorizations
            );
            return true;
        }

        if (triple instanceof AddEdgeVisalloRdfTriple) {
            addEdge(elements, (AddEdgeVisalloRdfTriple) triple, authorizations);
            return true;
        }

        throw new VisalloException("Unexpected triple type: " + triple.getClass().getName());
    }

    private void addEdge(Set<Element> elements, AddEdgeVisalloRdfTriple triple, Authorizations authorizations) {
        Edge edge = graph.addEdge(
                triple.getEdgeId(),
                triple.getOutVertexId(),
                triple.getInVertexId(),
                triple.getEdgeLabel(),
                triple.getEdgeVisibility(),
                authorizations
        );
        elements.add(edge);
    }

    private void setProperty(
            Set<Element> elements,
            String sourceFileName,
            PropertyVisalloRdfTriple triple,
            User user,
            Authorizations authorizations
    ) {
        ElementMutation m;

        // metadata
        Date now = new Date();
        Metadata metadata = new Metadata();
        Visibility defaultVisibility = visibilityTranslator.getDefaultVisibility();
        if (sourceFileName != null) {
            VisalloProperties.SOURCE_FILE_NAME_METADATA.setMetadata(metadata, sourceFileName, defaultVisibility);
        }
        VisalloProperties.MODIFIED_DATE_METADATA.setMetadata(metadata, now, defaultVisibility);
        VisalloProperties.MODIFIED_BY_METADATA.setMetadata(metadata, user.getUserId(), defaultVisibility);
        VisalloProperties.CONFIDENCE_METADATA.setMetadata(metadata, GraphRepository.SET_PROPERTY_CONFIDENCE, defaultVisibility);
        VisalloProperties.VISIBILITY_JSON_METADATA.setMetadata(metadata, new VisibilityJson(triple.getPropertyVisibilitySource()), defaultVisibility);

        // metadata property
        if (triple instanceof SetMetadataVisalloRdfTriple) {
            SetMetadataVisalloRdfTriple setMetadataVisalloRdfTriple = (SetMetadataVisalloRdfTriple) triple;

            Element elem = getExistingElement(triple, authorizations);
            m = elem.prepareMutation();
            ((ExistingElementMutation) m).setPropertyMetadata(
                    triple.getPropertyKey(),
                    triple.getPropertyName(),
                    setMetadataVisalloRdfTriple.getMetadataName(),
                    triple.getValue(),
                    setMetadataVisalloRdfTriple.getMetadataVisibility()
            );
        } else {
            m = getMutationForUpdate(triple, authorizations);
            m.addPropertyValue(
                    triple.getPropertyKey(),
                    triple.getPropertyName(),
                    triple.getValue(),
                    metadata,
                    triple.getPropertyVisibility()
            );
        }

        Element element = m.save(authorizations);
        elements.add(element);
    }

    private ElementMutation getMutationForUpdate(PropertyVisalloRdfTriple triple, Authorizations authorizations) {
        if (triple.getElementType() == ElementType.VERTEX) {
            return graph.prepareVertex(triple.getElementId(), triple.getElementVisibility());
        } else {
            Edge element = (Edge) getExistingElement(triple, authorizations);
            return element.prepareMutation();
        }
    }

    private Element getExistingElement(PropertyVisalloRdfTriple triple, Authorizations authorizations) {
        Element elem = triple.getElementType() == ElementType.VERTEX
                ? graph.getVertex(triple.getElementId(), authorizations)
                : graph.getEdge(triple.getElementId(), authorizations);
        if (elem == null) {
            graph.flush();
            elem = triple.getElementType() == ElementType.VERTEX
                    ? graph.getVertex(triple.getElementId(), authorizations)
                    : graph.getEdge(triple.getElementId(), authorizations);
        }
        checkNotNull(elem, "Could not find element with id " + triple.getElementId());
        return elem;
    }

    private void setConceptType(
            Set<Element> elements,
            String sourceFileName,
            ConceptTypeVisalloRdfTriple triple,
            User user,
            Authorizations authorizations
    ) {
        Date now = new Date();
        Metadata metadata = new Metadata();
        Visibility defaultVisibility = visibilityTranslator.getDefaultVisibility();
        if (sourceFileName != null) {
            VisalloProperties.SOURCE_FILE_NAME_METADATA.setMetadata(metadata, sourceFileName, defaultVisibility);
        }
        VisibilityJson visibilityJson = new VisibilityJson(triple.getElementVisibilitySource());
        VisalloProperties.VISIBILITY_JSON_METADATA.setMetadata(metadata, visibilityJson, defaultVisibility);
        VisalloProperties.MODIFIED_DATE_METADATA.setMetadata(metadata, now, defaultVisibility);
        VisalloProperties.MODIFIED_BY_METADATA.setMetadata(metadata, user.getUserId(), defaultVisibility);
        VisalloProperties.CONFIDENCE_METADATA.setMetadata(metadata, GraphRepository.SET_PROPERTY_CONFIDENCE, defaultVisibility);

        VertexBuilder m = graph.prepareVertex(triple.getElementId(), triple.getElementVisibility());
        VisalloProperties.CONCEPT_TYPE.setProperty(m, triple.getConceptType(), metadata, triple.getElementVisibility());
        VisalloProperties.VISIBILITY_JSON.setProperty(m, visibilityJson, metadata, triple.getElementVisibility());
        Vertex vertex = m.save(authorizations);
        elements.add(vertex);
    }
}
