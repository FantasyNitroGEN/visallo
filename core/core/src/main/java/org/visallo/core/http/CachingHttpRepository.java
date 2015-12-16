package org.visallo.core.http;

import com.google.inject.Inject;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.visallo.core.config.Configuration;
import org.visallo.core.config.FileConfigurationLoader;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class CachingHttpRepository extends HttpRepository {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(CachingHttpRepository.class);
    private static final String CONFIG_CACHE_DIR = "cachingHttp.cacheDir";
    private static final String INDEX_FILE_NAME = "index";
    private File cacheDir;

    @Inject
    public CachingHttpRepository(Configuration configuration) {
        super(configuration);
        String cacheDirString = configuration.get(CONFIG_CACHE_DIR, getDefaultHttpCacheDir());
        cacheDir = new File(cacheDirString);
        if (!cacheDir.exists()) {
            if (!cacheDir.mkdirs()) {
                throw new VisalloException("Could not make directory: " + cacheDir.getAbsolutePath());
            }
        }
        LOGGER.info("Using cache dir: %s", cacheDir.getAbsolutePath());
    }

    private String getDefaultHttpCacheDir() {
        File visalloDir = new File(FileConfigurationLoader.getDefaultVisalloDir());
        return new File(visalloDir, "httpCache").getAbsolutePath();
    }

    @Override
    public byte[] get(final String url) {
        String cacheMd5 = DigestUtils.md5Hex(url);
        return withCache(url, cacheMd5, new WithCache() {
            @Override
            public byte[] doIt() {
                return CachingHttpRepository.super.get(url);
            }
        });
    }

    @Override
    public byte[] post(final String url, final List<Parameter> formParameters) {
        String cacheMd5 = DigestUtils.md5Hex(url + createQueryString(formParameters));
        return withCache(url, cacheMd5, new WithCache() {
            @Override
            public byte[] doIt() {
                return CachingHttpRepository.super.post(url, formParameters);
            }
        });
    }

    private byte[] withCache(String url, String cacheMd5, WithCache withCache) {
        File indexFile = new File(cacheDir, INDEX_FILE_NAME);
        File cachedFile = new File(cacheDir, cacheMd5);
        try {
            if (cachedFile.exists()) {
                LOGGER.debug("cache hit: %s: %s", url, cachedFile.getAbsolutePath());
                return FileUtils.readFileToByteArray(cachedFile);
            }
            LOGGER.debug("cache miss: %s: %s", url, cachedFile.getAbsolutePath());
            byte[] data = withCache.doIt();
            FileUtils.writeByteArrayToFile(cachedFile, data);
            FileUtils.writeStringToFile(indexFile, cacheMd5 + " " + url + "\n", true);
            return data;
        } catch (IOException e) {
            throw new VisalloException("Could not read cache file: " + cachedFile.getAbsolutePath(), e);
        }
    }

    private interface WithCache {
        byte[] doIt();
    }
}
