package org.visallo.tools;

import org.visallo.tools.args.FindUserArgs;

public class UserNotFoundException extends RuntimeException {
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
