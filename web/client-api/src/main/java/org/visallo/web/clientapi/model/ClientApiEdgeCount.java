package org.visallo.web.clientapi.model;

public class ClientApiEdgeCount implements ClientApiObject {
    private final long count;

    public ClientApiEdgeCount(long count) {
        this.count = count;
    }

    public long getCount() {
        return count;
    }
}
