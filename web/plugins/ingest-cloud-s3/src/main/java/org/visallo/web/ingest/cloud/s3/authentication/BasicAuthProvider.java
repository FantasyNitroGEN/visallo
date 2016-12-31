package org.visallo.web.ingest.cloud.s3.authentication;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import org.json.JSONObject;

public class BasicAuthProvider implements AuthProvider {

    @Override
    public AWSCredentials getCredentials(JSONObject credentials) {
        String accessKey = credentials.optString("accessKey");
        String secret = credentials.optString("secret");
        return new BasicAWSCredentials(accessKey, secret);
    }
}
