package org.visallo.foodTruck;

import org.visallo.core.ingest.graphProperty.GraphPropertyWorkData;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorker;
import org.visallo.core.model.properties.VisalloProperties;
import org.vertexium.Element;
import org.vertexium.Property;
import org.vertexium.Vertex;

import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;

import static org.vertexium.util.IterableUtils.toList;

public class FoodTruckRemoveOldGeoLocationsGraphPropertyWorker extends GraphPropertyWorker {
    @Override
    public void execute(InputStream in, GraphPropertyWorkData data) throws Exception {
        Date geoLocationDate = FoodTruckOntology.GEO_LOCATION_DATE.getOnlyPropertyValue(data.getElement());
        if (geoLocationDate == null) {
            return;
        }
        Calendar geoLocationCalendar = Calendar.getInstance();
        geoLocationCalendar.setTime(geoLocationDate);

        Calendar nowCalendar = Calendar.getInstance();
        nowCalendar.setTime(new Date());

        if (geoLocationCalendar.get(Calendar.DAY_OF_YEAR) == nowCalendar.get(Calendar.DAY_OF_YEAR)) {
            return;
        }

        for (Property property : toList(FoodTruckOntology.GEO_LOCATION.getProperties(data.getElement()))) {
            data.getElement().softDeleteProperty(property.getKey(), property.getName(), getAuthorizations());
        }
        for (Property property : toList(FoodTruckOntology.GEO_LOCATION_DATE.getProperties(data.getElement()))) {
            data.getElement().softDeleteProperty(property.getKey(), property.getName(), getAuthorizations());
        }
        getGraph().flush();
    }

    @Override
    public boolean isHandled(Element element, Property property) {
        if (!(element instanceof Vertex)) {
            return false;
        }

        String conceptType = VisalloProperties.CONCEPT_TYPE.getPropertyValue(element);
        if (conceptType == null || !conceptType.equals(FoodTruckOntology.CONCEPT_TYPE_FOOD_TRUCK)) {
            return false;
        }

        return true;
    }
}
