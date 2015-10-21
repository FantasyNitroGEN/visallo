package org.visallo.core.model.longRunningProcess;

import org.json.JSONObject;
import org.vertexium.Authorizations;
import org.visallo.core.user.User;

import java.util.List;

public abstract class LongRunningProcessRepository {
    public static final String VISIBILITY_STRING = "longRunningProcess";

    public abstract String enqueue(JSONObject longRunningProcessQueueItem, User user, Authorizations authorizations);

    public void beginWork(JSONObject longRunningProcessQueueItem) {
    }

    public abstract void ack(JSONObject longRunningProcessQueueItem);

    public abstract void nak(JSONObject longRunningProcessQueueItem, Throwable ex);

    public abstract List<JSONObject> getLongRunningProcesses(User user);

    public abstract JSONObject findById(String longRunningProcessId, User user);

    public abstract void cancel(String longRunningProcessId, User user);

    public void reportProgress(JSONObject longRunningProcessQueueItem, double progressPercent, String message) {
        reportProgress(longRunningProcessQueueItem.getString("id"), progressPercent, message);
    }

    public abstract void reportProgress(String longRunningProcessId, double progressPercent, String message);

    public abstract void delete(String longRunningProcessId, User authUser);
}
