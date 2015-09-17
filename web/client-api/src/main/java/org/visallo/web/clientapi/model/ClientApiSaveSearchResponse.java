package org.visallo.web.clientapi.model;

public class ClientApiSaveSearchResponse implements ClientApiObject {
    public String id;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ClientApiSaveSearchResponse that = (ClientApiSaveSearchResponse) o;

        if (!id.equals(that.id)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "ClientApiSaveSearchResponse{" +
                "id='" + id + '\'' +
                '}';
    }
}
