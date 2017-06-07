package org.visallo.web.product.graph;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.google.inject.Inject;
import org.json.JSONArray;
import org.json.JSONObject;
import org.vertexium.*;
import org.vertexium.mutation.ElementMutation;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.graph.ElementUpdateContext;
import org.visallo.core.model.graph.GraphRepository;
import org.visallo.core.model.graph.GraphUpdateContext;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.user.AuthorizationRepository;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.model.workspace.WorkspaceProperties;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.model.workspace.product.WorkProductElements;
import org.visallo.core.security.VisalloVisibility;
import org.visallo.core.security.VisibilityTranslator;
import org.visallo.core.user.User;
import org.visallo.core.util.JSONUtil;
import org.visallo.core.util.StreamUtil;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.clientapi.model.GraphPosition;
import org.visallo.web.clientapi.model.VisibilityJson;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.visallo.web.product.graph.GraphProductOntology.ENTITY_POSITION;

public class GraphWorkProduct extends WorkProductElements {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(GraphWorkProduct.class);
    private final AuthorizationRepository authorizationRepository;
    private final GraphRepository graphRepository;
    private final UserRepository userRepository;
    private static final VisalloVisibility VISIBILITY = new VisalloVisibility(WorkspaceRepository.VISIBILITY_STRING);
    private static final String ROOT_NODE_ID = "root";

    @Inject
    public GraphWorkProduct(
            OntologyRepository ontologyRepository,
            AuthorizationRepository authorizationRepository,
            GraphRepository graphRepository,
            UserRepository userRepository
    ) {
        super(ontologyRepository, authorizationRepository);
        this.authorizationRepository = authorizationRepository;
        this.graphRepository = graphRepository;
        this.userRepository = userRepository;
    }

    @Override
    public JSONObject getExtendedData(
            Graph graph,
            Vertex workspaceVertex,
            Vertex productVertex,
            JSONObject params,
            User user,
            Authorizations authorizations
    ) {
        JSONObject extendedData = new JSONObject();
        String id = productVertex.getId();

        if (params.optBoolean("includeVertices")) {
            trimCompoundNodes(graph, productVertex);

            JSONObject vertices = new JSONObject();
            JSONObject compoundNodes = new JSONObject();

            List<Edge> productVertexEdges = Lists.newArrayList(productVertex.getEdges(
                    Direction.OUT,
                    WorkspaceProperties.PRODUCT_TO_ENTITY_RELATIONSHIP_IRI,
                    authorizations
            ));

            List<String> ids = productVertexEdges.stream()
                    .map(edge -> edge.getOtherVertexId(id))
                    .collect(Collectors.toList());
            Map<String, Vertex> othersById = StreamUtil.stream(graph.getVertices(ids, FetchHint.NONE, authorizations))
                    .collect(Collectors.toMap(Vertex::getId, Function.identity()));

            for (Edge propertyVertexEdge : productVertexEdges) {
                String otherId = propertyVertexEdge.getOtherVertexId(id);
                JSONObject vertexOrNode = new JSONObject();
                vertexOrNode.put("id", otherId);
                if (!othersById.containsKey(otherId)) {
                    vertexOrNode.put("unauthorized", true);
                }
                setEdgeJson(propertyVertexEdge, vertexOrNode);
                if (vertexOrNode.getString("type").equals("vertex")) {
                    vertices.put(otherId, vertexOrNode);
                } else {
                    compoundNodes.put(otherId, vertexOrNode);
                }
            }

            if (compoundNodes.length() > 0) {
                JSONUtil.stream(compoundNodes.toJSONArray(compoundNodes.names())) //TODO: synchronize this so I can take advantage of chains? (e.g c1 > c2 > c3 > v1, if v1 is visible all are visible)
                    .forEach(compoundNode -> {
                        ArrayDeque<JSONObject> childrenDFS = Queues.newArrayDeque();
                        Map<String,Object> compoundNodeMap = (Map<String, Object>) compoundNode;
                        String childId = ((String) ((List) compoundNodeMap.get("children")).get(0));
                        JSONObject firstChild = vertices.optJSONObject(childId);
                        if (firstChild == null) {
                            firstChild = compoundNodes.optJSONObject(childId);
                        }
                        childrenDFS.push(firstChild);

                        boolean visible = (boolean) compoundNodeMap.getOrDefault("visible", false);
                        while (!visible && !childrenDFS.isEmpty()) {
                            JSONObject child = childrenDFS.poll();
                            JSONArray children = child.optJSONArray("children");

                            if (children != null) {
                                JSONUtil.stream(children).forEach(nextChildId -> {
                                    JSONObject nextChild = vertices.optJSONObject((String) nextChildId);
                                    if (nextChild == null) {
                                        nextChild = compoundNodes.optJSONObject((String) nextChildId);
                                    }

                                    childrenDFS.push(nextChild);
                                });
                            } else {
                                child = vertices.optJSONObject(childId);
                                visible = !child.optBoolean("unauthorized");
                            }
                        }

                        compoundNodeMap.put("visible", visible);
                        compoundNodes.put((String) compoundNodeMap.get("id"), compoundNode);
                    });
            }

            extendedData.put("vertices", vertices);
            extendedData.put("compoundNodes", compoundNodes);
        }

        if (params.optBoolean("includeEdges")) {
            JSONObject edges = new JSONObject();
            JSONArray unauthorizedEdgeIds = new JSONArray();
            Authorizations systemAuthorizations = authorizationRepository.getGraphAuthorizations(
                    user,
                    VisalloVisibility.SUPER_USER_VISIBILITY_STRING
            );
            Iterable<Vertex> productVertices = Lists.newArrayList(productVertex.getVertices(
                    Direction.OUT,
                    WorkspaceProperties.PRODUCT_TO_ENTITY_RELATIONSHIP_IRI,
                    systemAuthorizations
            ));
            Iterable<RelatedEdge> productRelatedEdges = graph.findRelatedEdgeSummaryForVertices(productVertices, authorizations);
            List<String> ids = StreamUtil.stream(productRelatedEdges)
                    .map(RelatedEdge::getEdgeId)
                    .collect(Collectors.toList());
            Map<String, Boolean> relatedEdgesById = graph.doEdgesExist(ids, authorizations);

            for (RelatedEdge relatedEdge : productRelatedEdges) {
                String edgeId = relatedEdge.getEdgeId();
                if (relatedEdgesById.get(edgeId)) {
                    JSONObject edge = new JSONObject();
                    edge.put("edgeId", relatedEdge.getEdgeId());
                    edge.put("label", relatedEdge.getLabel());
                    edge.put("outVertexId", relatedEdge.getOutVertexId());
                    edge.put("inVertexId", relatedEdge.getInVertexId());
                    edges.put(edgeId, edge);
                } else {
                    unauthorizedEdgeIds.put(edgeId);
                }
            }
            extendedData.put("edges", edges);
            extendedData.put("unauthorizedEdgeIds", unauthorizedEdgeIds);
        }

        return extendedData;
    }

    private void trimCompoundNodes(Graph graph, Vertex productVertex) {
        String id = productVertex.getId();
        User systemUser = userRepository.getSystemUser();
        Visibility visibility = VISIBILITY.getVisibility();
        Authorizations authorizations = authorizationRepository.getGraphAuthorizations(systemUser);

        List<Edge> productVertexEdges = Lists.newArrayList(productVertex.getEdges(
                Direction.OUT,
                WorkspaceProperties.PRODUCT_TO_ENTITY_RELATIONSHIP_IRI,
                authorizations
        ));

        List<String> ids = productVertexEdges.stream()
                .map(edge -> edge.getOtherVertexId(id))
                .collect(Collectors.toList());
        Map<String, Vertex> othersById = StreamUtil.stream(graph.getVertices(ids, FetchHint.NONE, authorizations))
                .collect(Collectors.toMap(Vertex::getId, Function.identity()));

        try (GraphUpdateContext ctx = graphRepository.beginGraphUpdate(Priority.NORMAL, systemUser, authorizations)) {
            for (Edge propertyVertexEdge : productVertexEdges) {
                String otherId = propertyVertexEdge.getOtherVertexId(id);
                if (!othersById.containsKey(otherId)) {
                    removeChild(ctx, productVertex, otherId, id, visibility, authorizations);
                }
            }
        } catch(Exception ex) {
            throw new RuntimeException("Could not clean compound Nodes", ex);
        }

    }

    @Override
    public void cleanUpElements(
            Graph graph,
            Vertex productVertex,
            Authorizations authorizations
    ) {
        Iterable<Edge> productElementEdges = productVertex.getEdges(
                Direction.OUT,
                WorkspaceProperties.PRODUCT_TO_ENTITY_RELATIONSHIP_IRI,
                authorizations
        );

        for (Edge productToElement : productElementEdges) {
            if (GraphProductOntology.NODE_CHILDREN.hasProperty(productToElement)) {
                String otherElementId = productToElement.getOtherVertexId(productVertex.getId());
                graph.softDeleteVertex(otherElementId, authorizations);
            } else {
                graph.softDeleteEdge(productToElement, authorizations);
            }
        }

        graph.flush();
    }

    public JSONObject addCompoundNode(
            GraphUpdateContext ctx,
            Vertex productVertex,
            JSONObject params,
            User user,
            Visibility visibility,
            Authorizations authorizations
    ) {
        try {
            VisibilityJson visibilityJson = VisibilityJson.updateVisibilitySource(null, "");

            String vertexId = params.optString("id", null);
            GraphUpdateContext.UpdateFuture<Vertex> vertexFuture = ctx.getOrCreateVertexAndUpdate(vertexId, null, visibility, elemCtx -> {
                elemCtx.setConceptType(GraphProductOntology.CONCEPT_TYPE_COMPOUND_NODE);
                elemCtx.updateBuiltInProperties(new Date(), visibilityJson);
            });
            vertexId = vertexFuture.get().getId();

            String edgeId = getEdgeId(productVertex.getId(), vertexId);
            ctx.getOrCreateEdgeAndUpdate(edgeId, productVertex.getId(),
                    vertexId,
                    WorkspaceProperties.PRODUCT_TO_ENTITY_RELATIONSHIP_IRI,
                    visibility,
                    elemCtx -> updateProductEdge(elemCtx, params, visibility)
            );

            ctx.flush();

            JSONArray children = params.getJSONArray("children");
            List<String> childIds = JSONUtil.toStringList(children);
            for (String childId : childIds) {
                updateParent(ctx, productVertex, childId, vertexId, true, visibility, authorizations);
            }

            JSONObject json = new JSONObject();
            json.put("id", vertexId);
            json.put("visible", true);
            setEdgeJson(ctx.getGraph().getEdge(edgeId, authorizations), json);
            return json;
        }catch(Exception ex){
            throw new VisalloException("Could not add compound node",ex);
        }
    }


    public void updateVertices(
            GraphUpdateContext ctx,
            Vertex productVertex,
            JSONObject updateVertices,
            User user,
            Visibility visibility,
            Authorizations authorizations
    ) {
        @SuppressWarnings("unchecked")
        List<String> vertexIds = Lists.newArrayList(updateVertices.keys());
//        List<String> newCompoundNodes = new ArrayList<>();

        for (String id : vertexIds) {
            JSONObject updateData = updateVertices.getJSONObject(id);
            String edgeId = getEdgeId(productVertex.getId(), id);

            //undoing compound node deletion
            if (updateData.optJSONObject("children") != null && !ctx.getGraph().doesVertexExist(id, authorizations)) {
                addCompoundNode(ctx, productVertex, updateData, user, visibility, authorizations);
            }

            EdgeBuilderByVertexId edgeBuilder = ctx.getGraph().prepareEdge(
                    edgeId,
                    productVertex.getId(),
                    id,
                    WorkspaceProperties.PRODUCT_TO_ENTITY_RELATIONSHIP_IRI,
                    visibility
            );
            ctx.update(edgeBuilder, elemCtx -> updateProductEdge(elemCtx, updateData, visibility));
        }

//        for (String id : newCompoundNodes) {
//            String edgeId = getEdgeId(productVertex.getId(), id);
//            Edge productVertexEdge = ctx.getGraph().getEdge(edgeId, authorizations);
//            JSONArray children = GraphProductOntology.NODE_CHILDREN.getPropertyValue(productVertexEdge);
//            JSONUtil.stream(children).forEach(childId -> {
//                updateParent(ctx, productVertex, (String) childId, id, true, visibility, authorizations);
//            });
//        }

    }

    public void removeVertices(
            GraphUpdateContext ctx,
            Vertex productVertex,
            JSONArray removeVertices,
            boolean removeChildren,
            User user,
            Visibility visibility,
            Authorizations authorizations
    ) {
        JSONUtil.toList(removeVertices)
                .forEach(id -> {
                    String edgeId = getEdgeId(productVertex.getId(), (String) id);
                    Edge productVertexEdge = ctx.getGraph().getEdge(edgeId, authorizations);
                    String parentId = GraphProductOntology.PARENT_NODE.getPropertyValue(productVertexEdge);
                    JSONArray children = GraphProductOntology.NODE_CHILDREN.getPropertyValue(productVertexEdge);

                    if (children != null && children.length() > 0) {
                        if (removeChildren) {
                            Queue<String> childIdQueue = Queues.newSynchronousQueue();
                            JSONUtil.toStringList(children).forEach(childIdQueue::add);

                            while (!childIdQueue.isEmpty()) {
                                String childId = childIdQueue.poll();
                                String childEdgeId = getEdgeId(productVertex.getId(), childId);

                                Edge childEdge = ctx.getGraph().getEdge(childEdgeId, authorizations);
                                JSONArray nextChildren = GraphProductOntology.NODE_CHILDREN.getPropertyValue(childEdge);
                                JSONUtil.toStringList(nextChildren).forEach(childIdQueue::add);

                                if (nextChildren != null) {
                                    ctx.getGraph().softDeleteVertex(childId, authorizations);
                                } else {
                                    ctx.getGraph().softDeleteEdge(childEdgeId, authorizations);
                                }
                            }
                        } else {
                            JSONUtil.toStringList(children).forEach(childId -> {
                                updateParent(ctx, productVertex, childId, parentId, false, visibility, authorizations);
                            });
                            ctx.getGraph().softDeleteVertex((String) id, authorizations);
                        }
                    } else {
                        ctx.getGraph().softDeleteEdge(edgeId, authorizations);
                    }

                    if (!ROOT_NODE_ID.equals(parentId)) {
                        removeChild(ctx, productVertex, (String) id, parentId, visibility, authorizations);
                    }

                });
    }

    private void addChild(
            GraphUpdateContext ctx,
            Vertex productVertex,
            String childId,
            String parentId,
            Visibility visibility,
            Authorizations authorizations
    ) {
        if (parentId.equals(ROOT_NODE_ID)) return;

        String parentEdgeId = getEdgeId(productVertex.getId(), parentId);
        Edge parentProductVertexEdge = ctx.getGraph().getEdge(parentEdgeId, authorizations);

        JSONArray children = GraphProductOntology.NODE_CHILDREN.getPropertyValue(parentProductVertexEdge);
        if (!JSONUtil.arrayContains(children, childId)) {
            children.put(childId);

            EdgeBuilderByVertexId parentEdgeBuilder = ctx.getGraph().prepareEdge(
                    parentEdgeId,
                    productVertex.getId(),
                    childId,
                    WorkspaceProperties.PRODUCT_TO_ENTITY_RELATIONSHIP_IRI,
                    visibility
            );
            ctx.update(parentEdgeBuilder, elemCtx -> {
                GraphProductOntology.NODE_CHILDREN.updateProperty(elemCtx, children, visibility);
            });

            String childEdgeId = getEdgeId(productVertex.getId(), childId);
            EdgeBuilderByVertexId childEdgeBuilder = ctx.getGraph().prepareEdge(
                    childEdgeId,
                    productVertex.getId(),
                    childId,
                    WorkspaceProperties.PRODUCT_TO_ENTITY_RELATIONSHIP_IRI,
                    visibility
            );
            ctx.update(childEdgeBuilder, elemCtx -> {
                GraphProductOntology.PARENT_NODE.updateProperty(elemCtx, parentId, visibility);
            });
        }
    }

    private void removeChild(
            GraphUpdateContext ctx,
            Vertex productVertex,
            String childId,
            String parentId,
            Visibility visibility,
            Authorizations authorizations
    ) {
        if (parentId.equals(ROOT_NODE_ID)) return;

        String edgeId = getEdgeId(productVertex.getId(), parentId);
        Edge productVertexEdge = ctx.getGraph().getEdge(edgeId, authorizations);
        JSONArray children = GraphProductOntology.NODE_CHILDREN.getPropertyValue(productVertexEdge);

        for (int i = 0; i < children.length(); i++) {
            if (children.get(i).equals(childId)) {
                children.remove(i);
                break;
            }
            i++;
        }
        if (children.length() == 0) {
            ctx.getGraph().softDeleteVertex(parentId, authorizations);

            String ancestorId = GraphProductOntology.PARENT_NODE.getPropertyValue(productVertexEdge);
            if (ancestorId.equals(productVertex.getId())) {
                removeChild(ctx, productVertex, parentId, ancestorId, visibility, authorizations);
            }
        } else {
            EdgeBuilderByVertexId edgeBuilder = ctx.getGraph().prepareEdge(
                    edgeId,
                    parentId,
                    childId,
                    WorkspaceProperties.PRODUCT_TO_ENTITY_RELATIONSHIP_IRI,
                    visibility
            );
            edgeBuilder.setProperty(GraphProductOntology.NODE_CHILDREN.getPropertyName(), children, visibility);
        }

    }

    private void updateParent(
            GraphUpdateContext ctx,
            Vertex productVertex,
            String childId,
            String parentId,
            boolean removeFromPrevious,
            Visibility visibility,
            Authorizations authorizations
    ) {
        String edgeId = getEdgeId(productVertex.getId(), childId);
        Edge productVertexEdge = ctx.getGraph().getEdge(edgeId, authorizations);
        JSONObject updateData = new JSONObject();
        GraphPosition graphPosition;

        String oldParentId = GraphProductOntology.PARENT_NODE.getPropertyValue(productVertexEdge);
        graphPosition = calculatePositionFromParents(ctx, productVertex, childId, oldParentId, parentId,  false, authorizations); //TODO: get if new parent is ancestor

        updateData.put("pos", graphPosition.toJSONObject());
        updateData.put("parent", parentId);
        EdgeBuilderByVertexId edgeBuilder = ctx.getGraph().prepareEdge(
                edgeId,
                productVertex.getId(),
                childId,
                WorkspaceProperties.PRODUCT_TO_ENTITY_RELATIONSHIP_IRI,
                visibility
        );
        ctx.update(edgeBuilder, elemCtx -> updateProductEdge(elemCtx, updateData, visibility));

        if (removeFromPrevious) {
            removeChild(ctx, productVertex, childId, oldParentId, visibility, authorizations);
        }

        addChild(ctx, productVertex, childId, parentId, visibility, authorizations);
    }

    private GraphPosition calculatePositionFromParents(
            GraphUpdateContext ctx,
            Vertex productVertex,
            String childId,
            String oldParentId,
            String newParentId,
            boolean oldParentIsAncestor,
            Authorizations authorizations
    ) {
        GraphPosition parentOffset;
        String parentOffsetId = oldParentIsAncestor ? newParentId : oldParentId;
        if (parentOffsetId.equals(ROOT_NODE_ID)) {
            parentOffset = new GraphPosition(0, 0);
        } else {
            String offsetEdgeId = getEdgeId(productVertex.getId(), parentOffsetId);
            Edge offsetEdge = ctx.getGraph().getEdge(offsetEdgeId, authorizations);
            parentOffset = getGraphPosition(offsetEdge);
        }

        String childEdgeId = getEdgeId(productVertex.getId(), childId);
        Edge childEdge = ctx.getGraph().getEdge(childEdgeId, authorizations);

        GraphPosition graphPosition = getGraphPosition(childEdge);

        if (oldParentIsAncestor) {
            graphPosition.add(parentOffset);
        } else {
            graphPosition.subtract(parentOffset);
        }

        return graphPosition;
    }

    private GraphPosition getGraphPosition(Edge productVertexEdge) {
        JSONObject edgePositionData = ENTITY_POSITION.getPropertyValue(productVertexEdge);
        GraphPosition graphPosition = new GraphPosition(edgePositionData);
        return graphPosition;
    }

    @Override
    protected void updateProductEdge(ElementUpdateContext<Edge> elemCtx, JSONObject update, Visibility visibility) {
        JSONObject position = update.optJSONObject("pos");
        if (position != null) {
            ENTITY_POSITION.updateProperty(elemCtx, position, visibility);
        }

        String parent = update.optString("parent");
        if (parent != null) {
            GraphProductOntology.PARENT_NODE.updateProperty(elemCtx, parent, visibility);
        }

        JSONArray children = update.optJSONArray("children");
        if (children != null) {
            GraphProductOntology.NODE_CHILDREN.updateProperty(elemCtx, children, visibility);
        }
    }

    protected void updateProductEdge(ElementMutation<Edge> edgeMutation, JSONObject update, Visibility visibility) {
        JSONObject position = update.optJSONObject("pos");
        if (position != null) {
            edgeMutation.setProperty(ENTITY_POSITION.getPropertyName(), position, visibility);
        }

        String parent = update.optString("parent");
        if (parent != null) {
            edgeMutation.setProperty(GraphProductOntology.PARENT_NODE.getPropertyName(), position, visibility);
        }

        JSONArray children = update.optJSONArray("children");
        if (children != null) {
            edgeMutation.setProperty(GraphProductOntology.NODE_CHILDREN.getPropertyName(), position, visibility);
        }
    }

    protected void setEdgeJson(Edge propertyVertexEdge, JSONObject vertex) {
        JSONObject position = ENTITY_POSITION.getPropertyValue(propertyVertexEdge);
        String parent = GraphProductOntology.PARENT_NODE.getPropertyValue(propertyVertexEdge, ROOT_NODE_ID);
        JSONArray children = GraphProductOntology.NODE_CHILDREN.getPropertyValue(propertyVertexEdge);

        if (position == null) {
            position = new JSONObject();
            position.put("x", 0);
            position.put("y", 0);
        }
        vertex.put("pos", position);

        if (children != null) {
            vertex.put("children", children);
            vertex.put("type", "compoundNode");
        } else {
            vertex.put("type", "vertex");
        }

        vertex.put("parent", parent);
    }
}
