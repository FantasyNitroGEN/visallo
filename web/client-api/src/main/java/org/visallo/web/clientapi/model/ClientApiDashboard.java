package org.visallo.web.clientapi.model;

import java.util.ArrayList;
import java.util.List;

public class ClientApiDashboard implements ClientApiObject {
    public String id;
    public String workspaceId;
    public String title;
    public List<Item> items = new ArrayList<Item>();

    public static class Item {
        public String id;
        public String extensionId;
        public String title;
        public String configuration;
    }
}
