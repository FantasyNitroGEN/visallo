package org.visallo.core.model.user.cli.args;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

public class FindUserArgs extends Args {
    @Parameter(names = {"--username", "-u"}, arity = 1, description = "The username of the user to view or edit")
    public String userName;

    @Parameter(names = {"--userid", "-i"}, arity = 1, description = "The id of the user to view or edit")
    public String userId;

    @Override
    public void validate(JCommander j) {
        super.validate(j);
        if (userName == null && userId == null) {
            throw new ParameterException("'--username, -u' or '--userid, -i' is required");
        }
    }
}
