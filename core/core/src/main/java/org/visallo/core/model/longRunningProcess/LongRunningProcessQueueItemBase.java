package org.visallo.core.model.longRunningProcess;

import org.json.JSONObject;

public abstract class LongRunningProcessQueueItemBase {
    public String getType() {
        return this.getClass().getName();
    }

    public static boolean isA(JSONObject json, Class<? extends LongRunningProcessQueueItemBase> clazz) {
        String type = json.optString("type");
        if (type == null) {
            return false;
        }
        return clazz.getName().equals(type);
    }
}
