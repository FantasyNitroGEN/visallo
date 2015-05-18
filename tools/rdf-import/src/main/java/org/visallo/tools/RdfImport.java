package org.visallo.tools;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.converters.FileConverter;
import org.visallo.core.bootstrap.InjectHelper;
import org.visallo.core.cmdline.CommandLineTool;
import org.visallo.core.cmdline.converters.WorkQueuePriorityConverter;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.security.VisalloVisibility;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.rdf.RdfGraphPropertyWorker;
import org.vertexium.Visibility;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.List;

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

    public static void main(String[] args) throws Exception {
        CommandLineTool.main(new RdfImport(), args);
    }

    @Override
    protected int run() throws Exception {
        VisalloVisibility visibility = new VisalloVisibility(visibilitySource);
        importInFiles(inFiles, visibility.getVisibility());
        importInDirs(inDirs, pattern, visibility.getVisibility());
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
        RdfGraphPropertyWorker rdfGraphPropertyWorker = InjectHelper.getInstance(RdfGraphPropertyWorker.class);
        rdfGraphPropertyWorker.importRdf(getGraph(), inputFile, null, visibility, priority, getAuthorizations());
        getGraph().flush();
    }
}
