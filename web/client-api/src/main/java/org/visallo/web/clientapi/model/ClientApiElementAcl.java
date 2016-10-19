package org.visallo.web.clientapi.model;

import java.util.ArrayList;
import java.util.List;

public class ClientApiElementAcl extends ClientApiAcl {
    private List<ClientApiPropertyAcl> propertyAcls = new ArrayList<ClientApiPropertyAcl>();

    public List<ClientApiPropertyAcl> getPropertyAcls() {
        return propertyAcls;
    }

    public void setPropertyAcls(List<ClientApiPropertyAcl> propertyAcls) {
        this.propertyAcls = propertyAcls;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        ClientApiElementAcl that = (ClientApiElementAcl) o;

        if (this.propertyAcls.size() != that.propertyAcls.size()) {
            return false;
        }

        for (int i = 0; i < this.propertyAcls.size(); i++) {
            ClientApiPropertyAcl thisPropertyAcl = this.propertyAcls.get(i);
            ClientApiPropertyAcl thatPropertyAcl = that.propertyAcls.get(i);
            if (!thisPropertyAcl.equals(thatPropertyAcl)) {
                return false;
            }
        }


        return true;
    }
}
