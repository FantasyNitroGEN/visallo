package org.visallo.web.parameterProviders;

import com.v5analytics.webster.DefaultParameterValueConverter;
import org.visallo.core.exception.VisalloException;
import org.visallo.web.clientapi.model.ClientApiObject;
import org.visallo.web.clientapi.util.ObjectMapperFactory;

import java.io.IOException;

public class ClientApiObjectParameterValueConverter extends DefaultParameterValueConverter.SingleValueConverter<ClientApiObject> {
    @Override
    public ClientApiObject convert(Class parameterType, String parameterName, String value) {
        try {
            return (ClientApiObject) ObjectMapperFactory.getInstance().readValue(value, parameterType);
        } catch (IOException ex) {
            throw new VisalloException("Could not convert \"" + value + "\" to object of type " + parameterType.getName(), ex);
        }
    }
}
