package org.visallo.web.clientapi;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class UserNameAndPasswordVisalloApi extends FormLoginVisalloApi {
    private final String username;
    private final String password;

    public UserNameAndPasswordVisalloApi(String basePath, String username, String password) {
        this(basePath, username, password, false);
    }

    public UserNameAndPasswordVisalloApi(String basePath, String username, String password, boolean ignoreSslErrors) {
        super(basePath, ignoreSslErrors);
        this.username = username;
        this.password = password;
        logIn();
    }

    @Override
    protected String getLoginFormBody() {
        try {
            return "username=" + URLEncoder.encode(username, "UTF-8") + "&" + "password=" + URLEncoder.encode(password, "UTF-8");
        } catch (UnsupportedEncodingException uee) {
            throw new VisalloClientApiException("Failed to encode username and/or password", uee);
        }
    }
}
