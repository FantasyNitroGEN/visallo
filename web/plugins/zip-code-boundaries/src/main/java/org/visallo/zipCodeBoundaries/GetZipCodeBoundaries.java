package org.visallo.zipCodeBoundaries;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Optional;
import org.vertexium.type.GeoPoint;
import org.vertexium.type.GeoRect;
import org.visallo.web.BadRequestException;
import org.visallo.web.clientapi.model.ClientApiObject;

import java.util.List;

public class GetZipCodeBoundaries implements ParameterizedHandler {
    private final ZipCodeBoundariesRepository zipCodeBoundariesRepository;

    @Inject
    public GetZipCodeBoundaries(ZipCodeBoundariesRepository zipCodeBoundariesRepository) {
        this.zipCodeBoundariesRepository = zipCodeBoundariesRepository;
    }

    @Handle
    public ClientApiObject handle(
            @Optional(name = "zipCode") String zipCode,
            @Optional(name = "zipCode[]") String[] zipCodes,
            @Optional(name = "northWest") String northWestString,
            @Optional(name = "southEast") String southEastString
    ) throws Exception {
        if (zipCode != null) {
            zipCodes = new String[]{zipCode};
        }
        if (zipCodes != null) {
            List<Features.Feature> features = this.zipCodeBoundariesRepository.findZipCodes(zipCodes);
            if (!features.isEmpty()) {
                return features.size() == 1 ? features.get(0) : new Features(features);
            }
        }

        if (northWestString != null && southEastString != null) {
            GeoPoint northWest = GeoPoint.parse(northWestString);
            GeoPoint southEast = GeoPoint.parse(southEastString);
            GeoRect rect = new GeoRect(northWest, southEast);

            List<Features.Feature> features = this.zipCodeBoundariesRepository.find(rect);
            return new Features(features);
        }

        throw new BadRequestException("zipCode, zipCodes[], or northWest and southEast parameters are required");
    }
}
