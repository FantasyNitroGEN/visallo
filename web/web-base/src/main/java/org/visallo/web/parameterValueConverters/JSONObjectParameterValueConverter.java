package org.visallo.web.parameterValueConverters;

import com.v5analytics.webster.DefaultParameterValueConverter;
import org.json.JSONObject;
import org.visallo.core.exception.VisalloException;

public class JSONObjectParameterValueConverter extends DefaultParameterValueConverter.SingleValueConverter<JSONObject> {
    @Override
    public JSONObject convert(Class parameterType, String parameterName, String value) {
        try {
            return new JSONObject(value);
        } catch (Exception ex) {
            throw new VisalloException("Could not parse JSONObject: " + value);
        }
    }
}
