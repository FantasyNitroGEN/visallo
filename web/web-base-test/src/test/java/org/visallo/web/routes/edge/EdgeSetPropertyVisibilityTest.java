package org.visallo.web.routes.edge;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.vertexium.Authorizations;
import org.vertexium.Edge;
import org.vertexium.Vertex;
import org.vertexium.Visibility;
import org.visallo.core.user.ProxyUser;
import org.visallo.web.routes.SetPropertyVisibilityTestBase;

import java.io.IOException;
import java.util.ResourceBundle;

@RunWith(MockitoJUnitRunner.class)
public class EdgeSetPropertyVisibilityTest extends SetPropertyVisibilityTestBase {
    private EdgeSetPropertyVisibility edgeSetPropertyVisibility;
    private Edge e1;

    @Before
    public void before() throws IOException {
        super.before();

        edgeSetPropertyVisibility = new EdgeSetPropertyVisibility(
                graph,
                workspaceRepository,
                visibilityTranslator,
                graphRepository,
                workQueueRepository
        );

        Visibility visibility = new Visibility("");
        authorizations = graph.createAuthorizations("A");
        Vertex v1 = graph.addVertex("v1", visibility, authorizations);
        Vertex v2 = graph.addVertex("v2", visibility, authorizations);
        e1 = graph.prepareEdge("e1", v1, v2, "edgeLabel", visibility)
                .addPropertyValue("k1", "p1", "value1", visibility)
                .save(authorizations);
    }

    @Test
    public void testValid() throws Exception {
        super.testValid(e1);
    }

    @Override
    public void testBadVisibility() throws Exception {
        super.testBadVisibility(e1);
    }

    @Override
    protected void handle(
            String elementId,
            String newVisibilitySource,
            String oldVisibilitySource,
            String propertyKey,
            String propertyName,
            String workspaceId,
            ResourceBundle resourceBundle,
            ProxyUser user,
            Authorizations authorizations
    ) throws Exception {
        edgeSetPropertyVisibility.handle(
                elementId,
                newVisibilitySource,
                oldVisibilitySource,
                propertyKey,
                propertyName,
                workspaceId,
                resourceBundle,
                user,
                authorizations
        );
    }
}
