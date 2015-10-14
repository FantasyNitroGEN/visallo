package org.visallo.web.clientapi.model;

import org.visallo.web.clientapi.util.ClientApiConverter;

import java.util.ArrayList;
import java.util.List;

public class ClientApiElementFindRelatedResponse implements ClientApiObject {
    private long count;
    private List<ClientApiElement> elements = new ArrayList<>();

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public List<ClientApiElement> getElements() {
        return elements;
    }

    @Override
    public String toString() {
        return ClientApiConverter.clientApiToString(this);
    }
}
