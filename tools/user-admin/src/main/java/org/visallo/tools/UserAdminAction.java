package org.visallo.tools;

import java.util.ArrayList;
import java.util.List;

public enum UserAdminAction {
    CMD_ACTION_CREATE("create"),
    CMD_ACTION_LIST("list"),
    CMD_ACTION_ACTIVE("active"),
    CMD_ACTION_EXPORT_PASSWORDS("export-passwords"),
    CMD_ACTION_UPDATE_PASSWORD("update-password"),
    CMD_ACTION_DELETE("delete"),
    CMD_ACTION_SET_DISPLAYNAME_EMAIL("set-displayname-and-or-email");

    private final String commandLineString;

    UserAdminAction(String commandLineString) {
        this.commandLineString = commandLineString;
    }

    public String getCommandLineString() {
        return commandLineString;
    }

    public static UserAdminAction parse(String str) {
        for (UserAdminAction v : UserAdminAction.values()) {
            if (v.getCommandLineString().equalsIgnoreCase(str)) {
                return v;
            }
            if (v.name().equalsIgnoreCase(str)) {
                return v;
            }
        }
        return null;
    }

    public static List<String> getActions() {
        List<String> results = new ArrayList<>();
        for (UserAdminAction v : UserAdminAction.values()) {
            results.add(v.getCommandLineString());
        }
        return results;
    }
}
