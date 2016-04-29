package org.visallo.web.routes.workspace;

import com.google.common.base.Joiner;
import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Required;
import org.vertexium.Authorizations;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.workspace.WorkspaceUndoHelper;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.clientapi.model.ClientApiUndoItem;
import org.visallo.web.clientapi.model.ClientApiWorkspaceUndoResponse;
import org.visallo.web.parameterProviders.ActiveWorkspaceId;

import java.util.Arrays;

public class WorkspaceUndo implements ParameterizedHandler {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(WorkspaceUndo.class);
    private final WorkspaceUndoHelper workspaceUndoHelper;

    @Inject
    public WorkspaceUndo(
            final WorkspaceUndoHelper workspaceUndoHelper,
            final OntologyRepository ontologyRepository
    ) {
        this.workspaceUndoHelper = workspaceUndoHelper;

        String entityHasImageIri = ontologyRepository.getRelationshipIRIByIntent("entityHasImage");
        if (entityHasImageIri == null) {
            LOGGER.warn("'entityHasImage' intent has not been defined. Please update your ontology.");
        }

        String artifactContainsImageOfEntityIri = ontologyRepository.getRelationshipIRIByIntent("artifactContainsImageOfEntity");
        if (artifactContainsImageOfEntityIri == null) {
            LOGGER.warn("'artifactContainsImageOfEntity' intent has not been defined. Please update your ontology.");
        }
    }

    @Handle
    public ClientApiWorkspaceUndoResponse handle(
            @Required(name = "undoData") ClientApiUndoItem[] undoData,
            @ActiveWorkspaceId String workspaceId,
            User user,
            Authorizations authorizations
    ) throws Exception {
        LOGGER.debug("undoing:\n%s", Joiner.on("\n").join(undoData));
        ClientApiWorkspaceUndoResponse workspaceUndoResponse = new ClientApiWorkspaceUndoResponse();
        workspaceUndoHelper.undo(Arrays.asList(undoData), workspaceUndoResponse, workspaceId, user, authorizations);
        LOGGER.debug("undoing results: %s", workspaceUndoResponse);
        return workspaceUndoResponse;
    }
}
