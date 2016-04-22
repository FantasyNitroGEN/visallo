package org.visallo.tools.args;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.google.common.base.Strings;

public class UpdatePasswordArgs extends FindUserArgs {
    @Parameter(names = {"--password"}, arity = 1, required = false, description = "The password value to set")
    public String password;

    @Parameter(names = {"--passwordSaltAndHash"}, arity = 1, required = false, description = "The password salt:hash to set")
    public String passwordSaltAndHash;

    @Override
    public void validate(JCommander j) {
        super.validate(j);
        if (Strings.isNullOrEmpty(password) && Strings.isNullOrEmpty(passwordSaltAndHash)) {
            throw new ParameterException("'--password' or '--passwordSaltAndHash' is required");
        }
    }
}
