package org.visallo.tools.args;

import com.beust.jcommander.Parameter;

public class ListUsersArgs extends Args {
    @Parameter(names = {"--as-table", "-t"}, description = "List users in a table")
    public boolean asTable = false;
}
