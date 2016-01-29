package org.visallo.web.clientapi.model;

import java.util.ArrayList;
import java.util.List;

public class ClientApiDirectorySearchResponse implements ClientApiObject {
    private List<DirectoryEntity> entities = new ArrayList<DirectoryEntity>();

    public List<DirectoryEntity> getEntities() {
        return entities;
    }
}
