package org.visallo.core.model.workspace;

import org.visallo.core.model.properties.types.BooleanSingleValueVisalloProperty;
import org.visallo.core.model.properties.types.StreamingVisalloProperty;
import org.visallo.core.model.properties.types.StringSingleValueVisalloProperty;
import org.visallo.core.model.properties.types.StringVisalloProperty;

public class WorkspaceProperties {
    public static final String WORKSPACE_CONCEPT_IRI = "http://visallo.org/workspace#workspace";
    public static final String DASHBOARD_CONCEPT_IRI = "http://visallo.org/workspace#dashboard";
    public static final String PRODUCT_CONCEPT_IRI = "http://visallo.org/workspace#product";
    public static final String DASHBOARD_ITEM_CONCEPT_IRI = "http://visallo.org/workspace#dashboardItem";
    public static final String WORKSPACE_TO_ENTITY_RELATIONSHIP_IRI = "http://visallo.org/workspace#toEntity";
    public static final String WORKSPACE_TO_ONTOLOGY_RELATIONSHIP_IRI = "http://visallo.org/workspace#toOntology";
    public static final String WORKSPACE_TO_USER_RELATIONSHIP_IRI = "http://visallo.org/workspace#toUser";
    public static final String WORKSPACE_TO_DASHBOARD_RELATIONSHIP_IRI = "http://visallo.org/workspace#toDashboard";
    public static final String WORKSPACE_TO_PRODUCT_RELATIONSHIP_IRI = "http://visallo.org/workspace#toProduct";
    public static final String DASHBOARD_TO_DASHBOARD_ITEM_RELATIONSHIP_IRI = "http://visallo.org/workspace#toDashboardItem";

    public static final StringSingleValueVisalloProperty TITLE = new StringSingleValueVisalloProperty("http://visallo.org/workspace#workspace/title");
    public static final BooleanSingleValueVisalloProperty WORKSPACE_TO_USER_IS_CREATOR = new BooleanSingleValueVisalloProperty("http://visallo.org/workspace#toUser/creator");
    public static final StringSingleValueVisalloProperty WORKSPACE_TO_USER_ACCESS = new StringSingleValueVisalloProperty("http://visallo.org/workspace#toUser/access");

    public static final StringSingleValueVisalloProperty DASHBOARD_ITEM_EXTENSION_ID = new StringSingleValueVisalloProperty("http://visallo.org/workspace#extensionId");
    public static final StringSingleValueVisalloProperty DASHBOARD_ITEM_CONFIGURATION = new StringSingleValueVisalloProperty("http://visallo.org/workspace#configuration");

    public static final StringSingleValueVisalloProperty PRODUCT_KIND = new StringSingleValueVisalloProperty("http://visallo.org/product#kind");
    public static final StringVisalloProperty PRODUCT_DATA = new StringVisalloProperty("http://visallo.org/product#data");
    public static final StringVisalloProperty PRODUCT_EXTENDED_DATA = new StringVisalloProperty("http://visallo.org/product#extendedData");
    public static final StreamingVisalloProperty PRODUCT_PREVIEW_DATA_URL = new StreamingVisalloProperty("http://visallo.org/product#previewDataUrl");
}
