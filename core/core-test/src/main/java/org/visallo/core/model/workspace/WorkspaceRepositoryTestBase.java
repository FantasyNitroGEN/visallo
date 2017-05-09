package org.visallo.core.model.workspace;

import org.json.JSONObject;
import org.junit.Test;
import org.visallo.core.model.workspace.product.Product;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloInMemoryTestBase;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public abstract class WorkspaceRepositoryTestBase extends VisalloInMemoryTestBase {
    @Override
    protected abstract WorkspaceRepository getWorkspaceRepository();

    @Test
    public void testUpdateProductExtendedData() {
        User user = getUserRepository().getSystemUser();

        Workspace workspace = getWorkspaceRepository().add("ws1", "workspace 1", user);
        String workspaceId = workspace.getWorkspaceId();

        String kind = MockWorkProduct.class.getName();
        JSONObject params = new JSONObject();
        JSONObject extendedData = new JSONObject();
        extendedData.put("key1", "value1");
        extendedData.put("key2", "value2");
        extendedData.put("key3", new JSONObject("{\"a\":\"b\"}"));
        params.put("extendedData", extendedData);
        Product product = getWorkspaceRepository().addOrUpdateProduct(
                workspaceId,
                "p1",
                "product 1",
                kind,
                params,
                user
        );
        String productId = product.getId();

        product = getWorkspaceRepository().findProductById(workspaceId, productId, new JSONObject(), true, user);
        assertEquals("value1", product.getExtendedData().get("key1"));
        assertEquals("value2", product.getExtendedData().get("key2"));
        Object value3 = product.getExtendedData().get("key3");
        assertTrue(value3 instanceof Map);
        assertEquals("b", ((Map) value3).get("a"));

        // update extended data
        params = new JSONObject();
        extendedData = new JSONObject();
        extendedData.put("key1", JSONObject.NULL);
        extendedData.put("key2", "value2b");
        extendedData.put("key3", new JSONObject("{\"a\":\"b2\"}"));
        params.put("extendedData", extendedData);
        getWorkspaceRepository().addOrUpdateProduct(workspaceId, productId, null, null, params, user);

        product = getWorkspaceRepository().findProductById(workspaceId, productId, new JSONObject(), true, user);
        assertEquals(null, product.getExtendedData().get("key1"));
        assertEquals("value2b", product.getExtendedData().get("key2"));
        value3 = product.getExtendedData().get("key3");
        assertTrue(value3 instanceof Map);
        assertEquals("b2", ((Map) value3).get("a"));
    }
}
