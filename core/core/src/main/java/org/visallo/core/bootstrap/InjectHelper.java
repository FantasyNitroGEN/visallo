package org.visallo.core.bootstrap;

import com.fasterxml.jackson.module.guice.ObjectMapperModule;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.ProvisionException;
import org.visallo.core.bootstrap.lib.LibLoader;
import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.util.ServiceLoaderUtil;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.vertexium.util.IterableUtils.toList;

public class InjectHelper {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(InjectHelper.class);
    private static Injector injector;

    public static <T> T inject(T o, ModuleMaker moduleMaker, Configuration configuration) {
        ensureInjectorCreated(moduleMaker, configuration);
        inject(o);
        return o;
    }

    public static <T> T inject(T o) {
        if (injector == null) {
            throw new VisalloException("Could not find injector");
        }
        injector.injectMembers(o);
        return o;
    }

    public static Injector getInjector() {
        return injector;
    }

    public static <T> T getInstance(Class<T> clazz, ModuleMaker moduleMaker, Configuration configuration) {
        ensureInjectorCreated(moduleMaker, configuration);
        return injector.getInstance(clazz);
    }

    public static <T> T getInstance(Class<? extends T> clazz) {
        LOGGER.debug("getInstance of class: " + clazz.getName());
        if (injector == null) {
            throw new VisalloException("Could not find injector");
        }
        try {
            return injector.getInstance(clazz);
        } catch (ProvisionException ex) {
            LOGGER.error("Could not create class: %s: %s", clazz.getName(), ex.getMessage(), ex);
            throw new VisalloException("Could not create class: " + clazz.getName(), ex);
        } catch (Throwable ex) {
            throw new VisalloException("Could not create class: " + clazz.getName(), ex);
        }
    }

    public static <T> Collection<T> getInjectedServices(Class<T> clazz, Configuration configuration) {
        List<Class<? extends T>> serviceClasses = toList(ServiceLoaderUtil.loadClasses(clazz, configuration));
        Collection<T> results = new ArrayList<>();
        for (Class<? extends T> serviceClass : serviceClasses) {
            results.add(getInstance(serviceClass));
        }
        return results;
    }

    public static void shutdown() {
        injector = null;
    }

    public static boolean hasInjector() {
        return injector != null;
    }

    @VisibleForTesting
    public static void setInjector(Injector injector) {
        InjectHelper.injector = injector;
    }

    public interface ModuleMaker {
        Module createModule();

        Configuration getConfiguration();
    }

    private static void ensureInjectorCreated(ModuleMaker moduleMaker, Configuration configuration) {
        if (injector == null) {
            LOGGER.info("Loading libs...");
            for (LibLoader libLoader : ServiceLoaderUtil.loadWithoutInjecting(LibLoader.class, configuration)) {
                libLoader.loadLibs(moduleMaker.getConfiguration());
            }
            injector = Guice.createInjector(moduleMaker.createModule(), new ObjectMapperModule());
        }
    }
}
