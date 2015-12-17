package org.visallo.core.model.directory;

import org.visallo.core.user.User;

import java.util.List;

public abstract class DirectoryRepository {
    public abstract List<String> searchPeople(String search, User user);

    public abstract List<String> searchGroups(String search, User user);
}
