package org.visallo.web.clientapi.model;

import org.visallo.web.clientapi.util.ClientApiConverter;

import java.util.ArrayList;
import java.util.List;

public class ClientApiVertexFindPathResponse implements ClientApiObject {
    private List<List<String>> paths = new ArrayList<List<String>>();

    public List<List<String>> getPaths() {
        return paths;
    }

    @Override
    public String toString() {
        return ClientApiConverter.clientApiToString(this);
    }
}
