package org.visallo.core.model.properties.types;

import org.visallo.web.clientapi.model.DirectoryEntity;
import org.visallo.web.clientapi.util.ClientApiConverter;

public class DirectoryEntityVisalloProperty extends VisalloProperty<DirectoryEntity, String> {
    public DirectoryEntityVisalloProperty(String key) {
        super(key);
    }

    @Override
    public String wrap(DirectoryEntity value) {
        return ClientApiConverter.clientApiToString(value);
    }

    @Override
    public DirectoryEntity unwrap(Object value) {
        if (value == null) {
            return null;
        }
        String valueStr;
        if (value instanceof String) {
            valueStr = (String) value;
        } else {
            valueStr = value.toString();
        }
        return ClientApiConverter.toClientApi(valueStr, DirectoryEntity.class);
    }
}
