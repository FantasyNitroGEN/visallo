package org.visallo.web.clientapi.model;

import org.json.JSONArray;
import org.visallo.web.clientapi.util.StringUtils;

import java.util.*;

public final class Privilege {
    public static final String READ = "READ";
    public static final String COMMENT = "COMMENT"; // add comments and edit/delete own comments
    public static final String COMMENT_EDIT_ANY = "COMMENT_EDIT_ANY"; // edit other users' comments
    public static final String COMMENT_DELETE_ANY = "COMMENT_DELETE_ANY"; // delete other users' comments
    public static final String HISTORY_READ = "HISTORY_READ"; // read vertex/edge/property history
    public static final String SEARCH_SAVE_GLOBAL = "SEARCH_SAVE_GLOBAL";
    public static final String EDIT = "EDIT";
    public static final String PUBLISH = "PUBLISH";
    public static final String ADMIN = "ADMIN";

    static {
        // NOTE: Keep allNames in sync with the above public static strings.
        final String[] allNames = new String[] {
                READ,
                COMMENT,
                COMMENT_EDIT_ANY,
                COMMENT_DELETE_ANY,
                HISTORY_READ,
                SEARCH_SAVE_GLOBAL,
                EDIT,
                PUBLISH,
                ADMIN
        };
        Set<Privilege> allPrivileges = new HashSet<Privilege>(allNames.length);
        for (String name : allNames) {
            allPrivileges.add(new Privilege(name));
        }
        ALL_BUILT_IN = Collections.unmodifiableSet(allPrivileges);
    }

    public static final Set<Privilege> ALL_BUILT_IN;

    private final String name;

    public Privilege(String name) {
        this.name = name;
    }

    public static Set<String> newSet(String... privileges) {
        Set<String> set = new HashSet<String>();
        Collections.addAll(set, privileges);
        return Collections.unmodifiableSet(set);
    }

    private static List<String> sortPrivileges(Iterable<String> privileges) {
        List<String> sortedPrivileges = new ArrayList<String>();
        for (String privilege : privileges) {
            sortedPrivileges.add(privilege);
        }
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
            return Collections.emptySet();
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

    public static String toString(Iterable<String> privileges) {
        return StringUtils.join(sortPrivileges(privileges));
    }

    public static String toStringPrivileges(Iterable<Privilege> privileges) {
        Collection<String> privilegeStrings = new ArrayList<String>();
        for (Privilege privilege : privileges) {
            privilegeStrings.add(privilege.getName());
        }
        return toString(privilegeStrings);
    }

    public static boolean hasAll(Set<String> userPrivileges, Set<String> requiredPrivileges) {
        for (String privilege : requiredPrivileges) {
            if (!userPrivileges.contains(privilege)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return getName();
    }

    public String getName() {
        return name;
    }
}
