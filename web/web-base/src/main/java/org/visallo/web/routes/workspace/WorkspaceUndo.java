package org.visallo.web.routes.workspace;

import com.google.common.base.Joiner;
import com.google.inject.Inject;
import com.v5analytics.webster.HandlerChain;
import org.vertexium.Authorizations;
import org.visallo.core.config.Configuration;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.model.workspace.WorkspaceUndoHelper;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.BaseRequestHandler;
import org.visallo.web.clientapi.model.ClientApiUndoItem;
import org.visallo.web.clientapi.model.ClientApiWorkspaceUndoResponse;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;

public class WorkspaceUndo extends BaseRequestHandler {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(WorkspaceUndo.class);
    private String entityHasImageIri;
    private String artifactContainsImageOfEntityIri;
    private final WorkspaceUndoHelper workspaceUndoHelper;
    private final OntologyRepository ontologyRepository;

    @Inject
    public WorkspaceUndo(
            final Configuration configuration,
            final UserRepository userRepository,
            final WorkspaceUndoHelper workspaceUndoHelper,
            final WorkspaceRepository workspaceRepository,
            final OntologyRepository ontologyRepository) {
        super(userRepository, workspaceRepository, configuration);
        this.workspaceUndoHelper = workspaceUndoHelper;
        this.ontologyRepository = ontologyRepository;

        this.entityHasImageIri = ontologyRepository.getRelationshipIRIByIntent("entityHasImage");
        if (this.entityHasImageIri == null) {
            LOGGER.warn("'entityHasImage' intent has not been defined. Please update your ontology.");
        }

        this.artifactContainsImageOfEntityIri = ontologyRepository.getRelationshipIRIByIntent("artifactContainsImageOfEntity");
        if (this.artifactContainsImageOfEntityIri == null) {
            LOGGER.warn("'artifactContainsImageOfEntity' intent has not been defined. Please update your ontology.");
        }
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        if (this.entityHasImageIri == null) {
            this.entityHasImageIri = ontologyRepository.getRequiredRelationshipIRIByIntent("entityHasImage");
        }
        if (this.artifactContainsImageOfEntityIri == null) {
            this.artifactContainsImageOfEntityIri = ontologyRepository.getRequiredRelationshipIRIByIntent("artifactContainsImageOfEntity");
        }

        String undoDataString = getRequiredParameter(request, "undoData");
        ClientApiUndoItem[] undoData = getObjectMapper().readValue(undoDataString, ClientApiUndoItem[].class);
        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);
        String workspaceId = getActiveWorkspaceId(request);

        LOGGER.debug("undoing:\n%s", Joiner.on("\n").join(undoData));
        ClientApiWorkspaceUndoResponse workspaceUndoResponse = new ClientApiWorkspaceUndoResponse();
        workspaceUndoHelper.undo(Arrays.asList(undoData), workspaceUndoResponse, workspaceId, user, authorizations);
        LOGGER.debug("undoing results: %s", workspaceUndoResponse);
        respondWithClientApiObject(response, workspaceUndoResponse);
    }
}
