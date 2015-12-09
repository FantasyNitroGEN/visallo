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
}
