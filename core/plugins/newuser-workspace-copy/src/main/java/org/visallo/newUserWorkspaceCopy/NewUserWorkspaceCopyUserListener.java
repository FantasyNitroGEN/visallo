package org.visallo.newUserWorkspaceCopy;

import com.google.inject.Inject;
import org.visallo.core.config.Configuration;
import org.visallo.core.model.user.DefaultUserListener;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workspace.Workspace;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

public class NewUserWorkspaceCopyUserListener extends DefaultUserListener {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(NewUserWorkspaceCopyUserListener.class);
    public static final String SETTING_WORKSPACE_ID = "newUserWorkspaceCopy.workspaceId";
    public static final String SETTING_NEW_WORKSPACE_TITLE = "newUserWorkspaceCopy.newWorkspaceTitle";
    private Configuration configuration;
    private WorkspaceRepository workspaceRepository;
    private UserRepository userRepository;

    @Override
    public void newUserAdded(User user) {
        try {
            String workspaceId = this.configuration.get(SETTING_WORKSPACE_ID, null);
            if (workspaceId == null) {
                LOGGER.warn("cannot find configuration parameter: %s", SETTING_WORKSPACE_ID);
                return;
            }

            Workspace workspace = this.workspaceRepository.findById(workspaceId, this.userRepository.getSystemUser());
            if (workspace == null) {
                LOGGER.warn("cannot find workspace: %s", workspaceId);
                return;
            }

            Workspace newWorkspace = this.workspaceRepository.copyTo(workspace, user, this.userRepository.getSystemUser());

            String newWorkspaceTitle = this.configuration.get(SETTING_NEW_WORKSPACE_TITLE, null);
            if (newWorkspaceTitle != null) {
                workspaceRepository.setTitle(newWorkspace, newWorkspaceTitle, user);
            }

            this.userRepository.setCurrentWorkspace(user.getUserId(), newWorkspace.getWorkspaceId());
        } catch (Exception ex) {
            LOGGER.error("Could not share workspace", ex);
        }
    }

    @Inject
    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    @Inject
    public void setWorkspaceRepository(WorkspaceRepository workspaceRepository) {
        this.workspaceRepository = workspaceRepository;
    }

    @Inject
    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
}
