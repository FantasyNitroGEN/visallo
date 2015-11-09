package org.visallo.web.routes.map;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Required;
import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.http.HttpRepository;
import org.visallo.web.VisalloResponse;

public class MapzenTileProxy implements ParameterizedHandler {
    private static final String MAPZEN_TILE_SERVER = "http://vector.mapzen.com/";
    private final LoadingCache<String, byte[]> cache;

    @Inject
    public MapzenTileProxy(
            Configuration configuration,
            final HttpRepository httpRepository
    ) {
        final String key = configuration.get(Configuration.MAPZEN_TILE_API_KEY, null);

        cache = CacheBuilder.newBuilder()
                .maximumSize(100)
                .build(
                        new CacheLoader<String, byte[]>() {
                            public byte[] load(String uri) throws Exception {
                                if (key == null) {
                                    throw new VisalloException("MapZen api key not found: " + Configuration.MAPZEN_TILE_API_KEY);
                                }
                                return httpRepository.get(MAPZEN_TILE_SERVER + uri + "?api_key=" + key);
                            }
                        });
    }

    @Handle
    public void handle(
            @Required(name = "mapzenUri") String uri,
            VisalloResponse response
    ) throws Exception {
        byte[] data = cache.get(uri);
        response.setContentLength(data.length);
        response.setMaxAge(60 * 60 * 24 * 365);
        response.getOutputStream().write(data);
    }
}