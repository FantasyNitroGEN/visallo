package org.visallo.web.clientapi.model;

import java.util.Map;

public class ClientApiProduct implements ClientApiObject {
    public String id;
    public String workspaceId;
    public String title;
    public String kind;
    public Map<String, Object> data;
    public Map<String, Object> extendedData;
    public String previewMD5;

    public ClientApiProduct(String id, String workspaceId, String title, String kind, Map<String, Object> data, Map<String, Object> extendedData, String previewMD5) {
        this.id = id;
        this.workspaceId = workspaceId;
        this.title = title;
        this.kind = kind;
        this.data = data;
        this.extendedData = extendedData;
        this.previewMD5 = previewMD5;
    }
}
