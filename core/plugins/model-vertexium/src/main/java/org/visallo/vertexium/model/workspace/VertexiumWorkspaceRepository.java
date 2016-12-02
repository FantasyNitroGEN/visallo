package org.visallo.vertexium.model.workspace;

import com.google.common.base.Function;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.codec.digest.DigestUtils;
import org.json.JSONObject;
import org.vertexium.*;
import org.vertexium.mutation.ExistingEdgeMutation;
import org.vertexium.property.StreamingPropertyValue;
import org.vertexium.query.Compare;
import org.vertexium.query.QueryResultsIterable;
import org.vertexium.search.IndexHint;
import org.vertexium.util.FilterIterable;
import org.visallo.core.bootstrap.InjectHelper;
import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloAccessDeniedException;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.exception.VisalloResourceNotFoundException;
import org.visallo.core.formula.FormulaEvaluator;
import org.visallo.core.model.graph.GraphRepository;
import org.visallo.core.model.graph.GraphUpdateContext;
import org.visallo.core.model.lock.LockRepository;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.termMention.TermMentionRepository;
import org.visallo.core.model.user.AuthorizationRepository;
import org.visallo.core.model.user.GraphAuthorizationRepository;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.model.workspace.*;
import org.visallo.core.model.workspace.product.Product;
import org.visallo.core.model.workspace.product.WorkProduct;
import org.visallo.core.model.workspace.product.WorkProductElements;
import org.visallo.core.security.VisalloVisibility;
import org.visallo.core.security.VisibilityTranslator;
import org.visallo.core.trace.Traced;
import org.visallo.core.user.SystemUser;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.vertexium.model.user.VertexiumUserRepository;
import org.visallo.web.clientapi.model.ClientApiWorkspace;
import org.visallo.web.clientapi.model.ClientApiWorkspaceDiff;
import org.visallo.web.clientapi.model.WorkspaceAccess;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.visallo.core.util.StreamUtil.stream;

@Singleton
public class VertexiumWorkspaceRepository extends WorkspaceRepository {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(VertexiumWorkspaceRepository.class);
    private final UserRepository userRepository;
    private final GraphRepository graphRepository;
    private final GraphAuthorizationRepository graphAuthorizationRepository;
    private final WorkspaceDiffHelper workspaceDiff;
    private final Configuration configuration;
    private Collection<WorkProduct> workProducts;
    private final LockRepository lockRepository;

    private Cache<String, Boolean> usersWithReadAccessCache = CacheBuilder.newBuilder()
            .expireAfterWrite(15, TimeUnit.SECONDS)
            .build();
    private Cache<String, Boolean> usersWithCommentAccessCache = CacheBuilder.newBuilder()
            .expireAfterWrite(15, TimeUnit.SECONDS)
            .build();
    private Cache<String, Boolean> usersWithWriteAccessCache = CacheBuilder.newBuilder()
            .expireAfterWrite(15, TimeUnit.SECONDS)
            .build();
    private Cache<String, List<WorkspaceUser>> usersWithAccessCache = CacheBuilder.newBuilder()
            .expireAfterWrite(15, TimeUnit.SECONDS)
            .build();
    private Cache<String, Vertex> userWorkspaceVertexCache = CacheBuilder.newBuilder()
            .expireAfterWrite(15, TimeUnit.SECONDS)
            .build();
    private Cache<String, List<WorkspaceEntity>> workspaceEntitiesCached = CacheBuilder.newBuilder()
            .expireAfterWrite(15, TimeUnit.SECONDS)
            .build();

    public void clearCache() {
        usersWithReadAccessCache.invalidateAll();
        usersWithCommentAccessCache.invalidateAll();
        usersWithWriteAccessCache.invalidateAll();
        usersWithAccessCache.invalidateAll();
        userWorkspaceVertexCache.invalidateAll();
        workspaceEntitiesCached.invalidateAll();
    }

    @Inject
    public VertexiumWorkspaceRepository(
            Graph graph,
            Configuration configuration,
            GraphRepository graphRepository,
            UserRepository userRepository,
            GraphAuthorizationRepository graphAuthorizationRepository,
            WorkspaceDiffHelper workspaceDiff,
            LockRepository lockRepository,
            VisibilityTranslator visibilityTranslator,
            TermMentionRepository termMentionRepository,
            OntologyRepository ontologyRepository,
            WorkQueueRepository workQueueRepository,
            AuthorizationRepository authorizationRepository
    ) {
        super(
                graph,
                configuration,
                visibilityTranslator,
                termMentionRepository,
                ontologyRepository,
                workQueueRepository,
                authorizationRepository
        );
        this.graphRepository = graphRepository;
        this.userRepository = userRepository;
        this.graphAuthorizationRepository = graphAuthorizationRepository;
        this.workspaceDiff = workspaceDiff;
        this.lockRepository = lockRepository;
        this.configuration = configuration;

        graphAuthorizationRepository.addAuthorizationToGraph(VISIBILITY_STRING);
        graphAuthorizationRepository.addAuthorizationToGraph(VisalloVisibility.SUPER_USER_VISIBILITY_STRING);
    }

    @Override
    public void delete(final Workspace workspace, final User user) {
        if (!hasWritePermissions(workspace.getWorkspaceId(), user)) {
            throw new VisalloAccessDeniedException(
                    "user " + user.getUserId() + " does not have write access to workspace " + workspace.getWorkspaceId(),
                    user,
                    workspace.getWorkspaceId()
            );
        }

        fireWorkspaceBeforeDelete(workspace, user);

        lockRepository.lock(getLockName(workspace), () -> {
            Authorizations authorizations = getAuthorizationRepository().getGraphAuthorizations(
                    user,
                    UserRepository.VISIBILITY_STRING,
                    VisalloVisibility.SUPER_USER_VISIBILITY_STRING,
                    workspace.getWorkspaceId()
            );
            Vertex workspaceVertex = getVertexFromWorkspace(workspace, true, authorizations);

            List<Vertex> productVertices = Lists.newArrayList(workspaceVertex.getVertices(
                    Direction.BOTH,
                    WorkspaceProperties.WORKSPACE_TO_PRODUCT_RELATIONSHIP_IRI,
                    authorizations
            ));
            for (Vertex productVertex : productVertices) {
                getGraph().softDeleteVertex(productVertex, authorizations);
            }

            getGraph().softDeleteVertex(workspaceVertex, authorizations);
            getGraph().flush();

            graphAuthorizationRepository.removeAuthorizationFromGraph(workspace.getWorkspaceId());
        });
    }

    private String getLockName(Workspace workspace) {
        return getLockName(workspace.getWorkspaceId());
    }

    private String getLockName(String workspaceId) {
        return "WORKSPACE_" + workspaceId;
    }

    public Vertex getVertex(String workspaceId, User user) {
        String cacheKey = getUserWorkspaceVertexCacheKey(workspaceId, user);
        Vertex workspaceVertex = userWorkspaceVertexCache.getIfPresent(cacheKey);
        if (workspaceVertex != null) {
            return workspaceVertex;
        }

        Authorizations authorizations = getAuthorizationRepository().getGraphAuthorizations(
                user,
                UserRepository.VISIBILITY_STRING,
                VisalloVisibility.SUPER_USER_VISIBILITY_STRING,
                workspaceId
        );
        workspaceVertex = getGraph().getVertex(workspaceId, authorizations);
        if (workspaceVertex != null) {
            userWorkspaceVertexCache.put(cacheKey, workspaceVertex);
        }
        return workspaceVertex;
    }

    public String getUserWorkspaceVertexCacheKey(String workspaceId, User user) {
        return workspaceId + user.getUserId();
    }

    private Vertex getVertexFromWorkspace(Workspace workspace, boolean includeHidden, Authorizations authorizations) {
        if (workspace instanceof VertexiumWorkspace) {
            return ((VertexiumWorkspace) workspace).getVertex(getGraph(), includeHidden, authorizations);
        }
        return getGraph().getVertex(
                workspace.getWorkspaceId(),
                includeHidden ? FetchHint.ALL_INCLUDING_HIDDEN : FetchHint.ALL,
                authorizations
        );
    }

    @Override
    @Traced
    public Workspace findById(String workspaceId, boolean includeHidden, User user) {
        LOGGER.debug("findById(workspaceId: %s, userId: %s)", workspaceId, user.getUserId());
        Authorizations authorizations = getAuthorizationRepository().getGraphAuthorizations(
                user,
                VISIBILITY_STRING,
                workspaceId
        );

        Vertex workspaceVertex;
        try {
            workspaceVertex = getGraph().getVertex(
                    workspaceId,
                    includeHidden ? FetchHint.ALL_INCLUDING_HIDDEN : FetchHint.ALL,
                    authorizations
            );
        } catch (SecurityVertexiumException e) {
            if (!graphAuthorizationRepository.getGraphAuthorizations().contains(workspaceId)) {
                return null;
            }

            String message = String.format("user %s does not have read access to workspace %s", user.getUserId(), workspaceId);
            LOGGER.warn("%s", message, e);
            throw new VisalloAccessDeniedException(
                    message,
                    user,
                    workspaceId
            );
        }

        if (workspaceVertex == null) {
            return null;
        }

        if (!hasReadPermissions(workspaceId, user)) {
            throw new VisalloAccessDeniedException(
                    "user " + user.getUserId() + " does not have read access to workspace " + workspaceId,
                    user,
                    workspaceId
            );
        }
        return new VertexiumWorkspace(workspaceVertex);
    }

    @Override
    public Workspace add(String workspaceId, String title, User user) {
        if (workspaceId == null) {
            workspaceId = WORKSPACE_ID_PREFIX + getGraph().getIdGenerator().nextId();
        }

        graphAuthorizationRepository.addAuthorizationToGraph(workspaceId);

        Authorizations authorizations = getAuthorizationRepository().getGraphAuthorizations(
                user,
                UserRepository.VISIBILITY_STRING,
                VISIBILITY_STRING,
                workspaceId
        );
        Vertex userVertex = null;
        if (!user.getUserId().equals(userRepository.getSystemUser().getUserId())) {
            userVertex = getGraph().getVertex(user.getUserId(), authorizations);
            checkNotNull(userVertex, "Could not find user: " + user.getUserId());
        }

        VertexBuilder workspaceVertexBuilder = getGraph().prepareVertex(workspaceId, VISIBILITY.getVisibility());
        VisalloProperties.CONCEPT_TYPE.setProperty(
                workspaceVertexBuilder,
                WORKSPACE_CONCEPT_IRI,
                getVisibilityTranslator().getDefaultVisibility()
        );
        WorkspaceProperties.TITLE.setProperty(workspaceVertexBuilder, title, VISIBILITY.getVisibility());
        Vertex workspaceVertex = workspaceVertexBuilder.save(authorizations);

        if (userVertex != null) {
            addWorkspaceToUser(workspaceVertex, userVertex, authorizations);
        }

        getGraph().flush();
        VertexiumWorkspace workspace = new VertexiumWorkspace(workspaceVertex);
        fireWorkspaceAdded(workspace, user);
        return workspace;
    }

    public void addWorkspaceToUser(Vertex workspaceVertex, Vertex userVertex, Authorizations authorizations) {
        EdgeBuilder edgeBuilder = getGraph().prepareEdge(
                workspaceVertex,
                userVertex,
                WORKSPACE_TO_USER_RELATIONSHIP_IRI,
                VISIBILITY.getVisibility()
        );
        WorkspaceProperties.WORKSPACE_TO_USER_IS_CREATOR.setProperty(edgeBuilder, true, VISIBILITY.getVisibility());
        WorkspaceProperties.WORKSPACE_TO_USER_ACCESS.setProperty(
                edgeBuilder,
                WorkspaceAccess.WRITE.toString(),
                VISIBILITY.getVisibility()
        );
        edgeBuilder.save(authorizations);
    }

    @Override
    public Iterable<Workspace> findAllForUser(final User user) {
        checkNotNull(user, "User is required");
        Authorizations authorizations = getAuthorizationRepository().getGraphAuthorizations(
                user,
                VISIBILITY_STRING,
                UserRepository.VISIBILITY_STRING
        );
        Vertex userVertex = getGraph().getVertex(user.getUserId(), authorizations);
        checkNotNull(userVertex, "Could not find user vertex with id " + user.getUserId());
        return stream(userVertex.getVertices(Direction.IN, WORKSPACE_TO_USER_RELATIONSHIP_IRI, authorizations))
                .map((Vertex workspaceVertex) -> {
                    String cacheKey = getUserWorkspaceVertexCacheKey(workspaceVertex.getId(), user);
                    userWorkspaceVertexCache.put(cacheKey, workspaceVertex);
                    return new VertexiumWorkspace(workspaceVertex);
                })
                .collect(Collectors.toList());
    }

    @Override
    public Iterable<Workspace> findAll(User user) {
        if (!user.equals(userRepository.getSystemUser())) {
            throw new VisalloAccessDeniedException("Only system user can access all workspaces", user, null);
        }
        Authorizations authorizations = getAuthorizationRepository().getGraphAuthorizations(
                user,
                VISIBILITY_STRING,
                UserRepository.VISIBILITY_STRING
        );
        QueryResultsIterable<Vertex> workspaceVertices = getGraph().query(authorizations)
                .has(VisalloProperties.CONCEPT_TYPE.getPropertyName(), Compare.EQUAL, WORKSPACE_CONCEPT_IRI)
                .vertices();
        return stream(workspaceVertices)
                .map((Vertex workspaceVertex) -> {
                    String cacheKey = getUserWorkspaceVertexCacheKey(workspaceVertex.getId(), user);
                    userWorkspaceVertexCache.put(cacheKey, workspaceVertex);
                    return new VertexiumWorkspace(workspaceVertex);
                })
                .collect(Collectors.toList());
    }

    @Override
    public void setTitle(Workspace workspace, String title, User user) {
        if (!hasWritePermissions(workspace.getWorkspaceId(), user)) {
            throw new VisalloAccessDeniedException(
                    "user " + user.getUserId() + " does not have write access to workspace " + workspace.getWorkspaceId(),
                    user,
                    workspace.getWorkspaceId()
            );
        }
        Authorizations authorizations = getAuthorizationRepository().getGraphAuthorizations(user);
        Vertex workspaceVertex = getVertexFromWorkspace(workspace, false, authorizations);
        WorkspaceProperties.TITLE.setProperty(workspaceVertex, title, VISIBILITY.getVisibility(), authorizations);
        getGraph().flush();
    }

    @Override
    @Traced
    public List<WorkspaceUser> findUsersWithAccess(final String workspaceId, final User user) {
        String cacheKey = workspaceId + user.getUserId();
        List<WorkspaceUser> usersWithAccess = this.usersWithAccessCache.getIfPresent(cacheKey);
        if (usersWithAccess != null) {
            return usersWithAccess;
        }

        LOGGER.debug("BEGIN findUsersWithAccess query");
        Authorizations authorizations = getAuthorizationRepository().getGraphAuthorizations(
                user,
                VISIBILITY_STRING,
                workspaceId
        );
        Vertex workspaceVertex = getVertex(workspaceId, user);
        if (workspaceVertex == null) {
            return Lists.newArrayList();
        } else {
            Iterable<Edge> userEdges = workspaceVertex.getEdges(
                    Direction.OUT,
                    WORKSPACE_TO_USER_RELATIONSHIP_IRI,
                    authorizations
            );
            usersWithAccess = stream(userEdges)
                    .map((edge) -> {
                        String userId = edge.getOtherVertexId(workspaceId);

                        String accessString = WorkspaceProperties.WORKSPACE_TO_USER_ACCESS.getPropertyValue(edge);
                        WorkspaceAccess workspaceAccess = WorkspaceAccess.NONE;
                        if (accessString != null && accessString.length() > 0) {
                            workspaceAccess = WorkspaceAccess.valueOf(accessString);
                        }

                        boolean isCreator = WorkspaceProperties.WORKSPACE_TO_USER_IS_CREATOR.getPropertyValue(edge, false);

                        return new WorkspaceUser(userId, workspaceAccess, isCreator);
                    })
                    .collect(Collectors.toList());
            this.usersWithAccessCache.put(cacheKey, usersWithAccess);
            LOGGER.debug("END findUsersWithAccess query");
            return usersWithAccess;
        }
    }

    @Override
    public List<WorkspaceEntity> findEntities(final Workspace workspace, final boolean fetchVertices, final User user) {
        if (!hasReadPermissions(workspace.getWorkspaceId(), user)) {
            throw new VisalloAccessDeniedException(
                    "user " + user.getUserId() + " does not have read access to workspace " + workspace.getWorkspaceId(),
                    user,
                    workspace.getWorkspaceId()
            );
        }

        return lockRepository.lock(
                getLockName(workspace),
                () -> findEntitiesNoLock(workspace, false, fetchVertices, user)
        );
    }

    @Traced
    private List<WorkspaceEntity> findEntitiesNoLock(
            final Workspace workspace,
            final boolean includeHidden,
            final boolean fetchVertices,
            User user
    ) {
        LOGGER.debug(
                "BEGIN findEntitiesNoLock(workspaceId: %s, includeHidden: %b, userId: %s)",
                workspace.getWorkspaceId(),
                includeHidden,
                user.getUserId()
        );
        long startTime = System.currentTimeMillis();
        String cacheKey = workspace.getWorkspaceId() + includeHidden + user.getUserId();
        List<WorkspaceEntity> results = workspaceEntitiesCached.getIfPresent(cacheKey);
        if (results != null) {
            LOGGER.debug("END findEntitiesNoLock (cache hit, found: %d entities)", results.size());
            return results;
        }

        Authorizations authorizations = getAuthorizationRepository().getGraphAuthorizations(
                user,
                VISIBILITY_STRING,
                workspace.getWorkspaceId()
        );
        Vertex workspaceVertex = getVertexFromWorkspace(workspace, includeHidden, authorizations);
        List<Edge> entityEdges = stream(workspaceVertex.getEdges(
                Direction.BOTH,
                WORKSPACE_TO_ENTITY_RELATIONSHIP_IRI,
                authorizations
        ))
                .collect(Collectors.toList());

        final Map<String, Vertex> workspaceVertices;
        if (fetchVertices) {
            workspaceVertices = getWorkspaceVertices(workspace, entityEdges, authorizations);
        } else {
            workspaceVertices = null;
        }

        results = entityEdges.stream()
                .map(edge -> {
                    String entityVertexId = edge.getOtherVertexId(workspace.getWorkspaceId());

                    if (!includeHidden) {
                        return null;
                    }

                    Vertex workspaceVertex1 = null;
                    if (fetchVertices) {
                        workspaceVertex1 = workspaceVertices.get(entityVertexId);
                    }
                    return new WorkspaceEntity(
                            entityVertexId,
                            workspaceVertex1
                    );
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        workspaceEntitiesCached.put(cacheKey, results);
        LOGGER.debug(
                "END findEntitiesNoLock (found: %d entities, time: %dms)",
                results.size(),
                System.currentTimeMillis() - startTime
        );
        return results;
    }

    protected Map<String, Vertex> getWorkspaceVertices(
            final Workspace workspace,
            List<Edge> entityEdges,
            Authorizations authorizations
    ) {
        Map<String, Vertex> workspaceVertices;
        Iterable<String> workspaceVertexIds = entityEdges.stream()
                .map(edge -> edge.getOtherVertexId(workspace.getWorkspaceId()))
                .collect(Collectors.toList());
        Iterable<Vertex> vertices = getGraph().getVertices(
                workspaceVertexIds,
                FetchHint.ALL_INCLUDING_HIDDEN,
                authorizations
        );
        workspaceVertices = Maps.uniqueIndex(vertices, new Function<Vertex, String>() {
            @Nullable
            @Override
            public String apply(Vertex v) {
                return v.getId();
            }
        });
        return workspaceVertices;
    }

    @Override
    public Workspace copyTo(Workspace workspace, User destinationUser, User user) {
        Workspace newWorkspace = super.copyTo(workspace, destinationUser, user);
        getGraph().flush();
        return newWorkspace;
    }

    @Override
    public void softDeleteEntitiesFromWorkspace(Workspace workspace, List<String> entityIdsToDelete, User user) {
        if (entityIdsToDelete.size() == 0) {
            return;
        }
        if (!hasWritePermissions(workspace.getWorkspaceId(), user)) {
            throw new VisalloAccessDeniedException(
                    "user " + user.getUserId() + " does not have write access to workspace " + workspace.getWorkspaceId(),
                    user,
                    workspace.getWorkspaceId()
            );
        }
        Authorizations authorizations = getAuthorizationRepository().getGraphAuthorizations(
                user,
                VISIBILITY_STRING,
                workspace.getWorkspaceId()
        );
        final Vertex workspaceVertex = getVertexFromWorkspace(workspace, true, authorizations);
        List<Edge> allEdges = stream(workspaceVertex.getEdges(
                Direction.BOTH,
                authorizations
        )).collect(Collectors.toList());

        for (final String vertexId : entityIdsToDelete) {
            LOGGER.debug("workspace delete (%s): %s", workspace.getWorkspaceId(), vertexId);

            Iterable<Edge> edges = new FilterIterable<Edge>(allEdges) {
                @Override
                protected boolean isIncluded(Edge o) {
                    String entityVertexId = o.getOtherVertexId(workspaceVertex.getId());
                    return entityVertexId.equalsIgnoreCase(vertexId);
                }
            };
            for (Edge edge : edges) {
                ExistingEdgeMutation m = edge.prepareMutation();
                m.setIndexHint(IndexHint.DO_NOT_INDEX);
                m.save(authorizations);
            }
        }
        getGraph().flush();
    }

    @Override
    public void updateEntitiesOnWorkspace(
            final Workspace workspace,
            final Collection<String> vertexIds,
            final User user
    ) {
        if (vertexIds.size() == 0) {
            return;
        }
        if (!hasCommentPermissions(workspace.getWorkspaceId(), user)) {
            throw new VisalloAccessDeniedException(
                    "user " + user.getUserId() + " does not have comment access to workspace " + workspace.getWorkspaceId(),
                    user,
                    workspace.getWorkspaceId()
            );
        }

        lockRepository.lock(getLockName(workspace.getWorkspaceId()), () -> {
            Authorizations authorizations = getAuthorizationRepository().getGraphAuthorizations(
                    user,
                    VISIBILITY_STRING,
                    workspace.getWorkspaceId()
            );

            Vertex workspaceVertex = getVertexFromWorkspace(workspace, true, authorizations);
            if (workspaceVertex == null) {
                throw new VisalloResourceNotFoundException(
                        "Could not find workspace vertex: " + workspace.getWorkspaceId(),
                        workspace.getWorkspaceId()
                );
            }

            Iterable<Vertex> vertices = getGraph().getVertices(vertexIds, authorizations);
            ImmutableMap<String, Vertex> verticesMap = Maps.uniqueIndex(vertices, Element::getId);

            for (String vertexId : vertexIds) {
                Vertex otherVertex = verticesMap.get(vertexId);
                if (otherVertex == null) {
                    LOGGER.error(
                            "updateEntitiesOnWorkspace: could not find vertex with id \"%s\" for workspace \"%s\"",
                            vertexId,
                            workspace.getWorkspaceId()
                    );
                    continue;
                }

                createEdge(
                        workspaceVertex,
                        otherVertex,
                        authorizations
                );
            }
            getGraph().flush();
            workspaceEntitiesCached.invalidateAll();
        });

        fireWorkspaceUpdateEntities(workspace, vertexIds, user);
    }

    @Override
    public Dashboard findDashboardById(String workspaceId, String dashboardId, User user) {
        LOGGER.debug("findDashboardById(dashboardId: %s, userId: %s)", dashboardId, user.getUserId());
        Authorizations authorizations = getAuthorizationRepository().getGraphAuthorizations(
                user,
                VISIBILITY_STRING,
                workspaceId
        );
        Vertex dashboardVertex = getGraph().getVertex(dashboardId, authorizations);
        if (dashboardVertex == null) {
            return null;
        }
        if (!hasReadPermissions(workspaceId, user)) {
            throw new VisalloAccessDeniedException(
                    "user " + user.getUserId() + " does not have read access to workspace " + workspaceId,
                    user,
                    workspaceId
            );
        }
        return dashboardVertexToDashboard(workspaceId, dashboardVertex, authorizations);
    }

    @Override
    public void deleteDashboard(String workspaceId, String dashboardId, User user) {
        LOGGER.debug("deleteDashboard(dashboardId: %s, userId: %s)", dashboardId, user.getUserId());
        if (!hasWritePermissions(workspaceId, user)) {
            throw new VisalloAccessDeniedException(
                    "user " + user.getUserId() + " does not have write access to workspace " + workspaceId,
                    user,
                    workspaceId
            );
        }
        Authorizations authorizations = getAuthorizationRepository().getGraphAuthorizations(
                user,
                VISIBILITY_STRING,
                workspaceId
        );

        Vertex dashboardVertex = getGraph().getVertex(dashboardId, authorizations);
        Iterable<Vertex> dashboardItemVertices = dashboardVertex.getVertices(
                Direction.OUT,
                WorkspaceProperties.DASHBOARD_TO_DASHBOARD_ITEM_RELATIONSHIP_IRI,
                authorizations
        );
        for (Vertex dashboardItemVertex : dashboardItemVertices) {
            getGraph().softDeleteVertex(dashboardItemVertex, authorizations);
        }
        getGraph().softDeleteVertex(dashboardVertex, authorizations);
        getGraph().flush();
    }

    @Override
    public Collection<Dashboard> findAllDashboardsForWorkspace(final String workspaceId, User user) {
        LOGGER.debug("findAllDashboardsForWorkspace(workspaceId: %s, userId: %s)", workspaceId, user.getUserId());
        final Authorizations authorizations = getAuthorizationRepository().getGraphAuthorizations(
                user,
                VISIBILITY_STRING,
                workspaceId
        );
        final Vertex workspaceVertex = getVertex(workspaceId, user);
        if (workspaceVertex == null) {
            return null;
        }
        if (!hasReadPermissions(workspaceId, user)) {
            throw new VisalloAccessDeniedException(
                    "user " + user.getUserId() + " does not have read access to workspace " + workspaceId,
                    user,
                    workspaceId
            );
        }
        Iterable<Vertex> dashboardVertices = workspaceVertex.getVertices(
                Direction.OUT,
                WorkspaceProperties.WORKSPACE_TO_DASHBOARD_RELATIONSHIP_IRI,
                authorizations
        );
        return stream(dashboardVertices)
                .map(dashboardVertex -> dashboardVertexToDashboard(workspaceId, dashboardVertex, authorizations))
                .collect(Collectors.toList());
    }

    @Override
    public DashboardItem findDashboardItemById(String workspaceId, String dashboardItemId, User user) {
        LOGGER.debug("findDashboardItemById(dashboardItemId: %s, userId: %s)", dashboardItemId, user.getUserId());
        Authorizations authorizations = getAuthorizationRepository().getGraphAuthorizations(
                user,
                VISIBILITY_STRING,
                workspaceId
        );
        Vertex dashboardItemVertex = getGraph().getVertex(dashboardItemId, authorizations);
        if (dashboardItemVertex == null) {
            return null;
        }
        if (!hasReadPermissions(workspaceId, user)) {
            throw new VisalloAccessDeniedException(
                    "user " + user.getUserId() + " does not have read access to workspace " + workspaceId,
                    user,
                    workspaceId
            );
        }
        return dashboardItemVertexToDashboardItem(dashboardItemVertex);
    }

    @Override
    public void deleteDashboardItem(String workspaceId, String dashboardItemId, User user) {
        LOGGER.debug("deleteDashboardItem(dashboardItemId: %s, userId: %s)", dashboardItemId, user.getUserId());
        if (!hasWritePermissions(workspaceId, user)) {
            throw new VisalloAccessDeniedException(
                    "user " + user.getUserId() + " does not have write access to workspace " + workspaceId,
                    user,
                    workspaceId
            );
        }
        Authorizations authorizations = getAuthorizationRepository().getGraphAuthorizations(
                user,
                VISIBILITY_STRING,
                workspaceId
        );
        getGraph().softDeleteVertex(dashboardItemId, authorizations);
        getGraph().flush();
    }

    private DashboardItem dashboardItemVertexToDashboardItem(Vertex dashboardItemVertex) {
        String dashboardItemId = dashboardItemVertex.getId();
        String extensionId = WorkspaceProperties.DASHBOARD_ITEM_EXTENSION_ID.getPropertyValue(
                dashboardItemVertex,
                null
        );
        String dashboardItemTitle = WorkspaceProperties.TITLE.getPropertyValue(dashboardItemVertex, null);
        String configuration = WorkspaceProperties.DASHBOARD_ITEM_CONFIGURATION.getPropertyValue(
                dashboardItemVertex,
                null
        );
        return new VertexiumDashboardItem(dashboardItemId, extensionId, dashboardItemTitle, configuration);
    }

    @Override
    public String addOrUpdateDashboardItem(
            String workspaceId,
            String dashboardId,
            String dashboardItemId,
            String title,
            String configuration,
            String extensionId,
            User user
    ) {
        LOGGER.debug(
                "addOrUpdateDashboardItem(workspaceId: %s, dashboardId: %s, dashboardItemId: %s, userId: %s)",
                workspaceId,
                dashboardId,
                dashboardItemId,
                user.getUserId()
        );
        if (!hasWritePermissions(workspaceId, user)) {
            throw new VisalloAccessDeniedException(
                    "user " + user.getUserId() + " does not have write access to workspace " + workspaceId,
                    user,
                    workspaceId
            );
        }
        Authorizations authorizations = getAuthorizationRepository().getGraphAuthorizations(
                user,
                VISIBILITY_STRING,
                workspaceId
        );
        Visibility visibility = VISIBILITY.getVisibility();
        VertexBuilder dashboardItemVertexBuilder = getGraph().prepareVertex(dashboardItemId, visibility);
        VisalloProperties.CONCEPT_TYPE.setProperty(
                dashboardItemVertexBuilder,
                WorkspaceProperties.DASHBOARD_ITEM_CONCEPT_IRI,
                getVisibilityTranslator().getDefaultVisibility()
        );
        WorkspaceProperties.DASHBOARD_ITEM_EXTENSION_ID.setProperty(
                dashboardItemVertexBuilder,
                extensionId == null ? "" : extensionId,
                visibility
        );
        WorkspaceProperties.TITLE.setProperty(dashboardItemVertexBuilder, title == null ? "" : title, visibility);
        WorkspaceProperties.DASHBOARD_ITEM_CONFIGURATION.setProperty(
                dashboardItemVertexBuilder,
                configuration == null ? "" : configuration,
                visibility
        );
        Vertex dashboardItemVertex = dashboardItemVertexBuilder.save(authorizations);

        if (dashboardId != null) {
            Vertex dashboardVertex = getGraph().getVertex(dashboardId, authorizations);
            checkNotNull(dashboardVertex, "Could not find dashboard vertex with id: " + dashboardId);

            String edgeId = dashboardVertex.getId() + "_hasDashboardItem_" + dashboardItemVertex.getId();
            getGraph().addEdge(
                    edgeId,
                    dashboardVertex,
                    dashboardItemVertex,
                    WorkspaceProperties.DASHBOARD_TO_DASHBOARD_ITEM_RELATIONSHIP_IRI,
                    visibility,
                    authorizations
            );
        }

        getGraph().flush();

        return dashboardItemVertex.getId();
    }

    private Dashboard dashboardVertexToDashboard(
            String workspaceId,
            Vertex dashboardVertex,
            Authorizations authorizations
    ) {
        String title = WorkspaceProperties.TITLE.getPropertyValue(dashboardVertex);
        Iterable<Vertex> dashboardItemVertices = dashboardVertex.getVertices(
                Direction.OUT,
                WorkspaceProperties.DASHBOARD_TO_DASHBOARD_ITEM_RELATIONSHIP_IRI,
                authorizations
        );
        List<DashboardItem> items = stream(dashboardItemVertices)
                .map(this::dashboardItemVertexToDashboardItem)
                .collect(Collectors.toList());
        return new VertexiumDashboard(dashboardVertex.getId(), workspaceId, title, items);
    }

    private Product productVertexToProduct(String workspaceId, Vertex productVertex, Authorizations authorizations, JSONObject extendedData, User user) {
        String title = WorkspaceProperties.TITLE.getPropertyValue(productVertex);
        String kind = WorkspaceProperties.PRODUCT_KIND.getPropertyValue(productVertex);
        String data = WorkspaceProperties.PRODUCT_DATA.getPropertyValue(productVertex);
        String extendedDataStr = extendedData == null ? null : extendedData.toString();

        Property previewDataUrlProperty = WorkspaceProperties.PRODUCT_PREVIEW_DATA_URL.getProperty(productVertex, user.getUserId());
        String md5 = null;
        if (previewDataUrlProperty != null) {
            Metadata.Entry entry = previewDataUrlProperty.getMetadata().getEntry("http://visallo.org/product#previewImageMD5");
            if (entry != null) {
                md5 = (String) entry.getValue();
            }
        }

        // Don't use current workspace, use the product workspace.
        List<EdgeInfo> edgeInfos = Lists.newArrayList(productVertex.getEdgeInfos(Direction.BOTH, WorkspaceProperties.WORKSPACE_TO_PRODUCT_RELATIONSHIP_IRI, authorizations));
        if (edgeInfos.size() > 0) {
            workspaceId = edgeInfos.get(0).getVertexId();
        }

        return new VertexiumProduct(productVertex.getId(), workspaceId, title, kind, data, extendedDataStr, md5);
    }

    @Override
    public String addOrUpdateDashboard(String workspaceId, String dashboardId, String title, User user) {
        LOGGER.debug(
                "addOrUpdateDashboard(workspaceId: %s, dashboardId: %s, userId: %s)",
                workspaceId,
                dashboardId,
                user.getUserId()
        );
        if (!hasWritePermissions(workspaceId, user)) {
            throw new VisalloAccessDeniedException(
                    "user " + user.getUserId() + " does not have write access to workspace " + workspaceId,
                    user,
                    workspaceId
            );
        }
        Vertex workspaceVertex = getVertex(workspaceId, user);
        Authorizations authorizations = getAuthorizationRepository().getGraphAuthorizations(
                user,
                VISIBILITY_STRING,
                workspaceId
        );
        Visibility visibility = VISIBILITY.getVisibility();
        VertexBuilder dashboardVertexBuilder = getGraph().prepareVertex(dashboardId, visibility);
        VisalloProperties.CONCEPT_TYPE.setProperty(
                dashboardVertexBuilder,
                WorkspaceProperties.DASHBOARD_CONCEPT_IRI,
                getVisibilityTranslator().getDefaultVisibility()
        );
        WorkspaceProperties.TITLE.setProperty(dashboardVertexBuilder, title == null ? "" : title, visibility);
        Vertex dashboardVertex = dashboardVertexBuilder.save(authorizations);

        String edgeId = workspaceVertex.getId() + "_hasDashboard_" + dashboardVertex.getId();
        getGraph().addEdge(
                edgeId,
                workspaceVertex,
                dashboardVertex,
                WorkspaceProperties.WORKSPACE_TO_DASHBOARD_RELATIONSHIP_IRI,
                visibility,
                authorizations
        );

        getGraph().flush();

        return dashboardVertex.getId();
    }

    @Override
    public Collection<Product> findAllProductsForWorkspace(String workspaceId, User user) {
        LOGGER.debug("findAllProductsForWorkspace(workspaceId: %s, userId: %s)", workspaceId, user.getUserId());
        final Authorizations authorizations = getAuthorizationRepository().getGraphAuthorizations(
                user,
                VISIBILITY_STRING,
                workspaceId
        );
        final Vertex workspaceVertex = getVertex(workspaceId, user);
        if (workspaceVertex == null) {
            return null;
        }
        if (!hasReadPermissions(workspaceId, user)) {
            throw new VisalloAccessDeniedException(
                    "user " + user.getUserId() + " does not have read access to workspace " + workspaceId,
                    user,
                    workspaceId
            );
        }
        Iterable<Vertex> productVertices = workspaceVertex.getVertices(
                Direction.OUT,
                WorkspaceProperties.WORKSPACE_TO_PRODUCT_RELATIONSHIP_IRI,
                authorizations
        );
        return stream(productVertices)
                .map(productVertex -> productVertexToProduct(workspaceId, productVertex, authorizations, null, user))
                .collect(Collectors.toList());

    }

    @Override
    public Product updateProductPreview(String workspaceId, String productId, String previewDataUrl, User user) {
        LOGGER.debug(
                "updateProductPreview(workspaceId: %s, productId: %s, userId: %s)",
                workspaceId,
                productId,
                user.getUserId()
        );
        if (!hasReadPermissions(workspaceId, user)) {
            throw new VisalloAccessDeniedException(
                    "user " + user.getUserId() + " does not have read access to workspace " + workspaceId,
                    user,
                    workspaceId
            );
        }

        Authorizations authorizations = getAuthorizationRepository().getGraphAuthorizations(
                user,
                VISIBILITY_STRING,
                workspaceId
        );
        Visibility visibility = VISIBILITY.getVisibility();
        Vertex productVertex;
        ProductPreview preview = getProductPreviewFromUrl(previewDataUrl);

        try (GraphUpdateContext ctx = graphRepository.beginGraphUpdate(Priority.NORMAL, user, authorizations)) {
            productVertex = ctx.getOrCreateVertexAndUpdate(productId, visibility, elCtx -> {
                if (preview == null) {
                    WorkspaceProperties.PRODUCT_PREVIEW_DATA_URL.removeProperty(elCtx.getMutation(), user.getUserId(), visibility);
                } else {
                    StreamingPropertyValue value = new StreamingPropertyValue(new ByteArrayInputStream(preview.getImageData()), byte[].class);
                    value.store(true).searchIndex(false);
                    Metadata metadata = new Metadata();
                    metadata.add("http://visallo.org/product#previewImageMD5", preview.getMD5(), visibility);
                    WorkspaceProperties.PRODUCT_PREVIEW_DATA_URL.addPropertyValue(
                            elCtx.getMutation(),
                            user.getUserId(),
                            value,
                            metadata,
                            visibility
                    );
                }
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        getWorkQueueRepository().broadcastWorkProductPreviewChange(productVertex.getId(), workspaceId, user, preview == null ? null : preview.getMD5());

        return productVertexToProduct(workspaceId, productVertex, authorizations, null, user);
    }

    @Override
    public Product addOrUpdateProduct(String workspaceId, String productId, String title, String kind, JSONObject params, User user) {
        LOGGER.debug(
                "addOrUpdateProduct(workspaceId: %s, productId: %s, userId: %s)",
                workspaceId,
                productId,
                user.getUserId()
        );
        if (!hasWritePermissions(workspaceId, user)) {
            throw new VisalloAccessDeniedException(
                    "user " + user.getUserId() + " does not have write access to workspace " + workspaceId,
                    user,
                    workspaceId
            );
        }

        Vertex workspaceVertex = getVertex(workspaceId, user);
        Authorizations authorizations = getAuthorizationRepository().getGraphAuthorizations(
                user,
                VISIBILITY_STRING,
                workspaceId
        );
        Visibility visibility = VISIBILITY.getVisibility();

        AtomicBoolean isNew = new AtomicBoolean();
        Vertex productVertex;
        try (GraphUpdateContext ctx = graphRepository.beginGraphUpdate(Priority.NORMAL, user, authorizations)) {
            productVertex = ctx.getOrCreateVertexAndUpdate(productId, visibility, elemCtx -> {
                isNew.set(elemCtx.isNewElement());
                elemCtx.setConceptType(WorkspaceProperties.PRODUCT_CONCEPT_IRI);
                if (title != null) {
                    WorkspaceProperties.TITLE.updateProperty(elemCtx, title.substring(0, Math.min(title.length(), 128)), visibility);
                }
                if (kind != null) {
                    WorkspaceProperties.PRODUCT_KIND.updateProperty(elemCtx, kind, visibility);
                }
            });

            WorkProduct workProduct = getWorkProductByKind(
                    kind == null
                            ? WorkspaceProperties.PRODUCT_KIND.getPropertyValue(productVertex, null)
                            : kind
            );
            if (params != null) {
                workProduct.update(ctx, workspaceVertex, productVertex, params, user, visibility, authorizations);
            }
            String edgeId = workspaceVertex.getId() + "_hasProduct_" + productVertex.getId();
            ctx.getOrCreateEdgeAndUpdate(
                    edgeId,
                    workspaceId,
                    productVertex.getId(),
                    WorkspaceProperties.WORKSPACE_TO_PRODUCT_RELATIONSHIP_IRI,
                    visibility,
                    edgeCtx -> {
                    }
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        getGraph().flush();

        Workspace ws = findById(workspaceId, user);
        ClientApiWorkspace userWorkspace = toClientApi(ws, user, authorizations);

        String skipSourceId = null;
        if (params != null && params.has("broadcastOptions")) {
            JSONObject broadcastOptions = params.getJSONObject("broadcastOptions");
            if (broadcastOptions.optBoolean("preventBroadcastToSourceGuid", false)) {
                skipSourceId = broadcastOptions.getString("sourceGuid");
            }
        }
        getWorkQueueRepository().broadcastWorkProductChange(productVertex.getId(), userWorkspace, user, skipSourceId);

        Product product = productVertexToProduct(workspaceId, productVertex, authorizations, null, user);
        if (isNew.get()) {
            fireWorkspaceAddProduct(product, user);
        }
        fireWorkspaceProductUpdated(product, params, user);
        return product;
    }

    public void deleteProduct(String workspaceId, String productId, User user) {
        LOGGER.debug("deleteProduct(productId: %s, userId: %s)", productId, user.getUserId());
        if (!hasWritePermissions(workspaceId, user)) {
            throw new VisalloAccessDeniedException(
                    "user " + user.getUserId() + " does not have write access to workspace " + workspaceId,
                    user,
                    workspaceId
            );
        }

        fireWorkspaceBeforeDeleteProduct(workspaceId, productId, user);

        Authorizations authorizations = getAuthorizationRepository().getGraphAuthorizations(
                user,
                VISIBILITY_STRING,
                workspaceId
        );

        Vertex productVertex = getGraph().getVertex(productId, authorizations);
        Iterable<Edge> productElementEdges = productVertex.getEdges(
                Direction.OUT,
                WorkProductElements.WORKSPACE_PRODUCT_TO_ENTITY_RELATIONSHIP_IRI,
                authorizations
        );
        for (Edge productToElement : productElementEdges) {
            getGraph().softDeleteEdge(productToElement, authorizations);
        }

        getGraph().softDeleteVertex(productId, authorizations);
        getGraph().flush();

        Workspace ws = findById(workspaceId, user);
        ClientApiWorkspace userWorkspace = toClientApi(ws, user, authorizations);
        getWorkQueueRepository().broadcastWorkProductDelete(productId, userWorkspace);

    }

    protected WorkProduct getWorkProductByKind(String kind) {
        if (kind == null) {
            throw new VisalloException("Work product kind must not be null");
        }
        if (workProducts == null) {
            if (configuration == null) {
                throw new VisalloException("Configuration not injected");
            } else {
                workProducts = InjectHelper.getInjectedServices(WorkProduct.class, configuration);
            }
        }
        Optional<WorkProduct> foundProduct = workProducts.stream().filter(
                workProduct -> workProduct.getClass().getName().equals(kind)
        ).findFirst();

        if (foundProduct.isPresent()) {
            return foundProduct.get();
        } else {
            throw new VisalloException("Work Product of kind: " + kind + " not found");
        }
    }

    @Override
    public InputStream getProductPreviewById(String workspaceId, String productId, User user) {
        Authorizations authorizations = getAuthorizationRepository().getGraphAuthorizations(
                user,
                VISIBILITY_STRING,
                workspaceId
        );
        Vertex productVertex = getGraph().getVertex(productId, authorizations);
        if (productVertex != null) {
            Property previewDataUrlProperty = WorkspaceProperties.PRODUCT_PREVIEW_DATA_URL.getProperty(productVertex, user.getUserId());
            if (previewDataUrlProperty != null) {
                StreamingPropertyValue previewValue = (StreamingPropertyValue) previewDataUrlProperty.getValue();
                if (previewValue != null) {
                    return previewValue.getInputStream();
                }
            }
        }
        return null;
    }

    @Override
    public Product findProductById(String workspaceId, String productId, JSONObject params, boolean includeExtended, User user) {
        Authorizations authorizations = getAuthorizationRepository().getGraphAuthorizations(
                user,
                VISIBILITY_STRING,
                workspaceId
        );
        Vertex productVertex = getGraph().getVertex(productId, authorizations);
        if (productVertex == null) {
            return null;
        }

        String kind = WorkspaceProperties.PRODUCT_KIND.getPropertyValue(productVertex);
        WorkProduct workProduct = getWorkProductByKind(kind);
        JSONObject extendedData = null;
        if (includeExtended) {
            extendedData = workProduct.getExtendedData(getGraph(), getVertex(workspaceId, user), productVertex, params, user, authorizations);
        }

        return productVertexToProduct(workspaceId, productVertex, authorizations, extendedData, user);
    }

    private ProductPreview getProductPreviewFromUrl(String url) {
        if (url != null && url.contains("base64")) {
            String encodingPrefix = "base64,";
            int contentStartIndex = url.indexOf(encodingPrefix) + encodingPrefix.length();
            byte[] imageData = Base64.getDecoder().decode(url.substring(contentStartIndex));
            return new ProductPreview(imageData, DigestUtils.md5Hex(imageData));
        }
        return null;
    }

    private void createEdge(
            Vertex workspaceVertex,
            Vertex otherVertex,
            Authorizations authorizations
    ) {
        String workspaceVertexId = workspaceVertex.getId();
        String entityVertexId = otherVertex.getId();
        String edgeId = getWorkspaceToEntityEdgeId(workspaceVertexId, entityVertexId);
        EdgeBuilder edgeBuilder = getGraph().prepareEdge(
                edgeId,
                workspaceVertex,
                otherVertex,
                WORKSPACE_TO_ENTITY_RELATIONSHIP_IRI,
                VISIBILITY.getVisibility()
        );
        edgeBuilder.setIndexHint(IndexHint.DO_NOT_INDEX);
        edgeBuilder.save(authorizations);
    }

    @Override
    public void deleteUserFromWorkspace(final Workspace workspace, final String userId, final User user) {
        if (!hasWritePermissions(workspace.getWorkspaceId(), user)) {
            throw new VisalloAccessDeniedException(
                    "user " + user.getUserId() + " does not have write access to workspace " + workspace.getWorkspaceId(),
                    user,
                    workspace.getWorkspaceId()
            );
        }

        lockRepository.lock(getLockName(workspace), () -> {
            Authorizations authorizations = getAuthorizationRepository().getGraphAuthorizations(
                    user,
                    UserRepository.VISIBILITY_STRING,
                    VISIBILITY_STRING,
                    workspace.getWorkspaceId()
            );
            Vertex userVertex = getGraph().getVertex(userId, authorizations);
            if (userVertex == null) {
                throw new VisalloResourceNotFoundException("Could not find user: " + userId, userId);
            }
            Vertex workspaceVertex = getVertexFromWorkspace(workspace, true, authorizations);
            List<Edge> edges = stream(workspaceVertex.getEdges(
                    userVertex,
                    Direction.BOTH,
                    WORKSPACE_TO_USER_RELATIONSHIP_IRI,
                    authorizations
            )).collect(Collectors.toList());
            for (Edge edge : edges) {
                getGraph().softDeleteEdge(edge, authorizations);
            }
            getGraph().flush();

            clearCache();
        });

        fireWorkspaceDeleteUser(workspace, userId, user);
    }

    @Override
    public boolean hasCommentPermissions(String workspaceId, User user) {
        if (user instanceof SystemUser) {
            return true;
        }

        String cacheKey = workspaceId + user.getUserId();
        Boolean hasCommentAccess = usersWithCommentAccessCache.getIfPresent(cacheKey);
        if (hasCommentAccess != null && hasCommentAccess) {
            return true;
        }

        List<WorkspaceUser> usersWithAccess = findUsersWithAccess(workspaceId, user);
        for (WorkspaceUser userWithAccess : usersWithAccess) {
            if (userWithAccess.getUserId().equals(user.getUserId())
                    && WorkspaceAccess.hasCommentPermissions(userWithAccess.getWorkspaceAccess())) {
                usersWithCommentAccessCache.put(cacheKey, true);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasWritePermissions(String workspaceId, User user) {
        if (user instanceof SystemUser) {
            return true;
        }

        String cacheKey = workspaceId + user.getUserId();
        Boolean hasWriteAccess = usersWithWriteAccessCache.getIfPresent(cacheKey);
        if (hasWriteAccess != null && hasWriteAccess) {
            return true;
        }

        List<WorkspaceUser> usersWithAccess = findUsersWithAccess(workspaceId, user);
        for (WorkspaceUser userWithAccess : usersWithAccess) {
            if (userWithAccess.getUserId().equals(user.getUserId())
                    && WorkspaceAccess.hasWritePermissions(userWithAccess.getWorkspaceAccess())) {
                usersWithWriteAccessCache.put(cacheKey, true);
                return true;
            }
        }
        return false;
    }

    @Override
    @Traced
    public boolean hasReadPermissions(String workspaceId, User user) {
        if (user instanceof SystemUser) {
            return true;
        }

        String cacheKey = workspaceId + user.getUserId();
        Boolean hasReadAccess = usersWithReadAccessCache.getIfPresent(cacheKey);
        if (hasReadAccess != null && hasReadAccess) {
            return true;
        }

        List<WorkspaceUser> usersWithAccess = findUsersWithAccess(workspaceId, user);
        for (WorkspaceUser userWithAccess : usersWithAccess) {
            if (userWithAccess.getUserId().equals(user.getUserId())
                    && WorkspaceAccess.hasReadPermissions(userWithAccess.getWorkspaceAccess())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public UpdateUserOnWorkspaceResult updateUserOnWorkspace(
            final Workspace workspace,
            final String userId,
            final WorkspaceAccess workspaceAccess,
            final User user
    ) {
        if (!hasWritePermissions(workspace.getWorkspaceId(), user)) {
            throw new VisalloAccessDeniedException(
                    "user " + user.getUserId() + " does not have write access to workspace " + workspace.getWorkspaceId(),
                    user,
                    workspace.getWorkspaceId()
            );
        }

        return lockRepository.lock(getLockName(workspace), () -> {
            Authorizations authorizations = getAuthorizationRepository().getGraphAuthorizations(
                    user,
                    VISIBILITY_STRING,
                    workspace.getWorkspaceId()
            );
            Vertex otherUserVertex;
            if (userRepository instanceof VertexiumUserRepository) {
                otherUserVertex = ((VertexiumUserRepository) userRepository).findByIdUserVertex(userId);
            } else {
                otherUserVertex = getGraph().getVertex(userId, authorizations);
            }
            if (otherUserVertex == null) {
                throw new VisalloResourceNotFoundException("Could not find user: " + userId, userId);
            }

            Vertex workspaceVertex = getVertexFromWorkspace(workspace, true, authorizations);
            if (workspaceVertex == null) {
                throw new VisalloResourceNotFoundException(
                        "Could not find workspace vertex: " + workspace.getWorkspaceId(),
                        workspace.getWorkspaceId()
                );
            }

            UpdateUserOnWorkspaceResult result;
            List<Edge> existingEdges = stream(workspaceVertex.getEdges(
                    otherUserVertex,
                    Direction.OUT,
                    WORKSPACE_TO_USER_RELATIONSHIP_IRI,
                    authorizations
            )).collect(Collectors.toList());
            if (existingEdges.size() > 0) {
                for (Edge existingEdge : existingEdges) {
                    WorkspaceProperties.WORKSPACE_TO_USER_ACCESS.setProperty(
                            existingEdge,
                            workspaceAccess.toString(),
                            VISIBILITY.getVisibility(),
                            authorizations
                    );
                }
                result = UpdateUserOnWorkspaceResult.UPDATE;
            } else {
                EdgeBuilder edgeBuilder = getGraph().prepareEdge(
                        workspaceVertex,
                        otherUserVertex,
                        WORKSPACE_TO_USER_RELATIONSHIP_IRI,
                        VISIBILITY.getVisibility()
                );
                WorkspaceProperties.WORKSPACE_TO_USER_ACCESS.setProperty(
                        edgeBuilder,
                        workspaceAccess.toString(),
                        VISIBILITY.getVisibility()
                );
                edgeBuilder.save(authorizations);
                result = UpdateUserOnWorkspaceResult.ADD;
            }

            getGraph().flush();

            clearCache();

            fireWorkspaceUpdateUser(workspace, userId, workspaceAccess, user);

            return result;
        });
    }

    @Override
    @Traced
    public ClientApiWorkspaceDiff getDiff(
            final Workspace workspace,
            final User user,
            final Locale locale,
            final String timeZone
    ) {
        if (!hasReadPermissions(workspace.getWorkspaceId(), user)) {
            throw new VisalloAccessDeniedException(
                    "user " + user.getUserId() + " does not have read access to workspace " + workspace.getWorkspaceId(),
                    user,
                    workspace.getWorkspaceId()
            );
        }

        return lockRepository.lock(getLockName(workspace), () -> {
            List<WorkspaceEntity> workspaceEntities = findEntitiesNoLock(workspace, true, true, user);
            Iterable<Edge> workspaceEdges = findModifiedEdges(workspace, workspaceEntities, true, user);

            FormulaEvaluator.UserContext userContext = new FormulaEvaluator.UserContext(
                    locale,
                    timeZone,
                    workspace.getWorkspaceId()
            );
            return workspaceDiff.diff(workspace, workspaceEntities, workspaceEdges, userContext, user);
        });
    }

    private class ProductPreview {
        private byte[] imageData;
        private String md5;

        ProductPreview(byte[] imageData, String md5) {
            this.imageData = imageData;
            this.md5 = md5;
        }

        public byte[] getImageData() {
            return imageData;
        }

        public String getMD5() {
            return md5;
        }
    }
}
