package org.visallo.tools.args;

import com.beust.jcommander.Parameter;

public class CreateUserArgs extends Args {
    @Parameter(names = {"--username", "-u"}, required = true, arity = 1, description = "The username of the user to view or edit")
    public String userName;

    @Parameter(names = {"--password"}, required = true, arity = 1, description = "The password value to set")
    public String password;

    @Parameter(names = {"--displayname"}, arity = 1, description = "Display name to set")
    public String displayName;

    @Parameter(names = {"--email"}, arity = 1, description = "E-mail address to set")
    public String email;

    @Parameter(names = {"--authorizations", "-a"}, arity = 1, description = "Comma separated list of authorizations to set, or none")
    public String authorizations;

    @Parameter(names = {"--privileges", "-p"}, arity = 1, description = "Comma separated list of privileges to set, one or more of: READ, COMMENT, EDIT, PUBLISH, ADMIN or NONE")
    public String privileges;
}
