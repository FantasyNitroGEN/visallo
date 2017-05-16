package org.visallo.web.product.graph;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.stubbing.OngoingStubbing;
import org.vertexium.Authorizations;
import org.vertexium.Vertex;
import org.vertexium.Visibility;
import org.visallo.core.bootstrap.InjectHelper;
import org.visallo.core.config.Configuration;
import org.visallo.core.model.workspace.product.Product;
import org.visallo.core.model.workspace.product.WorkProduct;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloInMemoryTestBase;
import org.visallo.web.clientapi.model.GraphPosition;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

public class GraphWorkProductTest extends VisalloInMemoryTestBase {
    private User user;
    private Authorizations authorizations;
    private Visibility defaultVisibility;
    private Vertex productVertex;
    private GraphWorkProduct graphWorkProduct;
    private JSONObject includeAll = new JSONObject("{ includeVertices: true, includeEdges: true }");
    private String workspaceId = "w1";
    private String productId = "p1";
    private GraphPosition graphPosition1 = new GraphPosition(10, 10);
    private GraphPosition graphPosition2 = new GraphPosition(20, 20);
    private GraphPosition graphPosition3 = new GraphPosition(30, 30);

    @Mock
    private InjectHelper injectHelper;

    @Mock
    private Configuration configuration;

    @Before
    public void before() {
        super.before();
        user = getUserRepository().getSystemUser();
        authorizations = getAuthorizationRepository().getGraphAuthorizations(user);
        defaultVisibility = getVisibilityTranslator().getDefaultVisibility();

        getGraph().prepareVertex("v1", defaultVisibility).save(authorizations);
        getGraph().prepareVertex("v2", defaultVisibility).save(authorizations);
        getGraph().prepareVertex("v3", defaultVisibility).save(authorizations);

        JSONObject updateVertices = new JSONObject();
        JSONObject v1Data = createVertexUpdateJson("v1", graphPosition1, null, null);
        JSONObject v2Data = createVertexUpdateJson("v1", graphPosition2, null, null);
        JSONObject v3Data = createVertexUpdateJson("v1", graphPosition3, null, null);

        updateVertices.put("v1", v1Data);
        updateVertices.put("v2", v2Data);
        updateVertices.put("v3", v3Data);

        JSONObject params = new JSONObject();
        params.put("updateVertices", updateVertices);

        getWorkspaceRepository().addOrUpdateProduct(workspaceId, productId, null, null, params, user);
        productVertex = getGraph().getVertex(productId, authorizations);

        graphWorkProduct = new GraphWorkProduct(
                getOntologyRepository(),
                getAuthorizationRepository(),
                getVisibilityTranslator()
        );
    }
    @Test
    public void testAddCompoundNode() {
        addCompoundNodeV1V2();
        Product product = getWorkspaceRepository().findProductById(workspaceId, productId, includeAll, true, user);
        JSONObject extendedData = new JSONObject(product.getExtendedData());

        assertTrue(extendedData.getJSONObject("vertices").length() == 3);

        JSONArray compoundNodes = extendedData.getJSONArray("compoundNodes");
        assertTrue(extendedData.getJSONObject("compoundNodes").length() == 1);
        JSONObject compoundNodeData = new JSONObject(compoundNodes.getJSONObject(0));
        assertTrue(compoundNodeData.getString("children").equals(new JSONArray("[v1, v2]")));

    }

    @Test
    public void testRemoveCompoundNode() {}

    @Test
    public void testUncollapseCompoundNode() {}

    @Test
    public void testRemoveVertexFromCompoundNode() {}

    @Test
    public void testAddVertexToCompoundNode() {}

    @Test
    public void testShouldGetCompoundNodesWithExtendedData() {}

    @Test
    public void testShouldCleanupCompoundNodes() {}

    private void addCompoundNodeV1V2() {
        JSONArray children = new JSONArray("[v1, v2]");
        JSONObject compoundUpdate = createVertexUpdateJson(null, graphPosition2, null, children);
        JSONArray updateVertices = new JSONArray();
        updateVertices.put(compoundUpdate);
        JSONObject params = new JSONObject();
        params.put("updateVertices", updateVertices);
        getWorkspaceRepository().addOrUpdateProduct(workspaceId, productId, null, null, params, user);
    }

    private JSONObject createVertexUpdateJson(String id, GraphPosition graphPosition, String parentId, JSONArray children) {
        JSONObject updateData = new JSONObject();

        updateData.putOpt("id", id);
        updateData.putOpt("pos", graphPosition.toJSONObject());
        updateData.putOpt("parent", parentId);
        updateData.putOpt("children", children);

        return updateData;
    }
}