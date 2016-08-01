package org.visallo.core.model.user.cli;

import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.user.cli.args.FindUserArgs;

public class UserNotFoundException extends VisalloException {
    private static final long serialVersionUID = 6098547135173316004L;
    private final FindUserArgs findUserArgs;

    public UserNotFoundException(FindUserArgs findUserArgs) {
        this.findUserArgs = findUserArgs;
    }

    @Override
    public String getMessage() {
        if (findUserArgs.userName != null) {
            return "No user found with username: " + findUserArgs.userName;
        }

        if (findUserArgs.userId != null) {
            return "No user found with userid: " + findUserArgs.userId;
        }

        return "userName or userId not specified";
    }
}
