package org.visallo.web.clientapi.util;

import java.util.Collection;

public class StringUtils {
    public static String join(Collection items) {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for (Object o : items) {
            if (!first) {
                result.append(",");
            }
            if (o != null) {
                result.append(o);
            }
            first = false;
        }
        return result.toString();
    }
}
