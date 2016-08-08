package org.visallo.core.model.user.cli.args;

import com.beust.jcommander.DynamicParameter;
import com.beust.jcommander.Parameter;

import java.util.HashMap;
import java.util.Map;

public class CreateUserArgs extends Args {
    @Parameter(names = {"--username", "-u"}, required = true, arity = 1, description = "The username of the user to view or edit")
    public String userName;

    @Parameter(names = {"--password"}, required = true, arity = 1, description = "The password value to set")
    public String password;

    @Parameter(names = {"--displayname"}, arity = 1, description = "Display name to set")
    public String displayName;

    @Parameter(names = {"--email"}, arity = 1, description = "E-mail address to set")
    public String email;

    @DynamicParameter(names = "-A", description = "Additional arguments for the authorization repository")
    public Map<String, String> authorizationRepositoryArguments = new HashMap<>();

    @DynamicParameter(names = "-P", description = "Additional arguments for the privilege repository")
    public Map<String, String> privilegeRepositoryArguments = new HashMap<>();
}
