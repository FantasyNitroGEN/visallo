package org.visallo.web.clientapi;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class UserNameOnlyVisalloApi extends FormLoginVisalloApi {
    protected final String username;

    public UserNameOnlyVisalloApi(String basePath, String username) {
        this(basePath, username, false);
    }

    public UserNameOnlyVisalloApi(String basePath, String username, boolean ignoreSslErrors) {
        super(basePath, ignoreSslErrors);
        this.username = username;
        logIn();
    }

    @Override
    protected String getLoginFormBody() {
        try {
            return "username=" + URLEncoder.encode(username, "UTF-8");
        } catch (UnsupportedEncodingException uee) {
            throw new VisalloClientApiException("Failed to encode username", uee);
        }
    }
}
