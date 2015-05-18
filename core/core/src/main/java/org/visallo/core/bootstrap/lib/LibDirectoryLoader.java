package org.visallo.core.bootstrap.lib;

import org.visallo.core.config.Configuration;
import org.visallo.core.config.FileConfigurationLoader;
import org.visallo.core.model.Description;
import org.visallo.core.model.Name;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

import java.io.File;
import java.util.List;

@Name("lib Directory")
@Description("Loads LIBs from a local file system directory")
public class LibDirectoryLoader extends LibLoader {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(LibDirectoryLoader.class);

    @Override
    public void loadLibs(Configuration configuration) {
        LOGGER.info("Loading libs using %s", LibDirectoryLoader.class.getName());
        List<File> libDirectories = FileConfigurationLoader.getVisalloDirectoriesFromMostPriority("lib");
        for (File libDirectory : libDirectories) {
            addLibDirectory(libDirectory);
        }
    }
}
