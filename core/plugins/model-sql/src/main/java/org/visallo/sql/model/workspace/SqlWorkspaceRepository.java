package org.visallo.sql.model.workspace;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.Vertex;
import org.vertexium.util.ConvertingIterable;
import org.visallo.core.exception.VisalloAccessDeniedException;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.termMention.TermMentionRepository;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.model.workspace.Workspace;
import org.visallo.core.model.workspace.WorkspaceEntity;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.model.workspace.WorkspaceUser;
import org.visallo.core.security.VisibilityTranslator;
import org.visallo.core.user.ProxyUser;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.sql.model.HibernateSessionManager;
import org.visallo.sql.model.user.SqlUser;
import org.visallo.sql.model.user.SqlUserRepository;
import org.visallo.web.clientapi.model.ClientApiWorkspaceDiff;
import org.visallo.web.clientapi.model.WorkspaceAccess;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.vertexium.util.IterableUtils.toList;

@Singleton
public class SqlWorkspaceRepository extends WorkspaceRepository {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(SqlWorkspaceRepository.class);
    private final SqlUserRepository userRepository;
    private final HibernateSessionManager sessionManager;

    @Inject
    public SqlWorkspaceRepository(
            final SqlUserRepository userRepository,
            final HibernateSessionManager sessionManager,
            final Graph graph,
            final VisibilityTranslator visibilityTranslator,
            final TermMentionRepository termMentionRepository,
            final OntologyRepository ontologyRepository,
            final WorkQueueRepository workQueueRepository
    ) {
        super(
                graph,
                visibilityTranslator,
                termMentionRepository,
                ontologyRepository,
                workQueueRepository
        );
        this.userRepository = userRepository;
        this.sessionManager = sessionManager;
    }

    @Override
    public void delete(Workspace workspace, User user) {
        if (!hasWritePermissions(workspace.getWorkspaceId(), user)) {
            throw new VisalloAccessDeniedException("user " + user.getUserId() + " does not have write access to workspace " + workspace.getWorkspaceId(), user, workspace.getWorkspaceId());
        }

        Session session = sessionManager.getSession();
        Transaction transaction = null;
        try {
            transaction = session.beginTransaction();
            String nullCurrentWorkspacesSql = "update " + SqlUser.class.getSimpleName() + " set current_workspace_id = null where current_workspace_id = :workspaceId";
            Query query = session.createQuery(nullCurrentWorkspacesSql).setString("workspaceId", workspace.getWorkspaceId());
            session.delete(workspace);
            query.executeUpdate();
            transaction.commit();
        } catch (HibernateException e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new RuntimeException(e);
        }
    }

    @Override
    public Workspace findById(String workspaceId, boolean includeHidden, User user) {
        // TODO support includeHidden
        Session session = sessionManager.getSession();
        List workspaces = session.createCriteria(SqlWorkspace.class).add(Restrictions.eq("workspaceId", workspaceId)).list();
        if (workspaces.size() == 0) {
            return null;
        } else if (workspaces.size() > 1) {
            throw new VisalloException("more than one workspace was returned");
        } else {
            if (!hasReadPermissions(workspaceId, user)) {
                throw new VisalloAccessDeniedException("user " + user.getUserId() + " does not have read access to workspace " + workspaceId, user, workspaceId);
            }
            return (SqlWorkspace) workspaces.get(0);
        }
    }

    @Override
    public Workspace add(String workspaceId, String title, User user) {
        if (workspaceId == null) {
            workspaceId = WORKSPACE_ID_PREFIX + getGraph().getIdGenerator().nextId();
        }

        Session session = sessionManager.getSession();

        Transaction transaction = null;
        SqlWorkspace newWorkspace;
        try {
            transaction = session.beginTransaction();
            newWorkspace = new SqlWorkspace();
            newWorkspace.setDisplayTitle(title);
            if (user instanceof ProxyUser) {
                user = userRepository.findById(user.getUserId());
            }
            newWorkspace.setWorkspaceCreator((SqlUser) user);
            newWorkspace.setWorkspaceId(workspaceId);
            SqlWorkspaceUser sqlWorkspaceUser = new SqlWorkspaceUser();
            sqlWorkspaceUser.setWorkspaceAccess(WorkspaceAccess.WRITE);
            sqlWorkspaceUser.setUser((SqlUser) user);
            sqlWorkspaceUser.setWorkspace(newWorkspace);

            LOGGER.debug("add %s to workspace table", title);
            newWorkspace.getSqlWorkspaceUserList().add(sqlWorkspaceUser);
            session.save(newWorkspace);
            session.save(sqlWorkspaceUser);
            session.update(user);
            transaction.commit();
        } catch (HibernateException e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new RuntimeException(e);
        }
        return newWorkspace;
    }

    @Override
    public Iterable<Workspace> findAllForUser(User user) {
        Session session = sessionManager.getSession();
        List workspaces = session.createCriteria(SqlWorkspaceUser.class)
                .add(Restrictions.eq("sqlWorkspaceUser.user.userId", user.getUserId()))
                .add(Restrictions.in("workspaceAccess", new String[]{WorkspaceAccess.READ.toString(), WorkspaceAccess.WRITE.toString()}))
                .list();
        return new ConvertingIterable<Object, Workspace>(workspaces) {
            @Override
            protected Workspace convert(Object obj) {
                SqlWorkspaceUser sqlWorkspaceUser = (SqlWorkspaceUser) obj;
                return sqlWorkspaceUser.getWorkspace();
            }
        };
    }

    @Override
    public void setTitle(Workspace workspace, String title, User user) {
        Session session = sessionManager.getSession();
        if (!hasWritePermissions(workspace.getWorkspaceId(), user)) {
            throw new VisalloAccessDeniedException("user " + user.getUserId() + " does not have write access to workspace " + workspace.getWorkspaceId(), user, workspace.getWorkspaceId());
        }

        Transaction transaction = null;
        try {
            transaction = session.beginTransaction();
            ((SqlWorkspace) workspace).setDisplayTitle(title);
            session.update(workspace);
            transaction.commit();
        } catch (HibernateException e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<WorkspaceUser> findUsersWithAccess(String workspaceId, User user) {
        List<WorkspaceUser> withAccess = new ArrayList<>();
        List<SqlWorkspaceUser> sqlWorkspaceUsers = getSqlWorkspaceUserLists(workspaceId);

        for (SqlWorkspaceUser sqlWorkspaceUser : sqlWorkspaceUsers) {
            if (!sqlWorkspaceUser.getWorkspaceAccess().equals(WorkspaceAccess.NONE.toString())) {
                String userId = sqlWorkspaceUser.getUser().getUserId();
                Workspace workspace = findById(workspaceId, user);
                boolean isCreator = ((SqlWorkspace) workspace).getWorkspaceCreator().getUserId().equals(userId);
                withAccess.add(new WorkspaceUser(userId, WorkspaceAccess.valueOf(sqlWorkspaceUser.getWorkspaceAccess()), isCreator));
            }
        }

        return withAccess;
    }

    @Override
    public List<WorkspaceEntity> findEntities(Workspace workspace, final boolean fetchVertices, User user) {
        if (!hasReadPermissions(workspace.getWorkspaceId(), user)) {
            throw new VisalloAccessDeniedException("user " + user.getUserId() + " does not have read access to workspace " + workspace.getWorkspaceId(), user, workspace.getWorkspaceId());
        }

        final Authorizations authorizations = userRepository.getAuthorizations(user, VISIBILITY_STRING, workspace.getWorkspaceId());
        Session session = sessionManager.getSession();
        SqlWorkspace sqlWorkspace = (SqlWorkspace) session.get(SqlWorkspace.class, workspace.getWorkspaceId());
        List<WorkspaceEntity> workspaceEntities;
        List<SqlWorkspaceVertex> sqlWorkspaceVertices = sqlWorkspace.getSqlWorkspaceVertices();
        workspaceEntities = toList(new ConvertingIterable<SqlWorkspaceVertex, WorkspaceEntity>(sqlWorkspaceVertices) {
            @Override
            protected WorkspaceEntity convert(SqlWorkspaceVertex sqlWorkspaceVertex) {
                String vertexId = sqlWorkspaceVertex.getVertexId();
                Vertex vertex;
                if (fetchVertices) {
                    vertex = getGraph().getVertex(vertexId, authorizations);
                } else {
                    vertex = null;
                }

                Integer graphPositionX = sqlWorkspaceVertex.getGraphPositionX();
                Integer graphPositionY = sqlWorkspaceVertex.getGraphPositionY();
                boolean visible = sqlWorkspaceVertex.isVisible();

                // TODO implement graphLayoutJson in sql
                return new WorkspaceEntity(vertexId, visible, graphPositionX, graphPositionY, null, vertex);
            }
        });
        return workspaceEntities;
    }

    @Override
    public void softDeleteEntitiesFromWorkspace(Workspace workspace, List<String> entityIdsToDelete, User authUser) {
        Session session = sessionManager.getSession();
        Transaction transaction = null;
        try {
            transaction = session.beginTransaction();
            for (final String vertexId : entityIdsToDelete) {
                LOGGER.debug("workspace delete (%s): %s", workspace.getWorkspaceId(), vertexId);

                List<SqlWorkspaceVertex> sqlWorkspaceVertices = ((SqlWorkspace) workspace).getSqlWorkspaceVertices();
                for (SqlWorkspaceVertex sqlWorkspaceVertex : sqlWorkspaceVertices) {
                    if (entityIdsToDelete.contains(sqlWorkspaceVertex.getVertexId())) {
                        sqlWorkspaceVertex.setVisible(false);
                        session.update(sqlWorkspaceVertex);
                    }
                }
            }
            transaction.commit();
        } catch (HibernateException e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new RuntimeException(e);
        }
    }

    @Override
    public void updateEntitiesOnWorkspace(Workspace workspace, Collection<Update> updates, User user) {
        checkNotNull(workspace, "Workspace cannot be null");
        if (updates.size() == 0) {
            return;
        }

        if (!hasCommentPermissions(workspace.getWorkspaceId(), user)) {
            throw new VisalloAccessDeniedException("user " + user.getUserId() + " does not have write access to workspace " + workspace.getWorkspaceId(), user, workspace.getWorkspaceId());
        }

        Session session = sessionManager.getSession();
        Transaction transaction = null;
        try {
            transaction = session.beginTransaction();

            for (Update update : updates) {
                List vertices = session.createCriteria(SqlWorkspaceVertex.class)
                        .add(Restrictions.eq("vertexId", update.getVertexId()))
                        .add(Restrictions.eq("workspace.workspaceId", workspace.getWorkspaceId()))
                        .list();
                SqlWorkspaceVertex sqlWorkspaceVertex;
                if (vertices.size() > 1) {
                    throw new VisalloException("more than one vertex was returned");
                } else if (vertices.size() == 0) {
                    sqlWorkspaceVertex = new SqlWorkspaceVertex();
                    sqlWorkspaceVertex.setVertexId(update.getVertexId());
                    sqlWorkspaceVertex.setWorkspace((SqlWorkspace) workspace);
                    ((SqlWorkspace) workspace).getSqlWorkspaceVertices().add(sqlWorkspaceVertex);
                    session.update(workspace);
                } else {
                    sqlWorkspaceVertex = (SqlWorkspaceVertex) vertices.get(0);
                }
                sqlWorkspaceVertex.setVisible(update.getVisible());
                if (update.getGraphPosition() != null) {
                    sqlWorkspaceVertex.setGraphPositionX(update.getGraphPosition().getX());
                    sqlWorkspaceVertex.setGraphPositionY(update.getGraphPosition().getY());
                }
                session.saveOrUpdate(sqlWorkspaceVertex);
            }

            transaction.commit();
        } catch (HibernateException e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteUserFromWorkspace(Workspace workspace, String userId, User user) {
        updateUserOnWorkspace(workspace, userId, WorkspaceAccess.NONE, user);
    }

    /**
     * @param workspace       workspace to update
     * @param userId          userId of the user you want to update
     * @param workspaceAccess level of access to set
     * @param user            user requesting the update
     */
    @Override
    public void updateUserOnWorkspace(Workspace workspace, String userId, WorkspaceAccess workspaceAccess, User user) {
        checkNotNull(userId);
        if (!hasWritePermissions(workspace.getWorkspaceId(), user)) {
            throw new VisalloAccessDeniedException("user " + user.getUserId() + " does not have write access to workspace " + workspace.getWorkspaceId(), user, workspace.getWorkspaceId());
        }

        SqlUser userToUpdate = (SqlUser) userRepository.findById(userId);

        Session session = sessionManager.getSession();
        Transaction transaction = null;
        try {
            transaction = session.beginTransaction();
            SqlWorkspace sqlWorkspace = (SqlWorkspace) session.byId(SqlWorkspace.class).load(workspace.getWorkspaceId());
            List<SqlWorkspaceUser> sqlWorkspaceUsers = sqlWorkspace.getSqlWorkspaceUserList();
            boolean updateUser = false;
            for (SqlWorkspaceUser sqlWorkspaceUser : sqlWorkspaceUsers) {
                if (sqlWorkspaceUser.getUser().getUserId().equals(userId)) {
                    updateUser = true;
                    sqlWorkspaceUser.setWorkspaceAccess(workspaceAccess);
                }
            }

            if (!updateUser) {
                SqlWorkspaceUser sqlWorkspaceUser = new SqlWorkspaceUser();
                sqlWorkspaceUser.setWorkspaceAccess(WorkspaceAccess.WRITE);

                sqlWorkspaceUser.setUser(userToUpdate);
                sqlWorkspaceUser.setWorkspace(sqlWorkspace);

                sqlWorkspaceUsers.add(sqlWorkspaceUser);
            }
            session.update(sqlWorkspace);
            transaction.commit();
        } catch (HibernateException e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new RuntimeException(e);
        }
    }

    @Override
    public ClientApiWorkspaceDiff getDiff(Workspace workspace, User user, Locale locale, String timeZone) {
        return new ClientApiWorkspaceDiff();
    }

    @Override
    public boolean hasCommentPermissions(String workspaceId, User user) {
        List<SqlWorkspaceUser> sqlWorkspaceUsers = getSqlWorkspaceUserLists(workspaceId);
        for (SqlWorkspaceUser workspaceUser : sqlWorkspaceUsers) {
            if (workspaceUser.getUser().getUserId().equals(user.getUserId()) && (
                    workspaceUser.getWorkspaceAccess().equals(WorkspaceAccess.COMMENT.toString())
                            || workspaceUser.getWorkspaceAccess().equals(WorkspaceAccess.WRITE.toString()))
                    ) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasWritePermissions(String workspaceId, User user) {
        List<SqlWorkspaceUser> sqlWorkspaceUsers = getSqlWorkspaceUserLists(workspaceId);
        for (SqlWorkspaceUser workspaceUser : sqlWorkspaceUsers) {
            if (workspaceUser.getUser().getUserId().equals(user.getUserId()) && workspaceUser.getWorkspaceAccess().equals(WorkspaceAccess.WRITE.toString())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasReadPermissions(String workspaceId, User user) {
        if (hasWritePermissions(workspaceId, user)) {
            return true;
        }
        List<SqlWorkspaceUser> sqlWorkspaceUsers = getSqlWorkspaceUserLists(workspaceId);
        for (SqlWorkspaceUser workspaceUser : sqlWorkspaceUsers) {
            if (workspaceUser.getWorkspace().getWorkspaceId().equals(workspaceId) &&
                    workspaceUser.getWorkspaceAccess().equals(WorkspaceAccess.READ.toString())) {
                return true;
            }
        }
        return false;
    }

    protected List<SqlWorkspaceUser> getSqlWorkspaceUserLists(String workspaceId) {
        Session session = sessionManager.getSession();
        List<SqlWorkspaceUser> sqlWorkspaceUsers;
        sqlWorkspaceUsers = session.createCriteria(SqlWorkspaceUser.class).add(Restrictions.eq("sqlWorkspaceUser.workspace.workspaceId", workspaceId)).list();
        return sqlWorkspaceUsers;
    }

    protected List<SqlWorkspaceVertex> getSqlWorkspaceVertices(SqlWorkspace sqlWorkspace) {
        Session session = sessionManager.getSession();
        List<SqlWorkspaceVertex> sqlWorkspaceVertices;
        sqlWorkspaceVertices = session.createCriteria(SqlWorkspaceVertex.class).add(Restrictions.eq("workspace.workspaceId", sqlWorkspace.getWorkspaceId())).list();
        return sqlWorkspaceVertices;
    }
}
