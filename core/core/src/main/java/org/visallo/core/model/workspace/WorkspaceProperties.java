package org.visallo.core.model.workspace;

import org.visallo.core.model.properties.types.BooleanSingleValueVisalloProperty;
import org.visallo.core.model.properties.types.IntegerSingleValueVisalloProperty;
import org.visallo.core.model.properties.types.StringSingleValueVisalloProperty;

public class WorkspaceProperties {
    public static final StringSingleValueVisalloProperty TITLE = new StringSingleValueVisalloProperty(WorkspaceRepository.WORKSPACE_CONCEPT_IRI + "/title");
    public static final BooleanSingleValueVisalloProperty WORKSPACE_TO_USER_IS_CREATOR = new BooleanSingleValueVisalloProperty(WorkspaceRepository.WORKSPACE_TO_USER_RELATIONSHIP_IRI + "/creator");
    public static final StringSingleValueVisalloProperty WORKSPACE_TO_USER_ACCESS = new StringSingleValueVisalloProperty(WorkspaceRepository.WORKSPACE_TO_USER_RELATIONSHIP_IRI + "/access");
    public static final IntegerSingleValueVisalloProperty WORKSPACE_TO_ENTITY_GRAPH_POSITION_X = new IntegerSingleValueVisalloProperty(WorkspaceRepository.WORKSPACE_TO_ENTITY_RELATIONSHIP_IRI + "/graphPositionX");
    public static final IntegerSingleValueVisalloProperty WORKSPACE_TO_ENTITY_GRAPH_POSITION_Y = new IntegerSingleValueVisalloProperty(WorkspaceRepository.WORKSPACE_TO_ENTITY_RELATIONSHIP_IRI + "/graphPositionY");
    public static final StringSingleValueVisalloProperty WORKSPACE_TO_ENTITY_GRAPH_LAYOUT_JSON = new StringSingleValueVisalloProperty(WorkspaceRepository.WORKSPACE_TO_ENTITY_RELATIONSHIP_IRI + "/graphLayoutJson");
    public static final BooleanSingleValueVisalloProperty WORKSPACE_TO_ENTITY_VISIBLE = new BooleanSingleValueVisalloProperty(WorkspaceRepository.WORKSPACE_TO_ENTITY_RELATIONSHIP_IRI + "/visible");
}
