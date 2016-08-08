package org.visallo.core.model.user.cli.args;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

public class SetDisplayNameEmailArgs extends FindUserArgs {
    @Parameter(names = {"--displayname"}, arity = 1, description = "Display name to set")
    public String displayName;

    @Parameter(names = {"--email"}, arity = 1, description = "E-mail address to set")
    public String email;

    @Override
    public void validate(JCommander j) {
        super.validate(j);
        if (displayName == null && email == null) {
            throw new ParameterException("'--displayname' and/or '--email' is required");
        }
    }
}

