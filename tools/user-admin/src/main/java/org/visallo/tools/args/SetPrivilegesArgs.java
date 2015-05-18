package org.visallo.tools.args;

import com.beust.jcommander.Parameter;

public class SetPrivilegesArgs extends FindUserArgs {
    @Parameter(names = {"--privileges", "-p"}, arity = 1, required = true, description = "Comma separated list of privileges to set, one or more of: READ, COMMENT, EDIT, PUBLISH, ADMIN or NONE")
    public String privileges;
}
