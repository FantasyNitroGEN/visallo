package org.visallo.core.security;

import org.visallo.web.clientapi.model.VisibilityJson;
import org.vertexium.Visibility;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DirectVisibilityTranslator extends VisibilityTranslator {

    public void init(Map configuration) {

    }

    @Override
    public VisalloVisibility toVisibility(VisibilityJson visibilityJson) {
        return new VisalloVisibility(toVisibilityNoSuperUser(visibilityJson));
    }

    @Override
    public VisalloVisibility toVisibility(String visibilitySource) {
        return new VisalloVisibility(toVisibilityNoSuperUser(new VisibilityJson(visibilitySource)));
    }

    @Override
    public Visibility toVisibilityNoSuperUser(VisibilityJson visibilityJson) {
        StringBuilder visibilityString = new StringBuilder();

        List<String> required = new ArrayList<String>();

        String source = visibilityJson.getSource();
        if (source != null && source.trim().length() > 0) {
            required.add(source.trim());
        }

        Set<String> workspaces = visibilityJson.getWorkspaces();
        if (workspaces != null) {
            required.addAll(workspaces);
        }

        for (String v : required) {
            if (visibilityString.length() > 0) {
                visibilityString.append("&");
            }
            visibilityString
                    .append("(")
                    .append(v)
                    .append(")");
        }
        return new Visibility(visibilityString.toString());
    }

    @Override
    public Visibility getDefaultVisibility() {
        return new Visibility("");
    }
}
