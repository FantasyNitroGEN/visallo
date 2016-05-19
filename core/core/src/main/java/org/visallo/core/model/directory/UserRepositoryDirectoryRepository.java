package org.visallo.core.model.directory;

import com.google.inject.Inject;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.user.User;
import org.visallo.web.clientapi.model.DirectoryEntity;
import org.visallo.web.clientapi.model.DirectoryGroup;
import org.visallo.web.clientapi.model.DirectoryPerson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UserRepositoryDirectoryRepository extends DirectoryRepository {
    private final UserRepository userRepository;

    @Inject
    public UserRepositoryDirectoryRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public List<DirectoryPerson> searchPeople(String search, User user) {
        List<DirectoryPerson> results = new ArrayList<>();
        Iterable<User> users = userRepository.find(search);
        for (User u : users) {
            results.add(userToDirectoryPerson(u));
        }
        return results;
    }

    private DirectoryPerson userToDirectoryPerson(User u) {
        if (u == null) {
            return null;
        }
        return new DirectoryPerson(u.getUserId(), u.getUsername());
    }

    @Override
    public List<DirectoryGroup> searchGroups(String search, User user) {
        return Collections.emptyList();
    }

    @Override
    public DirectoryEntity findById(String id, User user) {
        return userToDirectoryPerson(userRepository.findById(id));
    }

    @Override
    public String getDirectoryEntityId(User user) {
        return user.getUserId();
    }

    @Override
    public List<DirectoryPerson> findAllPeopleInGroup(DirectoryGroup group) {
        return Collections.emptyList();
    }
}
