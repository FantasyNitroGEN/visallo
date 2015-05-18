package org.visallo.zipCodeResolver;

import com.google.inject.Inject;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorkData;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorker;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorkerPrepareData;
import org.visallo.core.model.Description;
import org.visallo.core.model.Name;
import org.visallo.core.model.properties.types.GeoPointVisalloProperty;
import org.visallo.core.model.properties.types.StringVisalloProperty;
import org.vertexium.Element;
import org.vertexium.Metadata;
import org.vertexium.Property;
import org.vertexium.type.GeoPoint;

import java.io.InputStream;
import java.util.Set;

import static org.vertexium.util.IterableUtils.toSet;

@Name("ZipCode Resolver")
@Description("Resolves ZipCodes")
public class ZipCodeLocationGraphPropertyWorker extends GraphPropertyWorker {
    public static final String CONFIG_PROPERTY_NAME_PREFIX = "zipCodeLocation.propertyName";
    public static final String GEO_LOCATION_INTENT = "geoLocation";
    public static final String ZIP_CODE_INTENT = "zipCode";
    private Set<String> propertyNames;
    private ZipCodeRepository zipCodeRepository;
    private GeoPointVisalloProperty geoLocationProperty;

    @Override
    public void prepare(GraphPropertyWorkerPrepareData workerPrepareData) throws Exception {
        super.prepare(workerPrepareData);
        String geoLocationIri = getOntologyRepository().getPropertyIRIByIntent(GEO_LOCATION_INTENT);
        if (geoLocationIri == null) {
            return;
        }
        geoLocationProperty = new GeoPointVisalloProperty(geoLocationIri);

        propertyNames = toSet(getConfiguration().getSubset(CONFIG_PROPERTY_NAME_PREFIX).values());

        String intentPropertyName = getOntologyRepository().getPropertyIRIByIntent(ZIP_CODE_INTENT);
        if (intentPropertyName != null) {
            propertyNames.add(intentPropertyName);
        }
    }

    @Override
    public void execute(InputStream in, GraphPropertyWorkData data) throws Exception {
        if (geoLocationProperty == null) {
            return;
        }

        String propertyValue = StringVisalloProperty.getValue(data.getProperty());
        ZipCodeEntry zipCode = zipCodeRepository.find(propertyValue);
        if (zipCode == null) {
            return;
        }

        GeoPoint existingZipCode = geoLocationProperty.getPropertyValue(data.getElement(), data.getProperty().getKey());
        if (existingZipCode == null) {
            Metadata metadata = new Metadata();
            GeoPoint value = zipCode.createGeoPoint();
            geoLocationProperty.addPropertyValue(data.getElement(), data.getProperty().getKey(), value, metadata, data.getVisibility(), getAuthorizations());
            getGraph().flush();

            getWorkQueueRepository().pushGraphPropertyQueue(data.getElement(), geoLocationProperty.getProperty(data.getElement(), data.getProperty().getKey()), data.getPriority());
        }
    }

    @Override
    public boolean isHandled(Element element, Property property) {
        if (property == null || geoLocationProperty == null) {
            return false;
        }

        if (!propertyNames.contains(property.getName())) {
            return false;
        }

        return true;
    }

    @Inject
    public void setZipCodeRepository(ZipCodeRepository zipCodeRepository) {
        this.zipCodeRepository = zipCodeRepository;
    }
}
