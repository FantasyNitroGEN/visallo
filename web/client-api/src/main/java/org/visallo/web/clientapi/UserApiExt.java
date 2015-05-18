package org.visallo.web.clientapi;

import org.visallo.web.clientapi.codegen.ApiException;
import org.visallo.web.clientapi.model.ClientApiUsers;
import org.visallo.web.clientapi.codegen.UserApi;

public class UserApiExt extends UserApi {
    public ClientApiUsers getAll() throws ApiException {
        return getAll(null, null);
    }

    public ClientApiUsers getAll(String query) throws ApiException {
        return getAll(query, null);
    }

    public ClientApiUsers getAllForWorkspace(String workspaceId) throws ApiException {
        return getAll(null, workspaceId);
    }

    public ClientApiUsers getAllForWorkspace(String query, String workspaceId) throws ApiException {
        return getAll(query, workspaceId);
    }
}
