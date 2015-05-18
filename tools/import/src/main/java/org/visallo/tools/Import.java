package org.visallo.tools;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.converters.FileConverter;
import com.google.inject.Inject;
import org.visallo.core.cmdline.CommandLineTool;
import org.visallo.core.cmdline.converters.WorkQueuePriorityConverter;
import org.visallo.core.ingest.FileImport;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.model.workspace.Workspace;
import org.visallo.core.model.workspace.WorkspaceRepository;

import java.io.File;

@Parameters(commandDescription = "Import data into the system")
public class Import extends CommandLineTool {
    private FileImport fileImport;
    private WorkspaceRepository workspaceRepository;

    @Parameter(names = {"--datadir"}, required = true, arity = 1, converter = FileConverter.class, description = "Location of the data directory")
    private File dataDir;

    @Parameter(names = {"--queuedups"}, description = "Specify if you would like to queue duplicate files")
    private boolean queueDuplicates = false;

    @Parameter(names = {"--visibilitysource"}, arity = 1, description = "The visibility source data")
    private String visibilitySource;

    @Parameter(names = {"--workspaceid"}, arity = 1, description = "The workspace id to import the files into")
    private String workspaceId;

    @Parameter(names = {"--priority", "-p"}, arity = 1, converter = WorkQueuePriorityConverter.class, description = "Priority at which to enqueue new files")
    private Priority priority = Priority.NORMAL;

    @Parameter(names = {"--conceptTypeIRI"}, arity = 1, description = "IRI of the concept type to force for all imported files")
    private String conceptTypeIRI;

    public static void main(String[] args) throws Exception {
        CommandLineTool.main(new Import(), args);
    }

    @Override
    protected int run() throws Exception {
        Workspace workspace;
        if (workspaceId == null) {
            workspace = null;
        } else {
            workspace = workspaceRepository.findById(workspaceId, getUser());
        }
        fileImport.importDirectory(dataDir, queueDuplicates, conceptTypeIRI, visibilitySource, workspace, priority, getUser(), getAuthorizations());
        return 0;
    }

    @Inject
    public void setFileImport(FileImport fileImport) {
        this.fileImport = fileImport;
    }

    @Inject
    public void setWorkspaceRepository(WorkspaceRepository workspaceRepository) {
        this.workspaceRepository = workspaceRepository;
    }
}
