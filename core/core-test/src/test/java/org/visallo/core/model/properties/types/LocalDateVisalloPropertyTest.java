package org.visallo.core.model.properties.types;

public class LocalDateVisalloPropertyTest extends LocalDateVisalloPropertyTestBase {
    @Override
    protected WrapsLocalDate createVisalloProperty() {
        return new LocalDateVisalloProperty("name");
    }
}
