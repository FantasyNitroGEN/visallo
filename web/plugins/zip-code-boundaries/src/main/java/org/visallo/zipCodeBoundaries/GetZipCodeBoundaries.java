package org.visallo.zipCodeBoundaries;

import com.google.inject.Inject;
import com.v5analytics.webster.HandlerChain;
import org.vertexium.type.GeoPoint;
import org.vertexium.type.GeoRect;
import org.visallo.core.config.Configuration;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.web.BaseRequestHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

public class GetZipCodeBoundaries extends BaseRequestHandler {
    private final ZipCodeBoundariesRepository zipCodeBoundariesRepository;

    @Inject
    public GetZipCodeBoundaries(
            UserRepository userRepository,
            WorkspaceRepository workspaceRepository,
            Configuration configuration,
            ZipCodeBoundariesRepository zipCodeBoundariesRepository
    ) {
        super(userRepository, workspaceRepository, configuration);
        this.zipCodeBoundariesRepository = zipCodeBoundariesRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        String zipCode = getOptionalParameter(request, "zipCode");
        if (zipCode != null) {
            Features.Feature feature = this.zipCodeBoundariesRepository.findZipCode(zipCode);
            respondWithClientApiObject(response, feature);
            return;
        }

        GeoPoint northWest = GeoPoint.parse(getRequiredParameter(request, "northWest"));
        GeoPoint southEast = GeoPoint.parse(getRequiredParameter(request, "southEast"));
        GeoRect rect = new GeoRect(northWest, southEast);

        List<Features.Feature> features = this.zipCodeBoundariesRepository.find(rect);
        respondWithClientApiObject(response, new Features(features));
    }
}
