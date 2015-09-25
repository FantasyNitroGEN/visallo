package org.visallo.core.util;

import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

public class StringArrayUtil {
    public static String[] removeNullOrEmptyElements(String[] elements) {
        return Iterables.toArray(Iterables.filter(Lists.newArrayList(elements), new Predicate<String>() {
            @Override
            public boolean apply(String value) {
                return !Strings.isNullOrEmpty(value);
            }
        }), String.class);
    }
}
