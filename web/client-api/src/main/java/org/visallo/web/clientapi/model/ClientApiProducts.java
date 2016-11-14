package org.visallo.web.clientapi.model;

import java.util.ArrayList;
import java.util.List;

public class ClientApiProducts implements ClientApiObject {
    public List<ClientApiProduct> products = new ArrayList<ClientApiProduct>();
    public List<String> types = new ArrayList<String>();

    public List<ClientApiProduct> getProducts() {
        return products;
    }
    public List<String> getTypes() { return types; }
}
