package org.visallo.core.model.properties.types;

public class LocalDateSingleValueVisalloPropertyTest extends LocalDateVisalloPropertyTestBase {
    @Override
    protected WrapsLocalDate createVisalloProperty() {
        return new LocalDateSingleValueVisalloProperty("name");
    }
}
