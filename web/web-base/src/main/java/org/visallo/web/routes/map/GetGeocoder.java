package org.visallo.web.routes.map;

import com.google.inject.Inject;
import org.visallo.core.config.Configuration;
import org.visallo.core.geocoding.GeocodeResult;
import org.visallo.core.geocoding.GeocoderRepository;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.web.BaseRequestHandler;
import org.visallo.web.clientapi.model.ClientApiMapGeocodeResponse;
import com.v5analytics.webster.HandlerChain;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

public class GetGeocoder extends BaseRequestHandler {
    private final GeocoderRepository geocoderRepository;

    @Inject
    public GetGeocoder(
            UserRepository userRepository,
            WorkspaceRepository workspaceRepository,
            Configuration configuration,
            GeocoderRepository geocoderRepository
    ) {
        super(userRepository, workspaceRepository, configuration);
        this.geocoderRepository = geocoderRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        String query = getRequiredParameter(request, "q");
        List<GeocodeResult> geocoderResults = this.geocoderRepository.find(query);
        ClientApiMapGeocodeResponse result = toClientApi(geocoderResults);
        respondWithClientApiObject(response, result);
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