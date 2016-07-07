package org.visallo.common.rdf;

import com.google.inject.Inject;
import com.hp.hpl.jena.shared.JenaException;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.TimeZone;

public class RdfImportHelper {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(RdfImportHelper.class);
    private final Graph graph;
    private final RdfXmlImportHelper rdfXmlImportHelper;
    private final RdfTripleImportHelper rdfTripleImportHelper;

    public void setFailOnFirstError(boolean failOnFirstError) {
        this.failOnFirstError = failOnFirstError;
    }

    private boolean failOnFirstError = false;

    @Inject
    public RdfImportHelper(
            Graph graph,
            RdfXmlImportHelper rdfXmlImportHelper,
            RdfTripleImportHelper rdfTripleImportHelper
    ) {
        this.graph = graph;
        this.rdfXmlImportHelper = rdfXmlImportHelper;
        this.rdfTripleImportHelper = rdfTripleImportHelper;
    }

    public void importRdf(
            File input,
            TimeZone timeZone,
            Priority priority,
            String visibilitySource,
            User user,
            Authorizations authorizations
    ) throws IOException {
        File inputFile = RdfFileLocator.findBestFile(input);

        LOGGER.info("Importing file: %s", inputFile.getAbsolutePath());
        rdfTripleImportHelper.setFailOnFirstError(failOnFirstError);
        if (inputFile.getName().endsWith(".nt")) {
            importFileRdfTriple(inputFile, timeZone, priority, visibilitySource, user, authorizations);
        } else {
            try {
                importFileRdfXml(inputFile, priority, visibilitySource, user, authorizations);
            } catch (JenaException ex) {
                if (ex.getMessage().contains("Content is not allowed in prolog.")) {
                    importFileRdfTriple(inputFile, timeZone, priority, visibilitySource, user, authorizations);
                } else {
                    throw ex;
                }
            }
        }
        graph.flush();
    }

    private void importFileRdfTriple(
            File inputFile,
            TimeZone timeZone,
            Priority priority,
            String visibilitySource,
            User user,
            Authorizations authorizations
    ) throws IOException {
        rdfTripleImportHelper.importRdfTriple(inputFile, timeZone, priority, visibilitySource, user, authorizations);
    }

    private void importFileRdfXml(
            File inputFile,
            Priority priority,
            String visibilitySource,
            User user,
            Authorizations authorizations
    ) throws IOException {
        rdfXmlImportHelper.importRdfXml(inputFile, null, priority, visibilitySource, user, authorizations);
    }
}
