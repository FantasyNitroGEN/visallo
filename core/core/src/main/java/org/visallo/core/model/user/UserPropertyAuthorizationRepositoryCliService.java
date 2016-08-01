package org.visallo.core.model.user;

import com.beust.jcommander.Parameter;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang.StringUtils;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.user.cli.AuthorizationRepositoryCliService;
import org.visallo.core.model.user.cli.UserAdmin;
import org.visallo.core.model.user.cli.args.Args;
import org.visallo.core.model.user.cli.args.CreateUserArgs;
import org.visallo.core.model.user.cli.args.FindUserArgs;
import org.visallo.core.user.User;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class UserPropertyAuthorizationRepositoryCliService implements AuthorizationRepositoryCliService {
    private static final String ACTION_SET_AUTHORIZATIONS = "set-authorizations";
    private static final String CLI_PARAMETER_AUTHORIZATIONS = "authorizations";
    private static final char SEPARATOR_CHAR = ',';
    private final UserPropertyAuthorizationRepository authorizationRepository;

    public UserPropertyAuthorizationRepositoryCliService(UserPropertyAuthorizationRepository authorizationRepository) {
        this.authorizationRepository = authorizationRepository;
    }

    @Override
    public void onCreateUser(UserAdmin userAdmin, CreateUserArgs createUserArgs, User user, User authUser) {
        String authorizationsString = createUserArgs.authorizationRepositoryArguments.get(CLI_PARAMETER_AUTHORIZATIONS);
        ImmutableSet<String> newAuthorizations;
        if (authorizationsString == null) {
            newAuthorizations = authorizationRepository.getDefaultAuthorizations();
        } else {
            String[] authorizations = StringUtils.split(authorizationsString, SEPARATOR_CHAR);
            newAuthorizations = ImmutableSet.copyOf(authorizations);
        }
        authorizationRepository.setAuthorizations(user, newAuthorizations, authUser);
    }

    @Override
    public void onPrintUser(UserAdmin userAdmin, Args args, String formatString, User user) {
        Set<String> authorizations = authorizationRepository.getAuthorizations(user);
        String authorizationsString = Joiner.on(SEPARATOR_CHAR).join(authorizations);
        System.out.println(String.format(formatString, "Authorizations", authorizationsString));
    }

    @Override
    public Collection<String> getActions(UserAdmin userAdmin) {
        return ImmutableList.of(ACTION_SET_AUTHORIZATIONS);
    }

    @Override
    public Args createArguments(UserAdmin userAdmin, String action) {
        switch (action) {
            case ACTION_SET_AUTHORIZATIONS:
                return new SetAuthorizationsCliArguments();
        }
        return null;
    }

    @Override
    public int run(UserAdmin userAdmin, String action, Args args, User authUser) {
        switch (action) {
            case ACTION_SET_AUTHORIZATIONS:
                return cliRunSetAuthorizations(userAdmin, (SetAuthorizationsCliArguments) args, authUser);
        }
        throw new VisalloException("Unhandled cli action " + action);
    }

    private int cliRunSetAuthorizations(UserAdmin userAdmin, SetAuthorizationsCliArguments args, User authUser) {
        Set<String> authorizations = new HashSet<>();
        if (args.authorizations != null && args.authorizations.length() > 0) {
            authorizations.addAll(Arrays.asList(StringUtils.split(args.authorizations, SEPARATOR_CHAR)));
        }

        User user = userAdmin.findUser(args);
        authorizationRepository.setAuthorizations(user, authorizations, authUser);
        userAdmin.printUser(user);
        return 0;
    }

    @Override
    public void validateArguments(UserAdmin userAdmin, String action, Args args) {
        switch (action) {
            case UserAdmin.ACTION_CREATE:
                validateCreateUserArguments((CreateUserArgs) args);
                break;
        }
    }

    @Override
    public void printHelp(UserAdmin userAdmin, String action) {
        switch (action) {
            case UserAdmin.ACTION_CREATE:
            case ACTION_SET_AUTHORIZATIONS:
                System.out.println("  Authorizations:");
                System.out.println("    -A" + CLI_PARAMETER_AUTHORIZATIONS + "=<authorizations>");
                System.out.println("       Comma separated list of authorizations");
                System.out.println();
                break;
        }
    }

    private void validateCreateUserArguments(CreateUserArgs args) {
        int s = args.authorizationRepositoryArguments.size();
        if (s == 0) {
            return;
        }
        String authsString = args.authorizationRepositoryArguments.get(CLI_PARAMETER_AUTHORIZATIONS);
        if ((s == 1 && authsString == null) || s != 1) {
            throw new VisalloException(this.getClass().getName() + " expects no parameters or '" + CLI_PARAMETER_AUTHORIZATIONS + "'");
        }
    }

    public static class SetAuthorizationsCliArguments extends FindUserArgs {
        @Parameter(names = {"--authorizations", "-a"}, arity = 1, required = true, description = "Comma separated list of authorizations to set, or none")
        public String authorizations;
    }
}
