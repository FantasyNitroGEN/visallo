package org.visallo.common.rdf;

import com.google.common.base.Strings;
import org.vertexium.DateOnly;
import org.vertexium.ElementType;
import org.vertexium.type.GeoPoint;
import org.visallo.core.exception.VisalloException;
import org.visallo.web.clientapi.model.DirectoryEntity;

import javax.xml.bind.DatatypeConverter;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public abstract class PropertyVisalloRdfTriple extends ElementVisalloRdfTriple {
    private final String propertyKey;
    private final String propertyName;
    private final String propertyVisibilitySource;
    private final Object value;

    public PropertyVisalloRdfTriple(
            ElementType elementType,
            String elementId,
            String elementVisibilitySource,
            String propertyKey,
            String propertyName,
            String propertyVisibilitySource,
            Object value
    ) {
        super(elementType, elementId, elementVisibilitySource);
        this.propertyKey = propertyKey;
        this.propertyName = propertyName;
        this.propertyVisibilitySource = propertyVisibilitySource;
        this.value = value;
    }

    public String getPropertyKey() {
        return propertyKey;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public String getPropertyVisibilitySource() {
        return propertyVisibilitySource;
    }

    public Object getValue() {
        return value;
    }

    protected String getPropertyRdfString() {
        String result = getPropertyName();
        if (!Strings.isNullOrEmpty(getPropertyKey())) {
            result += String.format(":%s", escape(getPropertyKey(), ':'));
        }
        if (!Strings.isNullOrEmpty(getPropertyVisibilitySource())) {
            result += String.format("[%s]", getPropertyVisibilitySource());
        }
        return result;
    }

    protected String getValueRdfString() {
        if (getValue() == null) {
            throw new VisalloException("Unexpected null value");
        }
        if (getValue() instanceof String) {
            return String.format("\"%s\"", getValue());
        }
        if (getValue() instanceof Integer) {
            return getValueRdfStringWithType(getValue(), VisalloRdfTriple.PROPERTY_TYPE_INT);
        }
        if (getValue() instanceof Double || getValue() instanceof Float) {
            return getValueRdfStringWithType(getValue(), VisalloRdfTriple.PROPERTY_TYPE_DOUBLE);
        }
        if (getValue() instanceof Boolean) {
            return getValueRdfStringWithType(getValue(), VisalloRdfTriple.PROPERTY_TYPE_BOOLEAN);
        }
        if (getValue() instanceof GeoPoint) {
            String geoPointStr = getValue().toString();
            geoPointStr = geoPointStr.substring(1, geoPointStr.length() - 1); // remove '(' and ')'
            return getValueRdfStringWithType(geoPointStr, VisalloRdfTriple.PROPERTY_TYPE_GEOLOCATION);
        }
        if (getValue() instanceof Date) {
            Calendar cal = new GregorianCalendar(TimeZone.getTimeZone(ZoneId.of("GMT")));
            cal.setTime(((Date) getValue()));
            String val = DatatypeConverter.printDateTime(cal);
            return getValueRdfStringWithType(val, VisalloRdfTriple.PROPERTY_TYPE_DATE_TIME);
        }
        if (getValue() instanceof DirectoryEntity) {
            return getValueRdfStringWithType(
                    ((DirectoryEntity) getValue()).getId(),
                    VisalloRdfTriple.PROPERTY_TYPE_DIRECTORY_ENTITY
            );
        }
        if (getValue() instanceof DateOnly) {
            return getValueRdfStringWithType(getValue(), VisalloRdfTriple.PROPERTY_TYPE_DATE);
        }
        throw new VisalloException("Unhandled value type \"" + getValue().getClass().getName() + "\" to convert to RDF string");
    }

    private static String getValueRdfStringWithType(Object value, String typeUri) {
        return String.format("\"%s\"^^<%s>", value, typeUri);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PropertyVisalloRdfTriple that = (PropertyVisalloRdfTriple) o;

        if (propertyKey != null ? !propertyKey.equals(that.propertyKey) : that.propertyKey != null) {
            return false;
        }
        if (propertyName != null ? !propertyName.equals(that.propertyName) : that.propertyName != null) {
            return false;
        }
        if (propertyVisibilitySource != null ? !propertyVisibilitySource.equals(that.propertyVisibilitySource) : that.propertyVisibilitySource != null) {
            return false;
        }
        if (value != null ? !value.equals(that.value) : that.value != null) {
            return false;
        }

        return super.equals(that);
    }
}
