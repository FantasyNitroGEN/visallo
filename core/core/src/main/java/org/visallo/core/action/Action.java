package org.visallo.core.action;

import org.json.JSONObject;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.user.User;

public abstract class Action {
    public static final String PROPERTY_TYPE = "type";

    public abstract void execute(ActionExecuteParameters parameters, User user);

    public void validateData(JSONObject actionData) {
        validateDataHas(actionData, PROPERTY_TYPE);
    }

    protected void validateDataHas(JSONObject actionData, String propertyName) {
        if (!actionData.has(propertyName)) {
            throw new VisalloException("Could not find " + propertyName + " in data: " + actionData.toString());
        }
    }

    protected static JSONObject createWorkflowData(Class clazz) {
        JSONObject data = new JSONObject();
        data.put("type", clazz.getName());
        return data;
    }
}
