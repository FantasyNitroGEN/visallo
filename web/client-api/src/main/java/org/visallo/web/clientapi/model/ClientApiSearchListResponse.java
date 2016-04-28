package org.visallo.web.clientapi.model;

import java.util.HashSet;
import java.util.Set;

public class ClientApiSearchListResponse implements ClientApiObject {
    public Set<ClientApiSearch> searches = new HashSet<ClientApiSearch>();

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
