package org.visallo.web.clientapi.model;

import java.util.ArrayList;
import java.util.List;

public class ClientApiTermMentionsResponse implements ClientApiObject {
    private List<ClientApiVertexiumObject> termMentions = new ArrayList<ClientApiVertexiumObject>();

    public List<ClientApiVertexiumObject> getTermMentions() {
        return termMentions;
    }
}
