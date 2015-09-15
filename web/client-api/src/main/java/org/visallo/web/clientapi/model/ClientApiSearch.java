package org.visallo.web.clientapi.model;

import java.util.Map;

public class ClientApiSearch implements ClientApiObject {
    public String id;
    public String url;
    public String name;
    public Map<String, Object> parameters;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ClientApiSearch that = (ClientApiSearch) o;

        if (!id.equals(that.id)) return false;
        if (!url.equals(that.url)) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (parameters != null ? !parameters.equals(that.parameters) : that.parameters != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + url.hashCode();
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (parameters != null ? parameters.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ClientApiSearch{" +
                "id='" + id + '\'' +
                ", url='" + url + '\'' +
                ", name='" + name + '\'' +
                ", parameters=" + parameters +
                '}';
    }
}
