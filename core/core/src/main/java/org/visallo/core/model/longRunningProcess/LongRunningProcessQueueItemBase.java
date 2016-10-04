package org.visallo.core.model.longRunningProcess;

import org.json.JSONObject;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.util.ClientApiConverter;

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

    @SuppressWarnings("unchecked")
    public static <T> T createFromJson(JSONObject json) {
        String type = json.getString("type");
        try {
            Class<T> clazz = (Class<T>) Class.forName(type);
            return ClientApiConverter.toClientApi(json, clazz);
        } catch (ClassNotFoundException ex) {
            throw new VisalloException("Could not find type: " + type, ex);
        }
    }
}
