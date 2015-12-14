package org.visallo.web.routes.userGuide;

import com.google.common.collect.Multimap;
import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.vfs.Vfs;

import java.util.Set;

public class ClasspathResourceUtils {

    public static Set<String> findClasspathResources(ClassLoader classLoader, String match) {
        Reflections reflections = new Reflections(
                new ConfigurationBuilder()
                        .setScanners(new ResourcesScanner(match))
                        .setUrls(ClasspathHelper.forClassLoader(classLoader))
        );
        Multimap<String, String> store = reflections.getStore().get(ResourcesScanner.class.getSimpleName());
        return store.keySet();
    }

    private static class ResourcesScanner extends org.reflections.scanners.ResourcesScanner {
        String match;

        public ResourcesScanner(String match) {
            this.match = match;
        }

        @Override
        public boolean acceptsInput(String file) {
            return file.contains(match);
        }

        @Override
        public Object scan(Vfs.File file, Object classObject) {
            if (file.getRelativePath().contains(match)) {
                getStore().put(file.getRelativePath(), file.getRelativePath());
            }
            return classObject;
        }
    }
}
