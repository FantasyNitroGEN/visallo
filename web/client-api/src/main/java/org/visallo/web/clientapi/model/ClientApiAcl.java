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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ClientApiAcl that = (ClientApiAcl) o;

        if (addable != that.addable) {
            return false;
        }
        if (updateable != that.updateable) {
            return false;
        }
        if (deleteable != that.deleteable) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = (addable ? 1 : 0);
        result = 31 * result + (updateable ? 1 : 0);
        result = 31 * result + (deleteable ? 1 : 0);
        return result;
    }
}
