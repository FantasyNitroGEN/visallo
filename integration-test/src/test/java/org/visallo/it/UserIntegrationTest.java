package org.visallo.it;

import org.visallo.web.clientapi.VisalloApi;
import org.visallo.web.clientapi.codegen.ApiException;
import org.visallo.web.clientapi.model.ClientApiUser;
import org.visallo.web.clientapi.model.ClientApiUsers;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class UserIntegrationTest extends TestBase {
    private String user1Id;
    private String user2Id;

    @Test
    public void testUsers() throws IOException, ApiException {
        createUsers();
        verifyGetAll();
        verifyGetByIds();
        verifyGetByUserName();
    }

    public void createUsers() throws ApiException {
        VisalloApi visalloApi = login(USERNAME_TEST_USER_1);
        user1Id = visalloApi.getUserApi().getMe().getId();
        visalloApi.logout();

        visalloApi = login(USERNAME_TEST_USER_2);
        user2Id = visalloApi.getUserApi().getMe().getId();
        visalloApi.logout();
    }

    private void verifyGetAll() throws ApiException {
        VisalloApi visalloApi = login(USERNAME_TEST_USER_1);

        ClientApiUsers users = visalloApi.getUserApi().getAll();
        assertEquals(2, users.getUsers().size());
        boolean foundUser1 = false;
        boolean foundUser2 = false;
        for (ClientApiUser user : users.getUsers()) {
            if (user.getUserName().equalsIgnoreCase(USERNAME_TEST_USER_1)) {
                foundUser1 = true;
            } else if (user.getUserName().equalsIgnoreCase(USERNAME_TEST_USER_2)) {
                foundUser2 = true;
            } else {
                throw new RuntimeException("Invalid user: " + user);
            }
        }
        assertTrue("Could not find " + USERNAME_TEST_USER_1, foundUser1);
        assertTrue("Could not find " + USERNAME_TEST_USER_2, foundUser2);

        visalloApi.logout();
    }

    private void verifyGetByIds() throws ApiException {
        VisalloApi visalloApi = login(USERNAME_TEST_USER_1);

        List<String> ids = new ArrayList<>();
        ids.add(user1Id);
        ids.add(user2Id);
        ClientApiUsers users = visalloApi.getUserApi().getManyByIds(ids);
        assertEquals(2, users.getUsers().size());
        boolean foundUser1 = false;
        boolean foundUser2 = false;
        for (ClientApiUser user : users.getUsers()) {
            if (user.getUserName().equalsIgnoreCase(USERNAME_TEST_USER_1)) {
                foundUser1 = true;
            } else if (user.getUserName().equalsIgnoreCase(USERNAME_TEST_USER_2)) {
                foundUser2 = true;
            } else {
                throw new RuntimeException("Invalid user: " + user);
            }
        }
        assertTrue("Could not find " + USERNAME_TEST_USER_1, foundUser1);
        assertTrue("Could not find " + USERNAME_TEST_USER_2, foundUser2);

        visalloApi.logout();
    }

    private void verifyGetByUserName() throws ApiException {
        VisalloApi visalloApi = login(USERNAME_TEST_USER_1);

        ClientApiUser user = visalloApi.getUserApi().getByUserName(USERNAME_TEST_USER_1);
        assertEquals(user1Id, user.getId());
        assertEquals(1, user.getWorkspaces().size());

        visalloApi.logout();
    }
}
