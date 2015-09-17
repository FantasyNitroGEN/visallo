package org.visallo.gpw.geocoder;

import com.google.inject.Inject;
import org.vertexium.Element;
import org.vertexium.ElementType;
import org.vertexium.Property;
import org.visallo.core.formula.FormulaEvaluator;
import org.visallo.core.geocoding.GeocoderRepository;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorkData;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorker;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorkerPrepareData;
import org.visallo.core.model.Description;
import org.visallo.core.model.Name;
import org.visallo.core.model.ontology.OntologyProperty;

import java.io.InputStream;
import java.util.List;
import java.util.Locale;

@Name("Geocoder")
@Description("Geocodes addresses")
public class GeocoderGraphPropertyWorker extends GraphPropertyWorker {
    public static final String INTENT_GEOCODABLE = "geocodable";
    private static final String INTENT_GEO_LOCATION = "geoLocation";
    private List<OntologyProperty> geocodableProperties;
    private GeocoderRepository geocoderRepository;
    private OntologyProperty geoLocationProperty;
    private FormulaEvaluator formulaEvaluator;
    private Locale locale;
    private String timeZone;

    @Override
    public void prepare(GraphPropertyWorkerPrepareData workerPrepareData) throws Exception {
        super.prepare(workerPrepareData);
        geocodableProperties = getOntologyRepository().getPropertiesByIntent(INTENT_GEOCODABLE);
        geoLocationProperty = getOntologyRepository().getRequiredPropertyByIntent(INTENT_GEO_LOCATION);
        locale = Locale.getDefault();
        timeZone = "GMT";
    }

    @Override
    public void execute(InputStream in, GraphPropertyWorkData data) throws Exception {
        String locationString = getLocationString(data.getElement(), data.getProperty(), data.getWorkspaceId());
        if (locationString == null || locationString.trim().length() == 0) {
            return;
        }
        geocoderRepository.queuePropertySet(
                locationString,
                ElementType.getTypeFromElement(data.getElement()),
                data.getElement().getId(),
                data.getProperty().getKey(),
                geoLocationProperty.getTitle(),
                data.getVisibility(),
                data.getPriority()
        );
    }

    private String getLocationString(Element element, Property property, String workspaceId) {
        OntologyProperty dependentPropertyParent = getOntologyRepository().getDependentPropertyParent(property.getName());
        if (dependentPropertyParent != null) {
            FormulaEvaluator.UserContext userContext = new FormulaEvaluator.UserContext(locale, timeZone, workspaceId);
            return this.formulaEvaluator.evaluatePropertyDisplayFormula(
                    element,
                    property.getKey(),
                    dependentPropertyParent.getTitle(),
                    userContext,
                    getAuthorizations()
            );
        }

        return property.getValue().toString();
    }

    @Override
    public boolean isHandled(Element element, Property property) {
        if (property == null) {
            return false;
        }

        for (OntologyProperty ontologyProperty : this.geocodableProperties) {
            if (doesPropertyMatch(ontologyProperty, property)) {
                return true;
            }
        }

        return false;
    }

    private boolean doesPropertyMatch(OntologyProperty ontologyProperty, Property property) {
        return property.getName().equals(ontologyProperty.getTitle());
    }

    @Inject
    public void setGeocoderRepository(GeocoderRepository geocoderRepository) {
        this.geocoderRepository = geocoderRepository;
    }

    @Inject
    public void setFormulaEvaluator(FormulaEvaluator formulaEvaluator) {
        this.formulaEvaluator = formulaEvaluator;
    }
}
