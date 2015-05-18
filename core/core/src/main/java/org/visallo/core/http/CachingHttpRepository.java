package org.visallo.core.http;

import com.google.inject.Inject;
import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public class CachingHttpRepository extends HttpRepository {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(CachingHttpRepository.class);
    private static final String CONFIG_CACHE_DIR = "cachingHttp.cacheDir";
    private static final String INDEX_FILE_NAME = "index";
    private File cacheDir;

    @Inject
    public CachingHttpRepository(Configuration configuration) {
        super(configuration);
        String cacheDirString = configuration.get(CONFIG_CACHE_DIR, "/opt/visallo/httpCache");
        cacheDir = new File(cacheDirString);
        if (!cacheDir.exists()) {
            if (!cacheDir.mkdirs()) {
                throw new VisalloException("Could not make directory: " + cacheDir.getAbsolutePath());
            }
        }
        LOGGER.info("Using cache dir: %s", cacheDir.getAbsolutePath());
    }

    @Override
    public byte[] get(String url) {
        String urlMd5 = DigestUtils.md5Hex(url);
        File indexFile = new File(cacheDir, INDEX_FILE_NAME);
        File cachedFile = new File(cacheDir, urlMd5);
        try {
            if (cachedFile.exists()) {
                LOGGER.debug("cache hit: %s: %s", url, cachedFile.getAbsolutePath());
                return FileUtils.readFileToByteArray(cachedFile);
            }
            LOGGER.debug("cache miss: %s: %s", url, cachedFile.getAbsolutePath());
            byte[] data = super.get(url);
            FileUtils.writeByteArrayToFile(cachedFile, data);
            FileUtils.writeStringToFile(indexFile, urlMd5 + " " + url + "\n", true);
            return data;
        } catch (IOException e) {
            throw new VisalloException("Could not read cache file: " + cachedFile.getAbsolutePath(), e);
        }
    }
}
