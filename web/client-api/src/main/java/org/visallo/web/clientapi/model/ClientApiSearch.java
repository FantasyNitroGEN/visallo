package org.visallo.web.clientapi.model;

import java.util.Map;

public class ClientApiSearch implements ClientApiObject {
    public String id;
    public String url;
    public String name;
    public Scope scope;
    public Map<String, Object> parameters;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ClientApiSearch that = (ClientApiSearch) o;

        if (!id.equals(that.id)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "ClientApiSearch{" +
                "id='" + id + '\'' +
                ", url='" + url + '\'' +
                ", name='" + name + '\'' +
                ", scope='" + scope + '\'' +
                ", parameters=" + parameters +
                '}';
    }

    public enum Scope {
        User,
        Global
    }
}
