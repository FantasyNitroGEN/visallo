package org.visallo.core.ingest;

import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.Vertex;
import org.vertexium.Visibility;
import org.visallo.core.model.properties.types.PropertyMetadata;
import org.visallo.core.model.properties.types.VisalloPropertyUpdate;
import org.visallo.core.model.workspace.Workspace;
import org.visallo.core.user.User;

import java.util.List;

/**
 * Provides a hook to modify the vertex before it goes into the Graph Property Worker pipeline.
 */
public abstract class PostFileImportHandler {
    public abstract void handle(
            Graph graph,
            Vertex vertex,
            List<VisalloPropertyUpdate> changedProperties,
            Workspace workspace,
            PropertyMetadata propertyMetadata,
            Visibility visibility,
            User user,
            Authorizations authorizations
    );
}
