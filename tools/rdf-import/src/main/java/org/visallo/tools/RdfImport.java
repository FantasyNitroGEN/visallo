package org.visallo.tools;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.converters.FileConverter;
import com.hp.hpl.jena.shared.JenaException;
import org.vertexium.Metadata;
import org.vertexium.Visibility;
import org.visallo.core.bootstrap.InjectHelper;
import org.visallo.core.cmdline.CommandLineTool;
import org.visallo.core.cmdline.converters.WorkQueuePriorityConverter;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.graph.GraphRepository;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.rdf.RdfGraphPropertyWorker;
import org.visallo.web.clientapi.model.VisibilityJson;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

@Parameters(commandDescription = "Import RDF data into the system")
public class RdfImport extends CommandLineTool {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(RdfImport.class);

    @Parameter(names = {"--infile", "-i"}, arity = 1, converter = FileConverter.class, description = "Input file")
    private List<File> inFiles = new ArrayList<>();

    @Parameter(names = {"--indir"}, arity = 1, converter = FileConverter.class, description = "Input directory")
    private List<File> inDirs = new ArrayList<>();

    @Parameter(names = {"--pattern"}, arity = 1, description = "Input directory pattern [default: *.{xml}]")
    private String pattern = "*.{xml}";

    @Parameter(names = {"--visibility", "-v"}, arity = 1, description = "The visibility of newly created items")
    private String visibilitySource;

    @Parameter(names = {"--priority", "-p"}, arity = 1, converter = WorkQueuePriorityConverter.class, description = "Priority at which to enqueue")
    private Priority priority = Priority.NORMAL;

    @Parameter(names = {"--timezone"}, arity = 1, description = "The Java timezone id")
    private String timeZoneId = "GMT";

    public static void main(String[] args) throws Exception {
        CommandLineTool.main(new RdfImport(), args);
    }

    @Override
    protected int run() throws Exception {
        Visibility visibility = getVisibilityTranslator().toVisibility(visibilitySource).getVisibility();
        importInFiles(inFiles, visibility);
        importInDirs(inDirs, pattern, visibility);
        return 0;
    }

    private void importInDirs(List<File> inputDirs, String pattern, Visibility visibility) throws IOException {
        for (File inputDir : inputDirs) {
            if (!inputDir.exists()) {
                throw new VisalloException("Could not find input directory: " + inputDir.getAbsolutePath());
            }
            PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
            File[] files = inputDir.listFiles();
            if (files == null) {
                continue;
            }
            for (File inputFile : files) {
                Path fileNamePath = FileSystems.getDefault().getPath(inputFile.getName());
                if (matcher.matches(fileNamePath)) {
                    importFile(inputFile, visibility);
                }
            }
        }
    }

    private void importInFiles(List<File> inFiles, Visibility visibility) throws IOException {
        for (File inputFile : inFiles) {
            if (!inputFile.exists()) {
                throw new VisalloException("Could not find file: " + inputFile.getAbsolutePath());
            }
            importFile(inputFile, visibility);
        }
    }

    private void importFile(File inputFile, Visibility visibility) throws IOException {
        LOGGER.info("Importing file: %s", inputFile.getAbsolutePath());
        if (inputFile.getName().endsWith(".nt")) {
            importFileRdfTriple(inputFile, visibility);
        } else {
            try {
                importFileRdfXml(inputFile, visibility);
            } catch (JenaException ex) {
                if (ex.getMessage().contains("Content is not allowed in prolog.")) {
                    importFileRdfTriple(inputFile, visibility);
                } else {
                    throw ex;
                }
            }
        }
        getGraph().flush();
    }

    private void importFileRdfTriple(File inputFile, Visibility visibility) throws IOException {
        TimeZone timeZone = TimeZone.getTimeZone(timeZoneId);
        RdfTripleImport rdfTripleImport = new RdfTripleImport(getGraph(), timeZone, visibility, getAuthorizations());
        Metadata metadata = new Metadata();
        Date now = new Date();
        Visibility metadataVisibility = getVisibilityTranslator().getDefaultVisibility();
        VisibilityJson visibilityJson = new VisibilityJson(visibility.getVisibilityString());
        User user = getUserRepository().getSystemUser();
        VisalloProperties.SOURCE_FILE_NAME_METADATA.setMetadata(metadata, inputFile.getName(), metadataVisibility);
        VisalloProperties.VISIBILITY_JSON_METADATA.setMetadata(metadata, visibilityJson, metadataVisibility);
        VisalloProperties.MODIFIED_DATE_METADATA.setMetadata(metadata, now, metadataVisibility);
        VisalloProperties.MODIFIED_BY_METADATA.setMetadata(metadata, user.getUserId(), metadataVisibility);
        VisalloProperties.CONFIDENCE_METADATA.setMetadata(metadata, GraphRepository.SET_PROPERTY_CONFIDENCE, metadataVisibility);
        rdfTripleImport.importRdf(inputFile, metadata);
    }

    private void importFileRdfXml(File inputFile, Visibility visibility) throws IOException {
        RdfGraphPropertyWorker rdfGraphPropertyWorker = InjectHelper.getInstance(RdfGraphPropertyWorker.class);
        rdfGraphPropertyWorker.importRdf(getGraph(), inputFile, null, visibility, priority, getAuthorizations());
    }
}
