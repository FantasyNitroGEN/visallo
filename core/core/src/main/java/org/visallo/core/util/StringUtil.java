package org.visallo.core.util;

import java.util.regex.Pattern;

public class StringUtil {
    public static boolean containsOnWordBoundaryCaseInsensitive(String str, String contains) {
        Pattern regex = Pattern.compile(".*\\b" + Pattern.quote(contains) + "\\b.*", Pattern.CASE_INSENSITIVE);
        return regex.matcher(str).matches();
    }
}
