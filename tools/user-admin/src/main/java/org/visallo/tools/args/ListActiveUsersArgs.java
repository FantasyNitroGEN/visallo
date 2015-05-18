package org.visallo.tools.args;

import com.beust.jcommander.Parameter;

public class ListActiveUsersArgs extends Args {
    @Parameter(names = {"--idle"}, description = "Include idle users")
    public boolean showIdle = false;
}
