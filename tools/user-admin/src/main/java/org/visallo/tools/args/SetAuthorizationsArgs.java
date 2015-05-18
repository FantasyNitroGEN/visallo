package org.visallo.tools.args;

import com.beust.jcommander.Parameter;

public class SetAuthorizationsArgs extends FindUserArgs {
    @Parameter(names = {"--authorizations", "-a"}, arity = 1, required = true, description = "Comma separated list of authorizations to set, or none")
    public String authorizations;
}
