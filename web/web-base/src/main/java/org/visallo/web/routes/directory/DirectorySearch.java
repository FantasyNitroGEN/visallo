package org.visallo.web.routes.directory;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Optional;
import com.v5analytics.webster.annotations.Required;
import org.visallo.core.model.directory.DirectoryRepository;
import org.visallo.core.user.User;
import org.visallo.web.clientapi.model.ClientApiDirectorySearchResponse;
import org.visallo.web.clientapi.model.DirectoryGroup;
import org.visallo.web.clientapi.model.DirectoryPerson;

import java.util.List;

public class DirectorySearch implements ParameterizedHandler {
    private final DirectoryRepository directoryRepository;

    @Inject
    public DirectorySearch(DirectoryRepository directoryRepository) {
        this.directoryRepository = directoryRepository;
    }

    @Handle
    public ClientApiDirectorySearchResponse handle(
            @Required(name = "search", allowEmpty = false) String search,
            @Optional(name = "people", defaultValue = "true") boolean searchPeople,
            @Optional(name = "groups", defaultValue = "true") boolean searchGroups,
            User user
    ) {
        ClientApiDirectorySearchResponse response = new ClientApiDirectorySearchResponse();

        if (searchPeople) {
            List<DirectoryPerson> people = this.directoryRepository.searchPeople(search, user);
            for (DirectoryPerson person : people) {
                response.getEntities().add(person);
            }
        }

        if (searchGroups) {
            List<DirectoryGroup> groups = this.directoryRepository.searchGroups(search, user);
            for (DirectoryGroup group : groups) {
                response.getEntities().add(group);
            }
        }

        return response;
    }
}
