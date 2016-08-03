package org.visallo.common.rdf;

import com.codahale.metrics.Timer;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import org.vertexium.*;
import org.vertexium.mutation.ElementMutation;
import org.vertexium.mutation.ExistingElementMutation;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.graph.GraphRepository;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.security.VisalloVisibility;
import org.visallo.core.security.VisibilityTranslator;
import org.visallo.core.status.MetricsManager;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.clientapi.model.VisibilityJson;

import java.io.*;
import java.util.*;

public class RdfTripleImportHelper {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(RdfTripleImportHelper.class);
    private static final String MULTIVALUE_KEY = RdfTripleImportHelper.class.getName();
    private final Graph graph;
    private final VisibilityTranslator visibilityTranslator;
    private WorkQueueRepository workQueueRepository;
    private final MetricsManager metricsManager;
    private static final Map<String, Visibility> visibilityCache = new HashMap<>();

    public void setFailOnFirstError(boolean failOnFirstError) {
        this.failOnFirstError = failOnFirstError;
    }

    private boolean failOnFirstError = false;

    @Inject
    public RdfTripleImportHelper(
            Graph graph,
            VisibilityTranslator visibilityTranslator,
            WorkQueueRepository workQueueRepository,
            MetricsManager metricsManager
    ) {
        this.graph = graph;
        this.visibilityTranslator = visibilityTranslator;
        this.workQueueRepository = workQueueRepository;
        this.metricsManager = metricsManager;
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
        long startTime = System.currentTimeMillis();
        Set<Element> elements = new HashSet<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        int lineNum = 1;
        String line;
        ImportContext ctx = null;
        String timerMetricName = metricsManager.getNamePrefix(this);
        Timer timer = metricsManager.timer(timerMetricName);
        try {
            while ((line = reader.readLine()) != null) {
                LOGGER.debug("Importing RDF triple on line: %d. Rate: %.2f / sec", lineNum, timer.getMeanRate());
                try (Timer.Context timerContext = timer.time()) {
                    ctx = importRdfLine(ctx, elements, sourceFileName, line, workingDir, timeZone, defaultVisibilitySource, user, authorizations);
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
            if (ctx != null) {
                elements.add(ctx.save(authorizations));
            }
        } finally {
            metricsManager.removeMetric(timerMetricName);
        }

        graph.flush();
        LOGGER.info("pushing %d elements from RDF import on to work queue", elements.size());
        workQueueRepository.pushElements(elements, priority);

        long endTime = System.currentTimeMillis();
        LOGGER.debug("RDF %s imported in %dms", sourceFileName, endTime - startTime);
    }

    @VisibleForTesting
    ImportContext importRdfLine(
            ImportContext ctx,
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
            return ctx;
        }
        RdfTriple rdfTriple = RdfTripleParser.parseLine(line);

        return importRdfTriple(ctx, elements, rdfTriple, sourceFileName, workingDir, timeZone, defaultVisibilitySource, user, authorizations);
    }

    private ImportContext importRdfTriple(
            ImportContext ctx,
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
            throw new VisalloException("Unhandled combination of RDF triples. First triple expected to be a URI, but was " + rdfTriple.getFirst().getClass().getName());
        }
        if (!(rdfTriple.getSecond() instanceof RdfTriple.UriPart)) {
            throw new VisalloException("Unhandled combination of RDF triples. Second triple expected to be a URI, but was " + rdfTriple.getFirst().getClass().getName());
        }

        VisalloRdfTriple triple = VisalloRdfTriple.parse(
                rdfTriple,
                defaultVisibilitySource,
                workingDir,
                timeZone
        );
        if (triple == null) {
            throw new VisalloException("Unhandled combination of RDF triples");
        }

        if (ctx == null || ctx.isNewElement(triple)) {
            if (ctx != null) {
                elements.add(ctx.save(authorizations));
            }
            ctx = triple.updateImportContext(ctx, this, authorizations);
        }

        if (triple instanceof ConceptTypeVisalloRdfTriple) {
            setConceptType(ctx, sourceFileName, (ConceptTypeVisalloRdfTriple) triple, user);
            return ctx;
        }

        if (triple instanceof PropertyVisalloRdfTriple) {
            setProperty(ctx, sourceFileName, (PropertyVisalloRdfTriple) triple, user);
            return ctx;
        }

        if (triple instanceof AddEdgeVisalloRdfTriple) {
            // handled by ImportContext
            return ctx;
        }

        throw new VisalloException("Unexpected triple type: " + triple.getClass().getName());
    }

    private void setProperty(
            ImportContext ctx,
            String sourceFileName,
            PropertyVisalloRdfTriple triple,
            User user
    ) {
        ElementMutation m = ctx.getElementMutation();

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
        if (!isLiteralVisibilityString(triple.getPropertyVisibilitySource())) {
            VisalloProperties.VISIBILITY_JSON_METADATA.setMetadata(metadata, new VisibilityJson(triple.getPropertyVisibilitySource()), defaultVisibility);
        }

        // metadata property
        if (triple instanceof SetMetadataVisalloRdfTriple) {
            SetMetadataVisalloRdfTriple setMetadataVisalloRdfTriple = (SetMetadataVisalloRdfTriple) triple;

            ((ExistingElementMutation) m).setPropertyMetadata(
                    triple.getPropertyKey(),
                    triple.getPropertyName(),
                    setMetadataVisalloRdfTriple.getMetadataName(),
                    triple.getValue(),
                    getVisibility(setMetadataVisalloRdfTriple.getMetadataVisibilitySource())
            );
        } else {
            m.addPropertyValue(
                    triple.getPropertyKey(),
                    triple.getPropertyName(),
                    triple.getValue(),
                    metadata,
                    getVisibility(triple.getPropertyVisibilitySource())
            );
        }
    }

    private void setConceptType(
            ImportContext ctx,
            String sourceFileName,
            ConceptTypeVisalloRdfTriple triple,
            User user
    ) {
        Date now = new Date();
        Visibility defaultVisibility = visibilityTranslator.getDefaultVisibility();

        Visibility elementVisibility = getVisibility(triple.getElementVisibilitySource());
        ElementMutation m = ctx.getElementMutation();
        VisalloProperties.CONCEPT_TYPE.setProperty(m, triple.getConceptType(), defaultVisibility);
        if (!isLiteralVisibilityString(triple.getElementVisibilitySource())) {
            VisibilityJson visibilityJson = new VisibilityJson(triple.getElementVisibilitySource());
            VisalloProperties.VISIBILITY_JSON.setProperty(m, visibilityJson, defaultVisibility);
        }
        VisalloProperties.MODIFIED_BY.setProperty(m, user.getUserId(), defaultVisibility);
        VisalloProperties.MODIFIED_DATE.setProperty(m, now, defaultVisibility);
        VisalloProperties.SOURCE.addPropertyValue(m, MULTIVALUE_KEY, sourceFileName, elementVisibility);
    }

    Visibility getVisibility(String visibilityString) {
        Visibility visibility = visibilityCache.get(visibilityString);
        if (visibility != null) {
            return visibility;
        }
        if (isLiteralVisibilityString(visibilityString)) {
            visibility = new VisalloVisibility(visibilityString.substring(1)).getVisibility();
        } else {
            visibility = visibilityTranslator.toVisibility(visibilityString).getVisibility();
        }
        visibilityCache.put(visibilityString, visibility);
        return visibility;
    }

    private boolean isLiteralVisibilityString(String visibilitySource) {
        return visibilitySource != null && visibilitySource.startsWith("!");
    }

    Graph getGraph() {
        return graph;
    }
}
