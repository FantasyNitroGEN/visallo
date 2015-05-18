package org.visallo.tools.args;

import com.beust.jcommander.Parameter;

public class UpdatePasswordArgs extends FindUserArgs {
    @Parameter(names = {"--password"}, arity = 1, required = true, description = "The password value to set")
    public String password;
}
