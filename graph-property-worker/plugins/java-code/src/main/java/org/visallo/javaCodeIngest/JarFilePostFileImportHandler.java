package org.visallo.javaCodeIngest;

import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.Vertex;
import org.vertexium.Visibility;
import org.vertexium.mutation.ExistingElementMutation;
import org.visallo.core.ingest.PostFileImportHandler;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.properties.types.PropertyMetadata;
import org.visallo.core.model.properties.types.VisalloPropertyUpdate;
import org.visallo.core.model.workspace.Workspace;
import org.visallo.core.user.User;

import java.util.List;

public class JarFilePostFileImportHandler extends PostFileImportHandler {
    private static final String MULTI_VALUE_KEY = JarFilePostFileImportHandler.class.getName();

    @Override
    public void handle(
            Graph graph,
            Vertex vertex,
            List<VisalloPropertyUpdate> changedProperties,
            Workspace workspace,
            PropertyMetadata propertyMetadata,
            Visibility visibility,
            User user,
            Authorizations authorizations
    ) {
        if (!VisalloProperties.RAW.hasProperty(vertex)) {
            return;
        }

        String fileName = VisalloProperties.FILE_NAME.getOnlyPropertyValue(vertex);
        if (fileName == null || !fileName.endsWith(".jar")) {
            return;
        }

        ExistingElementMutation<Vertex> m = vertex.prepareMutation();
        VisalloProperties.CONCEPT_TYPE.updateProperty(changedProperties, vertex, m, JavaCodeIngestOntology.CONCEPT_TYPE_JAR_FILE, propertyMetadata, visibility);
        VisalloProperties.MIME_TYPE.updateProperty(changedProperties, vertex, m, MULTI_VALUE_KEY, "application/java-archive", propertyMetadata, visibility);
        m.save(authorizations);
    }
}
