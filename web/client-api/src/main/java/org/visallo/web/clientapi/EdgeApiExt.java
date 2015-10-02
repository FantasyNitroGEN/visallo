package org.visallo.web.clientapi;

import org.visallo.web.clientapi.codegen.ApiException;
import org.visallo.web.clientapi.codegen.EdgeApi;
import org.visallo.web.clientapi.model.ClientApiEdgeWithVertexData;

public class EdgeApiExt extends EdgeApi {
    public ClientApiEdgeWithVertexData create(String outVertexId, String inVertexId, String label, String visibilitySource, String edgeId) throws ApiException {
        return create(outVertexId, inVertexId, label, visibilitySource, null, null, edgeId);
    }

    public void setProperty(String edgeId, String propertyKey, String propertyName, String value, String visibilitySource, String justificationText) throws ApiException {
        setProperty(edgeId, propertyKey, propertyName, value, visibilitySource, justificationText, null, null);
    }
}
