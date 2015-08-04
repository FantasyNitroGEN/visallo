package org.visallo.tools;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameters;
import org.apache.commons.lang.StringUtils;
import org.vertexium.Authorizations;
import org.visallo.core.cmdline.CommandLineTool;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.tools.args.*;
import org.visallo.web.clientapi.model.Privilege;
import org.visallo.web.clientapi.model.UserStatus;

import java.text.SimpleDateFormat;
import java.util.*;

import static org.vertexium.util.IterableUtils.toList;

@Parameters(commandDescription = "User administration")
public class UserAdmin extends CommandLineTool {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(UserAdmin.class, "cli-userAdmin");
    private Args args;
    private UserAdminAction userAdminAction;

    public static void main(String[] args) throws Exception {
        CommandLineTool.main(new UserAdmin(), args);
    }

    @Override
    protected JCommander parseArguments(String[] args) {
        try {
            userAdminAction = UserAdminAction.parse(args[0]);
            if (userAdminAction == null) {
                throw new VisalloException("Could not parse UserAdminAction");
            }
        } catch (Exception ex) {
            System.err.println("Action must be one of: " + StringUtils.join(UserAdminAction.getActions(), " | "));
            return null;
        }
        switch (userAdminAction) {
            case CMD_ACTION_CREATE:
                this.args = new CreateUserArgs();
                break;
            case CMD_ACTION_LIST:
                this.args = new ListUsersArgs();
                break;
            case CMD_ACTION_ACTIVE:
                this.args = new ListActiveUsersArgs();
                break;
            case CMD_ACTION_UPDATE_PASSWORD:
                this.args = new UpdatePasswordArgs();
                break;
            case CMD_ACTION_DELETE:
                this.args = new DeleteUserArgs();
                break;
            case CMD_ACTION_SET_PRIVILEGES:
                this.args = new SetPrivilegesArgs();
                break;
            case CMD_ACTION_SET_AUTHORIZATIONS:
                this.args = new SetAuthorizationsArgs();
                break;
            case CMD_ACTION_SET_DISPLAYNAME_EMAIL:
                this.args = new SetDisplayNameEmailArgs();
                break;
            default:
                throw new VisalloException("Unhandled userAdminAction: " + userAdminAction);
        }
        JCommander j = new JCommander(this.args, Arrays.copyOfRange(args, 1, args.length));
        if (this.args.help) {
            this.printHelp(j);
            return null;
        } else {
            this.args.validate(j);
        }
        return j;
    }

    @Override
    protected int run() throws Exception {
        LOGGER.info("running %s", userAdminAction);
        try {
            switch (userAdminAction) {
                case CMD_ACTION_CREATE:
                    return create((CreateUserArgs) this.args);
                case CMD_ACTION_LIST:
                    return list((ListUsersArgs) this.args);
                case CMD_ACTION_ACTIVE:
                    return active((ListActiveUsersArgs) this.args);
                case CMD_ACTION_UPDATE_PASSWORD:
                    return updatePassword((UpdatePasswordArgs) this.args);
                case CMD_ACTION_DELETE:
                    return delete((DeleteUserArgs) this.args);
                case CMD_ACTION_SET_PRIVILEGES:
                    return setPrivileges((SetPrivilegesArgs) this.args);
                case CMD_ACTION_SET_AUTHORIZATIONS:
                    return setAuthorizations((SetAuthorizationsArgs) args);
                case CMD_ACTION_SET_DISPLAYNAME_EMAIL:
                    return setDisplayNameAndOrEmail((SetDisplayNameEmailArgs) this.args);
            }
        } catch (UserNotFoundException ex) {
            System.err.println(ex.getMessage());
            return 2;
        }
        throw new VisalloException("Unhandled userAdminAction: " + userAdminAction);
    }

    private int create(CreateUserArgs args) {
        List<String> authorizations = new ArrayList<>();
        if (args.authorizations != null && args.authorizations.length() > 0) {
            authorizations.addAll(Arrays.asList(StringUtils.split(args.authorizations, ',')));
        }
        Set<Privilege> privileges = null;
        if (args.privileges != null) {
            privileges = Privilege.stringToPrivileges(args.privileges);
        }

        getUserRepository().findOrAddUser(args.userName, args.userName, null, args.password, authorizations.toArray(new String[authorizations.size()]));

        User user = getUserRepository().findByUsername(args.userName);

        if (privileges != null) {
            getUserRepository().setPrivileges(user, privileges, getUserRepository().getSystemUser());
        }
        if (args.displayName != null) {
            getUserRepository().setDisplayName(user, args.displayName);
        }
        if (args.email != null) {
            getUserRepository().setEmailAddress(user, args.email);
        }

        printUser(getUserRepository().findById(user.getUserId()));
        return 0;
    }

    private int list(ListUsersArgs args) {
        int skip = 0;
        int limit = 100;
        List<User> sortedUsers = new ArrayList<>();
        while (true) {
            List<User> users = toList(getUserRepository().find(skip, limit));
            if (users.size() == 0) {
                break;
            }
            sortedUsers.addAll(users);
            skip += limit;
        }
        Collections.sort(sortedUsers, new Comparator<User>() {
            @Override
            public int compare(User u1, User u2) {
                Date d1 = u1.getCreateDate();
                Date d2 = u2.getCreateDate();
                if (d1 != null && d2 != null) {
                    return d1.compareTo(d2);
                }
                return 0;
            }
        });
        if (args.asTable) {
            printUsers(sortedUsers);
        } else {
            for (User user : sortedUsers) {
                printUser(user);
            }
        }
        return 0;
    }

    private int active(ListActiveUsersArgs args) {
        int skip = 0;
        int limit = 100;
        List<User> activeUsers = new ArrayList<>();
        while (true) {
            List<User> users = toList(getUserRepository().findByStatus(skip, limit, UserStatus.ACTIVE));
            if (users.size() == 0) {
                break;
            }
            activeUsers.addAll(users);
            skip += limit;
        }
        System.out.println(activeUsers.size() + " " + UserStatus.ACTIVE + " user" + (activeUsers.size() == 1 ? "" : "s"));
        printUsers(activeUsers);

        if (args.showIdle) {
            skip = 0;
            limit = 100;
            List<User> idleUsers = new ArrayList<>();
            while (true) {
                List<User> users = toList(getUserRepository().findByStatus(skip, limit, UserStatus.IDLE));
                if (users.size() == 0) {
                    break;
                }
                idleUsers.addAll(users);
                skip += limit;
            }
            System.out.println(idleUsers.size() + " " + UserStatus.IDLE + " user" + (idleUsers.size() == 1 ? "" : "s"));
            printUsers(idleUsers);
        }

        return 0;
    }

    private int updatePassword(UpdatePasswordArgs args) {
        User user = findUser(args);
        getUserRepository().setPassword(user, args.password);
        printUser(user);
        return 0;
    }

    private int delete(DeleteUserArgs args) {
        User user = findUser(args);
        getUserRepository().delete(user);
        System.out.println("Deleted user " + user.getUserId());
        return 0;
    }

    private int setPrivileges(SetPrivilegesArgs args) {
        Set<Privilege> privileges = Privilege.stringToPrivileges(args.privileges);

        User user = findUser(args);

        System.out.println("Assigning privileges " + privileges + " to user " + user.getUserId());
        getUserRepository().setPrivileges(user, privileges, getUserRepository().getSystemUser());
        user = getUserRepository().findById(user.getUserId());

        printUser(user);
        return 0;
    }

    private int setAuthorizations(SetAuthorizationsArgs args) {
        List<String> authorizations = new ArrayList<>();
        if (args.authorizations != null && args.authorizations.length() > 0) {
            authorizations.addAll(Arrays.asList(StringUtils.split(args.authorizations, ',')));
        }

        User user = findUser(args);

        for (String auth : getUserRepository().getAuthorizations(user).getAuthorizations()) {
            if (authorizations.contains(auth)) {
                System.out.println("Keeping authorization:  " + auth);
                authorizations.remove(auth); // so we don't add it later
            } else {
                System.out.println("Removing authorization: " + auth);
                getUserRepository().removeAuthorization(user, auth, getUserRepository().getSystemUser());
            }
        }
        for (String auth : authorizations) {
            System.out.println("Adding authorization:   " + auth);
            getUserRepository().addAuthorization(user, auth, getUserRepository().getSystemUser());
        }
        System.out.println("");

        printUser(user);
        return 0;
    }

    private int setDisplayNameAndOrEmail(SetDisplayNameEmailArgs args) {
        if (args.displayName == null && args.email == null) {
            System.out.println("no display name or e-mail address provided");
            return -2;
        }

        User user = findUser(args);

        if (args.displayName != null) {
            getUserRepository().setDisplayName(user, args.displayName);
        }
        if (args.email != null) {
            getUserRepository().setEmailAddress(user, args.email);
        }

        printUser(getUserRepository().findById(user.getUserId()));
        return 0;
    }

    private User findUser(FindUserArgs findUserArgs) {
        User user = null;
        if (findUserArgs.userName != null) {
            user = getUserRepository().findByUsername(findUserArgs.userName);
        } else if (findUserArgs.userId != null) {
            user = getUserRepository().findById(findUserArgs.userId);
        }

        if (user == null) {
            throw new UserNotFoundException(findUserArgs);
        }

        return user;
    }

    private void printUser(User user) {
        System.out.println("                        ID: " + user.getUserId());
        System.out.println("                  Username: " + user.getUsername());
        System.out.println("            E-Mail Address: " + valueOrBlank(user.getEmailAddress()));
        System.out.println("              Display Name: " + user.getDisplayName());
        System.out.println("               Create Date: " + valueOrBlank(user.getCreateDate()));
        System.out.println("        Current Login Date: " + valueOrBlank(user.getCurrentLoginDate()));
        System.out.println(" Current Login Remote Addr: " + valueOrBlank(user.getCurrentLoginRemoteAddr()));
        System.out.println("       Previous Login Date: " + valueOrBlank(user.getPreviousLoginDate()));
        System.out.println("Previous Login Remote Addr: " + valueOrBlank(user.getPreviousLoginRemoteAddr()));
        System.out.println("               Login Count: " + user.getLoginCount());
        System.out.println("                Privileges: " + privilegesAsString(getUserRepository().getPrivileges(user)));
        System.out.println("            Authorizations: " + authorizationsAsString(getUserRepository().getAuthorizations(user)));
        System.out.println("");
    }

    private void printUsers(Iterable<User> users) {
        if (users != null) {
            int maxCreateDateWidth = 1;
            int maxIdWidth = 1;
            int maxUsernameWidth = 1;
            int maxEmailAddressWidth = 1;
            int maxDisplayNameWidth = 1;
            int maxLoginCountWidth = 1;
            int maxPrivilegesWidth = privilegesAsString(Privilege.ALL).length();
            for (User user : users) {
                maxCreateDateWidth = maxWidth(user.getCreateDate(), maxCreateDateWidth);
                maxIdWidth = maxWidth(user.getUserId(), maxIdWidth);
                maxUsernameWidth = maxWidth(user.getUsername(), maxUsernameWidth);
                maxEmailAddressWidth = maxWidth(user.getEmailAddress(), maxEmailAddressWidth);
                maxDisplayNameWidth = maxWidth(user.getDisplayName(), maxDisplayNameWidth);
                maxLoginCountWidth = maxWidth(Integer.toString(user.getLoginCount()), maxLoginCountWidth);
            }
            String format = String.format("%%%ds %%%ds %%%ds %%%ds %%%ds %%%dd %%%ds%%n", -1 * maxCreateDateWidth,
                    -1 * maxIdWidth,
                    -1 * maxUsernameWidth,
                    -1 * maxEmailAddressWidth,
                    -1 * maxDisplayNameWidth,
                    maxLoginCountWidth,
                    -1 * maxPrivilegesWidth);
            for (User user : users) {
                System.out.printf(format,
                        valueOrBlank(user.getCreateDate()),
                        user.getUserId(),
                        user.getUsername(),
                        valueOrBlank(user.getEmailAddress()),
                        user.getDisplayName(),
                        user.getLoginCount(),
                        privilegesAsString(getUserRepository().getPrivileges(user))
                );
            }
        } else {
            System.out.println("No users");
        }
    }

    private String privilegesAsString(Set<Privilege> privileges) {
        SortedSet<Privilege> sortedPrivileges = new TreeSet<>(new Comparator<Privilege>() {
            @Override
            public int compare(Privilege p1, Privilege p2) {
                return p1.ordinal() - p2.ordinal();
            }
        });
        sortedPrivileges.addAll(privileges);
        return sortedPrivileges.toString().replaceAll(" ", "");
    }

    private String authorizationsAsString(Authorizations authorizations) {
        List<String> list = Arrays.asList(authorizations.getAuthorizations());
        if (list.size() > 0) {
            Collections.sort(list);
            return "[" + StringUtils.join(list, ',') + "]";
        } else {
            return "";
        }
    }

    private String valueOrBlank(Object o) {
        if (o == null) {
            return "";
        } else if (o instanceof Date) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
            return sdf.format(o);
        } else {
            return o.toString();
        }
    }

    private int maxWidth(Object o, int max) {
        int width = valueOrBlank(o).length();
        return width > max ? width : max;
    }
}
