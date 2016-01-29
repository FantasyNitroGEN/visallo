package org.visallo.web.routes.directory;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Required;
import org.visallo.core.exception.VisalloResourceNotFoundException;
import org.visallo.core.model.directory.DirectoryRepository;
import org.visallo.core.user.User;
import org.visallo.web.clientapi.model.DirectoryEntity;

public class DirectoryGet implements ParameterizedHandler {
    private final DirectoryRepository directoryRepository;

    @Inject
    public DirectoryGet(DirectoryRepository directoryRepository) {
        this.directoryRepository = directoryRepository;
    }

    @Handle
    public DirectoryEntity handle(
            @Required(name = "id", allowEmpty = false) String id,
            User user
    ) {
        DirectoryEntity directoryEntity = this.directoryRepository.findById(id, user);
        if (directoryEntity == null) {
            throw new VisalloResourceNotFoundException("Could not find directory entry with id: " + id);
        }

        return directoryEntity;
    }
}
