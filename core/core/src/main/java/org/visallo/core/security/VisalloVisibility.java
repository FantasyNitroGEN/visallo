package org.visallo.core.security;

import org.vertexium.Visibility;

public class VisalloVisibility {
    public static final String SUPER_USER_VISIBILITY_STRING = "visallo";
    private final Visibility visibility;

    public VisalloVisibility() {
        this.visibility = new Visibility("");
    }

    public VisalloVisibility(String visibility) {
        if (visibility == null || visibility.length() == 0) {
            this.visibility = new Visibility("");
        } else {
            this.visibility = addSuperUser(visibility);
        }
    }

    public VisalloVisibility(Visibility visibility) {
        if (visibility == null || visibility.getVisibilityString().length() == 0
                || visibility.getVisibilityString().contains(SUPER_USER_VISIBILITY_STRING)) {
            this.visibility = visibility;
        } else {
            this.visibility = addSuperUser(visibility.getVisibilityString());
        }
    }

    public Visibility getVisibility() {
        return visibility;
    }

    private Visibility addSuperUser(String visibility) {
        return new Visibility("(" + visibility + ")|" + SUPER_USER_VISIBILITY_STRING);
    }

    @Override
    public String toString() {
        return getVisibility().toString();
    }

    public static Visibility and(Visibility visibility, String additionalVisibility) {
        if (visibility.getVisibilityString().length() == 0) {
            return new Visibility(additionalVisibility);
        }
        return new Visibility("(" + visibility.getVisibilityString() + ")&(" + additionalVisibility + ")");
    }

    public static Visibility or(Visibility visibility, String additionalVisibility) {
        if (visibility.getVisibilityString().length() == 0) {
            return new Visibility(additionalVisibility);
        }
        return new Visibility("(" + visibility.getVisibilityString() + ")|(" + additionalVisibility + ")");
    }
}
