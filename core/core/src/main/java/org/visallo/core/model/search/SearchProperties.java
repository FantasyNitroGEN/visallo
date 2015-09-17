package org.visallo.core.model.search;

import org.visallo.core.model.properties.types.JsonSingleValueVisalloProperty;
import org.visallo.core.model.properties.types.StringSingleValueVisalloProperty;

public class SearchProperties {
    public static final String IRI = "http://visallo.org/search";

    public static final String HAS_SAVED_SEARCH = "http://visallo.org/search#hasSavedSearch";

    public static final String CONCEPT_TYPE_SAVED_SEARCH = "http://visallo.org/search#savedSearch";

    public static final StringSingleValueVisalloProperty NAME = new StringSingleValueVisalloProperty("http://visallo.org/search#name");
    public static final StringSingleValueVisalloProperty URL = new StringSingleValueVisalloProperty("http://visallo.org/search#url");
    public static final JsonSingleValueVisalloProperty PARAMETERS = new JsonSingleValueVisalloProperty("http://visallo.org/search#parameters");
}
