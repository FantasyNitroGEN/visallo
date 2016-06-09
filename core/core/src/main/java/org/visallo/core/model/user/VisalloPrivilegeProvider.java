package org.visallo.core.model.user;

import com.google.common.collect.ImmutableList;
import org.visallo.web.clientapi.model.Privilege;

import java.util.ArrayList;
import java.util.List;

public class VisalloPrivilegeProvider implements PrivilegesProvider {
    private static final ImmutableList<Privilege> ALL_BUILT_IN;

    static {
        List<Privilege> allBuiltIn = new ArrayList<>();
        allBuiltIn.add(new Privilege(Privilege.READ));
        allBuiltIn.add(new Privilege(Privilege.COMMENT));
        allBuiltIn.add(new Privilege(Privilege.COMMENT_EDIT_ANY));
        allBuiltIn.add(new Privilege(Privilege.COMMENT_DELETE_ANY));
        allBuiltIn.add(new Privilege(Privilege.EDIT));
        allBuiltIn.add(new Privilege(Privilege.PUBLISH));
        allBuiltIn.add(new Privilege(Privilege.ADMIN));
        ALL_BUILT_IN = ImmutableList.copyOf(allBuiltIn);
    }

    @Override
    public Iterable<Privilege> getPrivileges() {
        return ALL_BUILT_IN;
    }
}
