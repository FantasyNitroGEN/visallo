package org.visallo.web.routes.map;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Required;
import org.visallo.core.geocoding.GeocodeResult;
import org.visallo.core.geocoding.GeocoderRepository;
import org.visallo.web.VisalloResponse;
import org.visallo.web.clientapi.model.ClientApiMapGeocodeResponse;

import java.util.List;

public class GetGeocoder implements ParameterizedHandler {
    private final GeocoderRepository geocoderRepository;

    @Inject
    public GetGeocoder(GeocoderRepository geocoderRepository) {
        this.geocoderRepository = geocoderRepository;
    }

    @Handle
    public void handle(
            @Required(name = "q") String query,
            VisalloResponse response
    ) throws Exception {
        List<GeocodeResult> geocoderResults = this.geocoderRepository.find(query);
        ClientApiMapGeocodeResponse result = toClientApi(geocoderResults);
        response.respondWithClientApiObject(result);
    }

    private ClientApiMapGeocodeResponse toClientApi(List<GeocodeResult> geocoderResults) {
        ClientApiMapGeocodeResponse result = new ClientApiMapGeocodeResponse();

        for (GeocodeResult geocodeResult : geocoderResults) {
            result.results.add(toClientApi(geocodeResult));
        }

        return result;
    }

    private ClientApiMapGeocodeResponse.Result toClientApi(GeocodeResult geocodeResult) {
        String name = geocodeResult.getName();
        double latitude = geocodeResult.getLatitude();
        double longitude = geocodeResult.getLongitude();
        return new ClientApiMapGeocodeResponse.Result(name, latitude, longitude);
    }
}