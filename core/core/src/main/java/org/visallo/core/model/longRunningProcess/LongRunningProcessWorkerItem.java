package org.visallo.core.model.longRunningProcess;

import org.json.JSONObject;
import org.visallo.core.ingest.graphProperty.WorkerItem;

public class LongRunningProcessWorkerItem extends WorkerItem {
    private final JSONObject json;

    public LongRunningProcessWorkerItem(byte[] data) {
        this(new String(data));
    }

    public LongRunningProcessWorkerItem(String data) {
        this(new JSONObject(data));
    }

    public LongRunningProcessWorkerItem(JSONObject json) {
        this.json = json;
    }

    public JSONObject getJson() {
        return json;
    }
}
