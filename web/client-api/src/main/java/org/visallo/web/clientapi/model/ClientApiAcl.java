package org.visallo.web.clientapi.model;

import org.visallo.web.clientapi.util.ClientApiConverter;

public abstract class ClientApiAcl implements ClientApiObject {
    private boolean addable;
    private boolean updateable;
    private boolean deleteable;

    public boolean isAddable() {
        return addable;
    }

    public void setAddable(boolean addable) {
        this.addable = addable;
    }

    public boolean isUpdateable() {
        return updateable;
    }

    public void setUpdateable(boolean updateable) {
        this.updateable = updateable;
    }

    public boolean isDeleteable() {
        return deleteable;
    }

    public void setDeleteable(boolean deleteable) {
        this.deleteable = deleteable;
    }

    @Override
    public String toString() {
        return ClientApiConverter.clientApiToString(this);
    }
}
