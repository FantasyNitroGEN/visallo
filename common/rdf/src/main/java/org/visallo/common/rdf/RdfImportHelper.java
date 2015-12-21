package org.visallo.common.rdf;

import com.google.inject.Inject;
import com.hp.hpl.jena.shared.JenaException;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.Metadata;
import org.vertexium.Visibility;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.graph.GraphRepository;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.security.VisibilityTranslator;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.clientapi.model.VisibilityJson;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.TimeZone;

public class RdfImportHelper {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(RdfImportHelper.class);
    private final Graph graph;
    private final VisibilityTranslator visibilityTranslator;
    private final UserRepository userRepository;
    private final RdfXmlImportHelper rdfXmlImportHelper;
    private final RdfTripleImportHelper rdfTripleImportHelper;

    @Inject
    public RdfImportHelper(
            Graph graph,
            VisibilityTranslator visibilityTranslator,
            UserRepository userRepository,
            RdfXmlImportHelper rdfXmlImportHelper,
            RdfTripleImportHelper rdfTripleImportHelper
    ) {
        this.graph = graph;
        this.visibilityTranslator = visibilityTranslator;
        this.userRepository = userRepository;
        this.rdfXmlImportHelper = rdfXmlImportHelper;
        this.rdfTripleImportHelper = rdfTripleImportHelper;
    }

    public void importRdf(
            File input,
            TimeZone timeZone,
            Priority priority,
            Visibility visibility,
            User user,
            Authorizations authorizations
    ) throws IOException {
        File inputFile = findRdfFile(input);

        LOGGER.info("Importing file: %s", inputFile.getAbsolutePath());
        if (inputFile.getName().endsWith(".nt")) {
            importFileRdfTriple(inputFile, timeZone, visibility, authorizations);
        } else {
            try {
                importFileRdfXml(inputFile, priority, visibility, user, authorizations);
            } catch (JenaException ex) {
                if (ex.getMessage().contains("Content is not allowed in prolog.")) {
                    importFileRdfTriple(inputFile, timeZone, visibility, authorizations);
                } else {
                    throw ex;
                }
            }
        }
        graph.flush();
    }

    private File findRdfFile(File input) {
        if (input.isFile()) {
            return input;
        }

        File firstFile = null;
        File[] files = input.listFiles();
        if (files == null) {
            throw new VisalloException("Could not get files from: " + input.getAbsolutePath());
        }
        for (File file : files) {
            if (file.isFile()) {
                if (firstFile == null) {
                    firstFile = file;
                }
                String fileName = file.getAbsolutePath().toLowerCase();
                if (fileName.endsWith(".nt") || fileName.endsWith(".xml")) {
                    return file;
                }
            }
        }
        if (firstFile == null) {
            throw new VisalloException("Could not find RDF file in directory: " + input.getAbsolutePath());
        }
        return firstFile;
    }

    private void importFileRdfTriple(
            File inputFile,
            TimeZone timeZone,
            Visibility defaultVisibility,
            Authorizations authorizations
    ) throws IOException {
        Metadata metadata = new Metadata();
        Date now = new Date();
        Visibility metadataVisibility = visibilityTranslator.getDefaultVisibility();
        VisibilityJson visibilityJson = new VisibilityJson(defaultVisibility.getVisibilityString());
        User user = userRepository.getSystemUser();
        VisalloProperties.SOURCE_FILE_NAME_METADATA.setMetadata(metadata, inputFile.getName(), metadataVisibility);
        VisalloProperties.VISIBILITY_JSON_METADATA.setMetadata(metadata, visibilityJson, metadataVisibility);
        VisalloProperties.MODIFIED_DATE_METADATA.setMetadata(metadata, now, metadataVisibility);
        VisalloProperties.MODIFIED_BY_METADATA.setMetadata(metadata, user.getUserId(), metadataVisibility);
        VisalloProperties.CONFIDENCE_METADATA.setMetadata(metadata, GraphRepository.SET_PROPERTY_CONFIDENCE, metadataVisibility);
        rdfTripleImportHelper.importRdfTriple(inputFile, metadata, timeZone, defaultVisibility, authorizations);
    }

    private void importFileRdfXml(
            File inputFile,
            Priority priority,
            Visibility visibility,
            User user,
            Authorizations authorizations
    ) throws IOException {
        rdfXmlImportHelper.importRdfXml(inputFile, null, visibility, priority, user, authorizations);
    }
}
