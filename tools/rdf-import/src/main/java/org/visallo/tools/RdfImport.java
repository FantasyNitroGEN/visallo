package org.visallo.tools;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.converters.FileConverter;
import com.google.inject.Inject;
import org.vertexium.Visibility;
import org.visallo.common.rdf.RdfImportHelper;
import org.visallo.core.cmdline.CommandLineTool;
import org.visallo.core.cmdline.converters.WorkQueuePriorityConverter;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.workQueue.Priority;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

@Parameters(commandDescription = "Import RDF data into the system")
public class RdfImport extends CommandLineTool {
    private RdfImportHelper rdfImportHelper;

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

    @Parameter(names = {"--failOnError"},  arity = 1, description = "If true, import process fails on first error")
    private boolean failOnFirstError = false;

    public static void main(String[] args) throws Exception {
        CommandLineTool.main(new RdfImport(), args);
    }

    @Override
    protected int run() throws Exception {
        rdfImportHelper.setFailOnFirstError(failOnFirstError);
        Visibility visibility = getVisibilityTranslator().toVisibility(visibilitySource).getVisibility();
        TimeZone timeZone = TimeZone.getTimeZone(timeZoneId);
        importInFiles(inFiles, timeZone, visibility);
        importInDirs(inDirs, pattern, timeZone, visibility);
        return 0;
    }

    private void importInDirs(List<File> inputDirs, String pattern, TimeZone timeZone, Visibility visibility) throws IOException {
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
                    rdfImportHelper.importRdf(inputFile, timeZone, priority, visibility, getUser(), getAuthorizations());
                }
            }
        }
    }

    private void importInFiles(List<File> inFiles, TimeZone timeZone, Visibility visibility) throws IOException {
        for (File inputFile : inFiles) {
            if (!inputFile.exists()) {
                throw new VisalloException("Could not find file: " + inputFile.getAbsolutePath());
            }
            rdfImportHelper.importRdf(inputFile, timeZone, priority, visibility, getUser(), getAuthorizations());
        }
    }

    @Inject
    public void setRdfImportHelper(RdfImportHelper rdfImportHelper) {
        this.rdfImportHelper = rdfImportHelper;
    }
}
