package org.visallo.core.model.notification;

import com.v5analytics.simpleorm.Field;
import com.v5analytics.simpleorm.Id;
import org.visallo.core.exception.VisalloException;
import org.apache.commons.codec.binary.Hex;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public abstract class Notification {
    public static final String ACTION_EVENT_EXTERNAL_URL = "EXTERNAL_URL";

    @Id
    private String id;

    @Field
    private String title;

    @Field
    private String message;

    @Field
    private String actionEvent;

    @Field
    private JSONObject actionPayload;

    protected Notification(String id, String title, String message, String actionEvent, JSONObject actionPayload) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.actionEvent = actionEvent;
        this.actionPayload = actionPayload;
    }

    protected Notification() {

    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getActionEvent() {
        return actionEvent;
    }

    public void setActionEvent(String actionEvent) {
        this.actionEvent = actionEvent;
    }

    public JSONObject getActionPayload() {
        return actionPayload;
    }

    public void setActionPayload(JSONObject actionPayload) {
        this.actionPayload = actionPayload;
    }

    public final JSONObject toJSONObject() {
        JSONObject json = new JSONObject();
        json.put("id", getId());
        json.put("title", getTitle());
        json.put("type", getType());
        json.put("message", getMessage());
        json.put("actionEvent", getActionEvent());
        json.put("actionPayload", getActionPayload());
        populateJSONObject(json);
        json.put("hash", hashJson(json));
        return json;
    }

    protected abstract void populateJSONObject(JSONObject json);

    protected abstract String getType();

    private static String hashJson(JSONObject json) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] md5 = digest.digest(json.toString().getBytes());
            return Hex.encodeHexString(md5);
        } catch (NoSuchAlgorithmException e) {
            throw new VisalloException("Could not find MD5", e);
        }
    }
}
