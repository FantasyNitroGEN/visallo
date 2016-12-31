package org.visallo.web.ingest.cloud.s3.authentication;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicSessionCredentials;
import org.json.JSONObject;

public class SessionAuthProvider implements AuthProvider {

    @Override
    public AWSCredentials getCredentials(JSONObject credentials) {
        String accessKey = credentials.optString("accessKey");
        String secret = credentials.optString("secret");
        String token = credentials.optString("token");
        return new BasicSessionCredentials(accessKey, secret, token);
    }

}
