package twitter4j;

import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

/**
 * Allow access to the package private construction of twitter4j.Status objects.
 */
public class StatusFactory {
    public static Status createStatus(String json) throws JSONException, TwitterException {
        // Create a config object that indicates that the data will be stored as JSON
        final Configuration config = new ConfigurationBuilder().setJSONStoreEnabled(true).build();
        JSONObject jsonObject = new JSONObject(json);
        return new StatusJSONImpl(jsonObject, config);
    }
}
