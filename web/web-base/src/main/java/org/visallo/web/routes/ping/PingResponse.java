package org.visallo.web.routes.ping;

import org.visallo.web.clientapi.model.ClientApiObject;

public class PingResponse implements ClientApiObject {
    private final long searchTime;
    private final long retrievalTime;
    private final long saveTime;
    private final long enqueueTime;

    public PingResponse(long searchTime, long retrievalTime, long saveTime, long enqueueTime) {
        this.searchTime = searchTime;
        this.retrievalTime = retrievalTime;
        this.saveTime = saveTime;
        this.enqueueTime = enqueueTime;
    }

    public long getSearchTime() {
        return searchTime;
    }

    public long getRetrievalTime() {
        return retrievalTime;
    }

    public long getSaveTime() {
        return saveTime;
    }

    public long getEnqueueTime() {
        return enqueueTime;
    }
}
