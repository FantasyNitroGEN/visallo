package org.visallo.core.action;

import org.json.JSONObject;
import org.vertexium.Element;

public class ActionExecuteParameters {
    private final Element element;
    private final JSONObject data;

    public ActionExecuteParameters(Element element, JSONObject data) {
        this.element = element;
        this.data = data;
    }

    public Element getElement() {
        return element;
    }

    public JSONObject getData() {
        return data;
    }
}
