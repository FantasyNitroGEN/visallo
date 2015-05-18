package org.visallo.csv.model;

import org.visallo.core.model.properties.types.ClientApiVisalloProperty;

public class MappingProperty extends ClientApiVisalloProperty<Mapping> {
    public MappingProperty(String key) {
        super(key, Mapping.class);
    }
}
