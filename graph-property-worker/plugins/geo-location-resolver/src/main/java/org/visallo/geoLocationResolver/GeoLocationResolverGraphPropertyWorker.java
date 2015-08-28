package org.visallo.geoLocationResolver;

import com.google.inject.Inject;
import org.vertexium.Element;
import org.vertexium.ElementType;
import org.vertexium.Property;
import org.visallo.core.geocoding.GeocoderRepository;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorkData;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorker;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorkerPrepareData;
import org.visallo.core.model.Description;
import org.visallo.core.model.Name;
import org.visallo.core.model.properties.types.GeoPointVisalloProperty;
import org.visallo.core.model.properties.types.StringVisalloProperty;

import java.io.InputStream;

@Name("Geo Location Resolver")
@Description("Geo-locate Location properties")
public class GeoLocationResolverGraphPropertyWorker extends GraphPropertyWorker {
    public static final String GEO_LOCATION_INTENT = "geoLocation";
    public static final String LOCATION_INTENT = "location";
    
    private GeoPointVisalloProperty geoLocationProperty;
    private StringVisalloProperty locationProperty;
    private GeocoderRepository geocoderRepository;

    @Override
    public void prepare(GraphPropertyWorkerPrepareData workerPrepareData) throws Exception {
        super.prepare(workerPrepareData);

        geoLocationProperty = getOntologyRepository().getRequiredVisalloPropertyByIntent(GEO_LOCATION_INTENT, GeoPointVisalloProperty.class);
        locationProperty = getOntologyRepository().getRequiredVisalloPropertyByIntent(LOCATION_INTENT, StringVisalloProperty.class);
    }

    @Override
    public void execute(InputStream in, GraphPropertyWorkData data) throws Exception {
        String locationValue = locationProperty.getPropertyValue(data.getProperty());
        geocoderRepository.queuePropertySet(
                locationValue,
                ElementType.VERTEX,
                data.getElement().getId(),
                data.getProperty().getKey(),
                geoLocationProperty.getPropertyName(),
                data.getProperty().getVisibility(),
                data.getPriority()
        );
    }

    @Override
    public boolean isHandled(Element element, Property property) {
        if (property == null) {
            return false;
        }

        if (!property.getName().equals(locationProperty.getPropertyName())) {
            return false;
        }

        if (geoLocationProperty.hasProperty(element)) {
            return false;
        }

        return true;
    }

    @Inject
    public void setGeocoderRepository(GeocoderRepository geocoderRepository) {
        this.geocoderRepository = geocoderRepository;
    }
}
