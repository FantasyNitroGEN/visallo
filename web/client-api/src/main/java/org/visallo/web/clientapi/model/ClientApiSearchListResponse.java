package org.visallo.web.clientapi.model;

import java.util.ArrayList;
import java.util.List;

public class ClientApiSearchListResponse implements ClientApiObject {
    public List<ClientApiSearch> searches = new ArrayList<ClientApiSearch>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ClientApiSearchListResponse that = (ClientApiSearchListResponse) o;

        if (!searches.equals(that.searches)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return searches.hashCode();
    }

    @Override
    public String toString() {
        return "ClientApiSearchListResponse{" +
                "searches=" + searches +
                '}';
    }
}
