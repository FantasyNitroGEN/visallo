package org.visallo.geocoder.bing;

import com.google.inject.Inject;
import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.geocoding.GeocodeResult;
import org.visallo.core.geocoding.GeocoderRepository;
import org.visallo.core.http.HttpRepository;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.vertexium.ElementType;
import org.vertexium.Visibility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BingGeocoderRepository extends GeocoderRepository {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(BingGeocoderRepository.class);
    private static final String CONFIG_KEY = "geocoder.bing.key";
    private final HttpRepository httpRepository;
    private final BingExternalResourceWorker bingExternalResourceWorker;
    private String key;

    @Inject
    public BingGeocoderRepository(
            Configuration configuration,
            HttpRepository httpRepository,
            BingExternalResourceWorker bingExternalResourceWorker
    ) {
        this.httpRepository = httpRepository;
        this.bingExternalResourceWorker = bingExternalResourceWorker;
        key = configuration.get(CONFIG_KEY, null);
        if (key == null) {
            LOGGER.error("Could not find bing geocoder configuration key: " + CONFIG_KEY);
        }
    }

    @Override
    public List<GeocodeResult> find(String query) {
        try {
            LOGGER.debug("Geocoding: %s", query);
            Map<String, String> parameters = new HashMap<>();
            parameters.put("query", query);
            parameters.put("output", "json");
            parameters.put("key", key);
            String responseText = new String(httpRepository.get("http://dev.virtualearth.net/REST/v1/Locations", parameters));
            JSONObject responseJson = new JSONObject(responseText);
            return toGeocodeResults(responseJson);
        } catch (Exception ex) {
            throw new VisalloException("Could not geocode: " + query, ex);
        }
    }

    @Override
    public void queuePropertySet(String locationString, ElementType elementType, String elementId, String propertyKey, String propertyName, Visibility visibility, Priority priority) {
        this.bingExternalResourceWorker.queuePropertySet(locationString, elementType, elementId, propertyKey, propertyName, visibility, priority);
    }

    private List<GeocodeResult> toGeocodeResults(JSONObject responseJson) {
        List<GeocodeResult> results = new ArrayList<>();

        JSONArray resourceSets = responseJson.getJSONArray("resourceSets");
        for (int resourceSetIndex = 0; resourceSetIndex < resourceSets.length(); resourceSetIndex++) {
            JSONObject resourceSet = resourceSets.getJSONObject(resourceSetIndex);
            JSONArray resources = resourceSet.getJSONArray("resources");
            for (int resourceIndex = 0; resourceIndex < resources.length(); resourceIndex++) {
                JSONObject resource = resources.getJSONObject(resourceIndex);
                results.add(bingResourceToGeocodeResult(resource));
            }
        }

        return results;
    }

    private GeocodeResult bingResourceToGeocodeResult(JSONObject resource) {
        JSONArray coordinates = resource.getJSONObject("point").getJSONArray("coordinates");
        String name = resource.getString("name");
        double latitude = coordinates.getDouble(0);
        double longitude = coordinates.getDouble(1);
        return new GeocodeResult(name, latitude, longitude);
    }
}
