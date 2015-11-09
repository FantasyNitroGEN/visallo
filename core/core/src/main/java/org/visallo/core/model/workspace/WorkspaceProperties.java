package org.visallo.core.model.workspace;

import org.visallo.core.model.properties.types.BooleanSingleValueVisalloProperty;
import org.visallo.core.model.properties.types.IntegerSingleValueVisalloProperty;
import org.visallo.core.model.properties.types.StringSingleValueVisalloProperty;

public class WorkspaceProperties {
    public static final StringSingleValueVisalloProperty TITLE = new StringSingleValueVisalloProperty("http://visallo.org/workspace#workspace/title");
    public static final BooleanSingleValueVisalloProperty WORKSPACE_TO_USER_IS_CREATOR = new BooleanSingleValueVisalloProperty("http://visallo.org/workspace#toUser/creator");
    public static final StringSingleValueVisalloProperty WORKSPACE_TO_USER_ACCESS = new StringSingleValueVisalloProperty("http://visallo.org/workspace#toUser/access");
    public static final IntegerSingleValueVisalloProperty WORKSPACE_TO_ENTITY_GRAPH_POSITION_X = new IntegerSingleValueVisalloProperty("http://visallo.org/workspace#toEntity/graphPositionX");
    public static final IntegerSingleValueVisalloProperty WORKSPACE_TO_ENTITY_GRAPH_POSITION_Y = new IntegerSingleValueVisalloProperty("http://visallo.org/workspace#toEntity/graphPositionY");
    public static final StringSingleValueVisalloProperty WORKSPACE_TO_ENTITY_GRAPH_LAYOUT_JSON = new StringSingleValueVisalloProperty("http://visallo.org/workspace#toEntity/graphLayoutJson");
    public static final BooleanSingleValueVisalloProperty WORKSPACE_TO_ENTITY_VISIBLE = new BooleanSingleValueVisalloProperty("http://visallo.org/workspace#toEntity/visible");
}
