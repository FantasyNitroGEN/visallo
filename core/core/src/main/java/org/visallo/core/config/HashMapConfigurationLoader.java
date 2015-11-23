package org.visallo.core.config;

import java.io.File;
import java.util.Map;

public class HashMapConfigurationLoader extends ConfigurationLoader {
    public HashMapConfigurationLoader(Map initParameters) {
        super(initParameters);
    }

    @Override
    public Configuration createConfiguration() {
        return new Configuration(this, getInitParameters());
    }

    @Override
    public File resolveFileName(String fileName) {
        return FileConfigurationLoader.resolveLocalFileName(fileName);
    }
}
