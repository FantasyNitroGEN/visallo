package org.visallo.web.clientapi.model;

public class ClientApiVertexCount implements ClientApiObject {
    private final long count;

    public ClientApiVertexCount(long count) {
        this.count = count;
    }

    public long getCount() {
        return count;
    }
}
