package org.visallo.web.clientapi.model;

import java.util.ArrayList;
import java.util.List;

public class ClientApiElementSearchResponse extends ClientApiSearchResponse {
    private List<ClientApiElement> elements = new ArrayList<ClientApiElement>();

    public List<ClientApiElement> getElements() {
        return elements;
    }

    @Override
    public int getItemCount() {
        return getElements().size();
    }
}
