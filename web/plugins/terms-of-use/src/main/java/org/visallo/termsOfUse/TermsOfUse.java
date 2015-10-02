package org.visallo.termsOfUse;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Required;
import org.apache.commons.codec.binary.Hex;
import org.json.JSONObject;
import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.user.User;

import javax.servlet.http.HttpServletRequest;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TermsOfUse implements ParameterizedHandler {
    public static final String TITLE_PROPERTY = "termsOfUse.title";
    public static final String DEFAULT_TITLE = "Terms of Use";
    public static final String HTML_PROPERTY = "termsOfUse.html";
    public static final String DEFAULT_HTML = "<p>These are the terms to which you must agree before using Visallo:</p><p>With great power comes great responsibility. Please use Visallo responsibly.</p>";
    public static final String DATE_PROPERTY = "termsOfUse.date";
    public static final String DATE_PROPERTY_FORMAT = "yyyy-MM-dd";
    private static final String UI_PREFERENCE_KEY = "termsOfUse";
    private static final String UI_PREFERENCE_HASH_SUBKEY = "hash";
    private static final String UI_PREFERENCE_DATE_SUBKEY = "date";
    private JSONObject termsJson;
    private String termsHash;
    private UserRepository userRepository;

    @Inject
    protected TermsOfUse(Configuration configuration,
                         UserRepository userRepository) {
        this.userRepository = userRepository;
        String title = configuration.get(TITLE_PROPERTY, DEFAULT_TITLE);
        String html = configuration.get(HTML_PROPERTY, DEFAULT_HTML);
        termsHash = hash(html);
        Date date = null;
        String dateString = configuration.get(DATE_PROPERTY, null);
        if (dateString != null) {
            SimpleDateFormat sdf = new SimpleDateFormat(DATE_PROPERTY_FORMAT);
            try {
                date = sdf.parse(dateString);
            } catch (ParseException e) {
                throw new VisalloException("unable to parse " + DATE_PROPERTY + " property with format " + DATE_PROPERTY_FORMAT, e);
            }
        }

        termsJson = new JSONObject();
        termsJson.put("title", title);
        termsJson.put("html", html);
        termsJson.put("hash", termsHash);
        if (date != null) {
            termsJson.put("date", date);
        }
    }

    @Handle
    public JSONObject handle(
            @Required(name = "hash") String hash,
            HttpServletRequest request,
            User user
    ) throws Exception {
        if (request.getMethod().equals("POST")) {
            recordAcceptance(user, hash);
            JSONObject successJson = new JSONObject();
            successJson.put("success", true);
            successJson.put("message", "Terms of Use accepted.");
            return successJson;
        }
        JSONObject termsAndStatus = new JSONObject();
        termsAndStatus.put("terms", termsJson);
        termsAndStatus.put("status", getStatus(user));
        return termsAndStatus;
    }

    private String hash(String s) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] md5 = digest.digest(s.getBytes());
            return Hex.encodeHexString(md5);
        } catch (NoSuchAlgorithmException e) {
            throw new VisalloException("Could not find MD5", e);
        }
    }

    private JSONObject getUiPreferences(User user) {
        JSONObject uiPreferences = user.getUiPreferences();
        return uiPreferences != null ? uiPreferences : new JSONObject();
    }

    private void recordAcceptance(User user, String hash) {
        JSONObject uiPreferences = getUiPreferences(user);

        JSONObject touJson = new JSONObject();
        touJson.put(UI_PREFERENCE_HASH_SUBKEY, hash);
        touJson.put(UI_PREFERENCE_DATE_SUBKEY, new Date());
        uiPreferences.put(UI_PREFERENCE_KEY, touJson);
        userRepository.setUiPreferences(user, uiPreferences);
    }

    private JSONObject getStatus(User user) {
        JSONObject uiPreferences = getUiPreferences(user);
        JSONObject touJson = uiPreferences.optJSONObject(UI_PREFERENCE_KEY);

        JSONObject statusJson = new JSONObject();
        statusJson.put("current", false);

        if (touJson != null) {
            if (touJson.getString(UI_PREFERENCE_HASH_SUBKEY).equals(termsHash)) {
                statusJson.put("current", true);
                statusJson.put("accepted", touJson.getString(UI_PREFERENCE_DATE_SUBKEY));
            }
        }

        return statusJson;
    }
}
