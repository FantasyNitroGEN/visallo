package org.visallo.web.clientapi.model;

import java.util.ArrayList;
import java.util.List;

public class ClientApiElementSearchResponse extends ClientApiSearchResponse {
    private List<ClientApiVertexiumObject> elements = new ArrayList<ClientApiVertexiumObject>();
    private List<ClientApiVertexiumObject> referencedElements;

    public List<ClientApiVertexiumObject> getElements() {
        return elements;
    }

    public List<ClientApiVertexiumObject> getReferencedElements() {
        return referencedElements;
    }

    public void setReferencedElements(List<ClientApiVertexiumObject> referencedElements) {
        this.referencedElements = referencedElements;
    }

    @Override
    public int getItemCount() {
        return getElements().size();
    }
}
