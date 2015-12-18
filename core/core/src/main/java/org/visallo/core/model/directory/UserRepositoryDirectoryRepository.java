package org.visallo.core.model.directory;

import com.google.inject.Inject;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.user.User;

import java.util.ArrayList;
import java.util.List;

public class UserRepositoryDirectoryRepository extends DirectoryRepository {
    private final UserRepository userRepository;

    @Inject
    public UserRepositoryDirectoryRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public List<String> searchPeople(String search, User user) {
        List<String> results = new ArrayList<>();
        Iterable<User> users = userRepository.find(search);
        for (User u : users) {
            results.add(u.getUsername());
        }
        return results;
    }

    @Override
    public List<String> searchGroups(String search, User user) {
        return new ArrayList<>();
    }
}
