package org.visallo.core.bootstrap;

import com.google.common.base.Strings;
import com.google.inject.Binder;
import com.google.inject.Scopes;
import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.util.ClassUtil;

public class BinderHelper {
    public static <T> void bind(
            Binder binder,
            Configuration configuration,
            String propertyKey,
            Class<T> type,
            Class<? extends T> defaultClass
    ) {
        String className = configuration.get(propertyKey, defaultClass == null ? null : defaultClass.getName());
        if (Strings.isNullOrEmpty(className)) {
            throw new VisalloException("Missing configuration: " + propertyKey);
        }
        try {
            Class<? extends T> klass = ClassUtil.forName(className);
            binder.bind(type).to(klass).in(Scopes.SINGLETON);
        } catch (Exception ex) {
            throw new VisalloException(
                    "Failed to bind " + className + " as singleton instance of " + type.getName() + "(configure with " + propertyKey + ")",
                    ex
            );
        }
    }

    public static <T> void bind(Binder binder, Configuration configuration, String propertyKey, Class<T> type) {
        bind(binder, configuration, propertyKey, type, null);
    }
}
