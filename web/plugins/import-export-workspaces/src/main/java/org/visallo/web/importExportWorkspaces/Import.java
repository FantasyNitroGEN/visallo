package org.visallo.web.importExportWorkspaces;

import com.v5analytics.webster.HandlerChain;
import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.vertexium.model.user.VertexiumUserRepository;
import org.visallo.vertexium.model.workspace.VertexiumWorkspaceRepository;
import org.visallo.web.BaseRequestHandler;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.Vertex;
import org.vertexium.tools.GraphRestore;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Import extends BaseRequestHandler {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(Import.class);
    private final VertexiumWorkspaceRepository workspaceRepository;
    private final Graph graph;

    @Inject
    public Import(
            UserRepository userRepository,
            Configuration configuration,
            WorkspaceRepository workspaceRepository,
            Graph graph) {
        super(userRepository, workspaceRepository, configuration);
        this.workspaceRepository = (VertexiumWorkspaceRepository) workspaceRepository;
        this.graph = graph;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        if (!ServletFileUpload.isMultipartContent(request)) {
            LOGGER.warn("Could not process request without multi-part content");
            respondWithBadRequest(response, "file", "Could not process request without multi-part content");
            return;
        }

        User user = getUser(request);
        Vertex userVertex = ((VertexiumUserRepository) getUserRepository()).findByIdUserVertex(user.getUserId());
        GraphRestore graphRestore = new GraphRestore();

        for (Part part : request.getParts()) {
            if (part.getName().equals("workspace")) {
                File outFile = File.createTempFile("visalloWorkspaceImport", "visalloworkspace");
                copyPartToFile(part, outFile);

                String workspaceId = getWorkspaceId(outFile);

                Authorizations authorizations = getUserRepository().getAuthorizations(user, UserRepository.VISIBILITY_STRING, WorkspaceRepository.VISIBILITY_STRING, workspaceId);

                try (InputStream in = new FileInputStream(outFile)) {
                    graphRestore.restore(graph, in, authorizations);

                    Vertex workspaceVertex = this.workspaceRepository.getVertex(workspaceId, user);
                    this.workspaceRepository.addWorkspaceToUser(workspaceVertex, userVertex, authorizations);
                } finally {
                    graph.flush();
                }
            }
        }

        respondWithPlaintext(response, "Workspace Imported");
    }

    private String getWorkspaceId(File file) throws IOException {
        BufferedReader in = new BufferedReader(new FileReader(file));
        String workspaceLine = in.readLine();
        Matcher m = Pattern.compile(".*\"id\":\"(.*?)\".*").matcher(workspaceLine);
        if (!m.matches()) {
            throw new VisalloException("Could not find Workspace id in line: " + workspaceLine);
        }
        return m.group(1);
    }
}
