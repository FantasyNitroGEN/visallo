package org.visallo.web.clientapi.model;

public class ClientApiProductUpdateResponse implements ClientApiObject {
    public String id;

    public ClientApiProductUpdateResponse(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
