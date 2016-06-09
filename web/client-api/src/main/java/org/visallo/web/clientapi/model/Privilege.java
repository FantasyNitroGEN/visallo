package org.visallo.web.clientapi.model;

import org.json.JSONArray;
import org.visallo.web.clientapi.util.StringUtils;

import java.util.*;

public final class Privilege {
    public static final String READ = "READ";
    public static final String COMMENT = "COMMENT"; // add comments and edit/delete own comments
    public static final String COMMENT_EDIT_ANY = "COMMENT_EDIT_ANY"; // edit other users' comments
    public static final String COMMENT_DELETE_ANY = "COMMENT_DELETE_ANY"; // delete other users' comments
    public static final String EDIT = "EDIT";
    public static final String PUBLISH = "PUBLISH";
    public static final String ADMIN = "ADMIN";

    public static final Set<String> NONE = Collections.emptySet();

    private final String name;

    public Privilege(String name) {
        this.name = name;
    }

    public static Set<String> newSet(String... privileges) {
        Set<String> set = new HashSet<String>();
        Collections.addAll(set, privileges);
        return Collections.unmodifiableSet(set);
    }

    private static List<String> sortPrivileges(Collection<String> privileges) {
        List<String> sortedPrivileges = new ArrayList<String>(privileges);
        Collections.sort(sortedPrivileges);
        return sortedPrivileges;
    }

    public static JSONArray toJson(Set<String> privileges) {
        JSONArray json = new JSONArray();
        for (String privilege : sortPrivileges(privileges)) {
            json.put(privilege);
        }
        return json;
    }

    public static Set<String> stringToPrivileges(String privilegesString) {
        if (privilegesString == null || privilegesString.equalsIgnoreCase("NONE")) {
            return NONE;
        }

        String[] privilegesStringParts = privilegesString.split(",");
        Set<String> privileges = new HashSet<String>();
        for (String privilegesStringPart : privilegesStringParts) {
            if (privilegesStringPart.trim().length() == 0) {
                continue;
            }
            privileges.add(privilegesStringPart.trim());
        }
        return privileges;
    }

    public static String toString(Collection<String> privileges) {
        return StringUtils.join(sortPrivileges(privileges));
    }

    public static boolean hasAll(Set<String> userPrivileges, Set<String> requiredPrivileges) {
        for (String privilege : requiredPrivileges) {
            if (!userPrivileges.contains(privilege)) {
                return false;
            }
        }
        return true;
    }

    public String getName() {
        return name;
    }
}
