package org.visallo.core.model.ontology;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.vertexium.Authorizations;
import org.vertexium.type.GeoCircle;
import org.vertexium.type.GeoHash;
import org.vertexium.type.GeoPoint;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.properties.types.*;
import org.visallo.web.clientapi.model.ClientApiOntology;
import org.visallo.web.clientapi.model.PropertyType;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class OntologyProperty {
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    public static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    public static final SimpleDateFormat DATE_TIME_WITH_SECONDS_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public static final Pattern GEO_LOCATION_FORMAT = Pattern.compile("POINT\\((.*?),(.*?)\\)", Pattern.CASE_INSENSITIVE);
    public static final Pattern GEO_LOCATION_ALTERNATE_FORMAT = Pattern.compile("(.*?),(.*)", Pattern.CASE_INSENSITIVE);

    static {
        TimeZone utc = TimeZone.getTimeZone("UTC");
        DATE_FORMAT.setTimeZone(utc);
        DATE_TIME_FORMAT.setTimeZone(utc);
        DATE_TIME_WITH_SECONDS_FORMAT.setTimeZone(utc);
    }

    public String getIri() {
        return getTitle();
    }

    public abstract String getTitle();

    public abstract String getDisplayName();

    public abstract boolean getUserVisible();

    public abstract boolean getSearchable();

    public abstract boolean getAddable();

    public abstract boolean getSortable();

    public abstract PropertyType getDataType();

    public abstract Double getBoost();

    public abstract Map<String, String> getPossibleValues();

    public abstract String getDisplayType();

    public abstract String getPropertyGroup();

    public abstract String getValidationFormula();

    public abstract String getDisplayFormula();

    public abstract boolean getUpdateable();

    public abstract boolean getDeleteable();

    public abstract ImmutableList<String> getDependentPropertyIris();

    public abstract String[] getIntents();

    public abstract void addIntent(String intent, Authorizations authorizations);

    public abstract void removeIntent(String intent, Authorizations authorizations);

    public void updateIntents(String[] newIntents, Authorizations authorizations) {
        ArrayList<String> toBeRemovedIntents = Lists.newArrayList(getIntents());
        for (String newIntent : newIntents) {
            if (toBeRemovedIntents.contains(newIntent)) {
                toBeRemovedIntents.remove(newIntent);
            } else {
                addIntent(newIntent, authorizations);
            }
        }
        for (String toBeRemovedIntent : toBeRemovedIntents) {
            removeIntent(toBeRemovedIntent, authorizations);
        }
    }

    public abstract void setProperty(String name, Object value, Authorizations authorizations);

    public static Collection<ClientApiOntology.Property> toClientApiProperties(Iterable<OntologyProperty> properties) {
        Collection<ClientApiOntology.Property> results = new ArrayList<>();
        for (OntologyProperty property : properties) {
            results.add(property.toClientApi());
        }
        return results;
    }

    public ClientApiOntology.Property toClientApi() {
        try {
            ClientApiOntology.Property result = new ClientApiOntology.Property();
            result.setTitle(getTitle());
            result.setDisplayName(getDisplayName());
            result.setUserVisible(getUserVisible());
            result.setSearchable(getSearchable());
            result.setAddable(getAddable());
            result.setSortable(getSortable());
            result.setDataType(getDataType());
            result.setDisplayType(getDisplayType());
            result.setPropertyGroup(getPropertyGroup());
            result.setValidationFormula(getValidationFormula());
            result.setDisplayFormula(getDisplayFormula());
            result.setDependentPropertyIris(getDependentPropertyIris());
            result.setDeleteable(getDeleteable());
            result.setUpdateable(getUpdateable());
            if (getPossibleValues() != null) {
                result.getPossibleValues().putAll(getPossibleValues());
            }
            if (getIntents() != null) {
                result.getIntents().addAll(Arrays.asList(getIntents()));
            }
            return result;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public Object convert(Object value) throws ParseException {
        if (value == null) {
            return null;
        }
        return convertString(value.toString());
    }

    public Object convertString(String valueStr) throws ParseException {
        PropertyType dataType = getDataType();
        Object value = valueStr;
        switch (dataType) {
            case DATE:
                value = parseDateTime(valueStr);
                break;
            case GEO_LOCATION:
                value = parseGeoLocation(valueStr);
                break;
            case CURRENCY:
                value = new BigDecimal(valueStr);
                break;
            case DOUBLE:
                value = Double.parseDouble(valueStr);
                break;
            case INTEGER:
                value = Integer.parseInt(valueStr);
                break;
            case BOOLEAN:
                value = Boolean.parseBoolean(valueStr);
                break;
        }
        return value;
    }

    public static Object convert(JSONArray values, PropertyType propertyDataType, int index) throws ParseException {
        switch (propertyDataType) {
            case DIRECTORY_ENTITY:
                if (values.get(index) instanceof JSONObject) {
                    JSONObject json = values.getJSONObject(index);
                    String id = json.optString("id");
                    checkNotNull(id, "id is a required field for directory entity json");
                    return id;
                } else {
                    String valueStr = values.getString(index);
                    try {
                        JSONObject json = new JSONObject(valueStr);
                        String id = json.optString("id");
                        checkNotNull(id, "id is a required field for directory entity json");
                        return id;
                    } catch (JSONException ex) {
                        return valueStr;
                    }
                }
            case DATE: {
                String valueStr = values.getString(index);
                return parseDateTime(valueStr);
            }
            case GEO_LOCATION:
                if (values.get(index) instanceof String) {
                    String valueStr = values.getString(index);
                    return new GeoHash(valueStr);
                }
                return new GeoCircle(
                        values.getDouble(index),
                        values.getDouble(index + 1),
                        values.getDouble(index + 2)
                );
            case CURRENCY:
                return new BigDecimal(values.getString(index));
            case INTEGER:
                return values.getInt(index);
            case DOUBLE:
                return values.getDouble(index);
            case BOOLEAN:
                Object result = values.get(index);
                if ("T".equals(result)) {
                    return true;
                }
                if ("F".equals(result)) {
                    return false;
                }
                return values.getBoolean(index);
        }
        return values.getString(index);
    }

    protected static Object parseGeoLocation(String valueStr) {
        try {
            JSONObject json = new JSONObject(valueStr);
            double latitude = json.getDouble("latitude");
            double longitude = json.getDouble("longitude");
            String altitudeString = json.optString("altitude");
            Double altitude = (altitudeString == null || altitudeString.length() == 0) ? null : Double.parseDouble(altitudeString);
            String description = json.optString("description");
            return new GeoPoint(latitude, longitude, altitude, description);
        } catch (Exception ex) {
            Matcher match = GEO_LOCATION_FORMAT.matcher(valueStr);
            if (match.find()) {
                double latitude = Double.parseDouble(match.group(1).trim());
                double longitude = Double.parseDouble(match.group(2).trim());
                return new GeoPoint(latitude, longitude);
            }
            match = GEO_LOCATION_ALTERNATE_FORMAT.matcher(valueStr);
            if (match.find()) {
                double latitude = Double.parseDouble(match.group(1).trim());
                double longitude = Double.parseDouble(match.group(2).trim());
                return new GeoPoint(latitude, longitude);
            }
            throw new VisalloException("Could not parse location: " + valueStr);
        }
    }

    public boolean hasDependentPropertyIris() {
        return getDependentPropertyIris() != null && getDependentPropertyIris().size() > 0;
    }

    private static Date parseDateTime(String valueStr) throws ParseException {
        try {
            return DATE_TIME_WITH_SECONDS_FORMAT.parse(valueStr);
        } catch (ParseException ex1) {
            try {
                return DATE_TIME_FORMAT.parse(valueStr);
            } catch (ParseException ex2) {
                try {
                    return DATE_FORMAT.parse(valueStr);
                } catch (ParseException ex3) {
                    return new Date(Long.parseLong(valueStr));
                }
            }
        }
    }

    public VisalloProperty getVisalloProperty() {
        switch (getDataType()) {
            case IMAGE:
            case BINARY:
                return new StreamingVisalloProperty(getIri());
            case BOOLEAN:
                return new BooleanVisalloProperty(getIri());
            case DATE:
                return new DateVisalloProperty(getIri());
            case CURRENCY:
            case DOUBLE:
                return new DoubleVisalloProperty(getIri());
            case GEO_LOCATION:
                return new GeoPointVisalloProperty(getIri());
            case INTEGER:
                return new IntegerVisalloProperty(getIri());
            case STRING:
            case DIRECTORY_ENTITY:
                return new StringVisalloProperty(getIri());
            default:
                throw new VisalloException("Could not get " + VisalloProperty.class.getName() + " for data type " + getDataType());
        }
    }

    @Override
    public String toString() {
        return this.getClass().getName() + "{iri:" + getIri() + "}";
    }

    @Override
    public int hashCode() {
        return getIri().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof OntologyProperty)) {
            return false;
        }

        String otherIri = ((OntologyProperty) obj).getIri();
        return getIri().equals(otherIri);
    }
}
