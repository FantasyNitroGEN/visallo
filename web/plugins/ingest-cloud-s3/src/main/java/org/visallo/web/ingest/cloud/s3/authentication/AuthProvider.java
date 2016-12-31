package org.visallo.web.ingest.cloud.s3.authentication;

import com.amazonaws.auth.AWSCredentials;
import org.json.JSONObject;

public interface AuthProvider {

    public AWSCredentials getCredentials(JSONObject credentials);

}
