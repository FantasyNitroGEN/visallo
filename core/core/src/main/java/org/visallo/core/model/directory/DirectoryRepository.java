package org.visallo.core.model.directory;

import org.visallo.core.user.User;
import org.visallo.web.clientapi.model.DirectoryEntity;
import org.visallo.web.clientapi.model.DirectoryGroup;
import org.visallo.web.clientapi.model.DirectoryPerson;

import java.util.List;

public abstract class DirectoryRepository {
    public abstract List<DirectoryPerson> searchPeople(String search, User user);

    public abstract List<DirectoryGroup> searchGroups(String search, User user);

    public abstract DirectoryEntity findById(String id, User user);

    public abstract String getDirectoryEntityId(User user);

    public abstract List<DirectoryPerson> findAllPeopleInGroup(DirectoryGroup group);
}
