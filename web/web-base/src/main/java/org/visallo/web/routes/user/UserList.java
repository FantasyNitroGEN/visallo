package org.visallo.web.routes.user;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Optional;
import org.vertexium.util.ConvertingIterable;
import org.vertexium.util.FilterIterable;
import org.visallo.core.exception.VisalloResourceNotFoundException;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workspace.Workspace;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.model.workspace.WorkspaceUser;
import org.visallo.core.user.User;
import org.visallo.web.clientapi.model.ClientApiUsers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static org.vertexium.util.IterableUtils.toList;

public class UserList implements ParameterizedHandler {
    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;

    @Inject
    public UserList(
            final UserRepository userRepository,
            final WorkspaceRepository workspaceRepository
    ) {
        this.userRepository = userRepository;
        this.workspaceRepository = workspaceRepository;
    }

    @Handle
    public ClientApiUsers handle(
            User user,
            @Optional(name = "q") String query,
            @Optional(name = "workspaceId") String workspaceId,
            @Optional(name = "userIds[]") String[] userIds
    ) throws Exception {
        List<User> users;
        if (userIds != null) {
            checkArgument(query == null, "Cannot use userIds[] and q at the same time");
            checkArgument(workspaceId == null, "Cannot use userIds[] and workspaceId at the same time");
            users = new ArrayList<>();
            for (String userId : userIds) {
                User u = userRepository.findById(userId);
                if (u == null) {
                    throw new VisalloResourceNotFoundException("User " + userId + " not found");
                }
                users.add(u);
            }
        } else {
            users = toList(userRepository.find(query));

            if (workspaceId != null) {
                users = toList(getUsersWithWorkspaceAccess(workspaceId, users, user));
            }
        }

        Iterable<String> workspaceIds = getCurrentWorkspaceIds(users);
        Map<String, String> workspaceNames = getWorkspaceNames(workspaceIds, user);

        ClientApiUsers clientApiUsers = userRepository.toClientApi(users, workspaceNames);
        return clientApiUsers;
    }

    private Map<String, String> getWorkspaceNames(Iterable<String> workspaceIds, User user) {
        Map<String, String> result = new HashMap<>();
        for (Workspace workspace : workspaceRepository.findByIds(workspaceIds, user)) {
            if (workspace != null) {
                result.put(workspace.getWorkspaceId(), workspace.getDisplayTitle());
            }
        }
        return result;
    }

    private Iterable<String> getCurrentWorkspaceIds(Iterable<User> users) {
        return new ConvertingIterable<User, String>(users) {
            @Override
            protected String convert(User user) {
                return user.getCurrentWorkspaceId();
            }
        };
    }

    private Iterable<User> getUsersWithWorkspaceAccess(String workspaceId, final Iterable<User> users, User user) {
        final List<WorkspaceUser> usersWithAccess = workspaceRepository.findUsersWithAccess(workspaceId, user);
        return new FilterIterable<User>(users) {
            @Override
            protected boolean isIncluded(User u) {
                return contains(usersWithAccess, u);
            }

            private boolean contains(List<WorkspaceUser> usersWithAccess, User u) {
                for (WorkspaceUser userWithAccess : usersWithAccess) {
                    if (userWithAccess.getUserId().equals(u.getUserId())) {
                        return true;
                    }
                }
                return false;
            }
        };
    }
}
