package org.visallo.core.security;

import org.visallo.web.clientapi.model.VisibilityJson;
import org.vertexium.Visibility;

import java.util.Map;

public abstract class VisibilityTranslator {
    public static final String JSON_SOURCE = "source";
    public static final String JSON_WORKSPACES = "workspaces";

    public abstract void init(Map configuration);

    public abstract VisalloVisibility toVisibility(VisibilityJson visibilityJson);

    public abstract VisalloVisibility toVisibility(String visibilitySource);

    public abstract Visibility toVisibilityNoSuperUser(VisibilityJson visibilityJson);

    public abstract Visibility getDefaultVisibility();
}
