package org.visallo.web.routes.vertex;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.vertexium.Authorizations;
import org.vertexium.Vertex;
import org.vertexium.Visibility;
import org.visallo.core.user.ProxyUser;
import org.visallo.web.routes.SetPropertyVisibilityTestBase;

import java.io.IOException;
import java.util.ResourceBundle;

@RunWith(MockitoJUnitRunner.class)
public class VertexSetPropertyVisibilityTest extends SetPropertyVisibilityTestBase {
    private VertexSetPropertyVisibility vertexSetPropertyVisibility;
    private Vertex v1;

    @Before
    public void before() throws IOException {
        super.before();

        vertexSetPropertyVisibility = new VertexSetPropertyVisibility(
                graph,
                workspaceRepository,
                visibilityTranslator,
                graphRepository,
                workQueueRepository
        );

        Visibility visibility = new Visibility("");
        v1 = graph.prepareVertex("v1", visibility)
                .addPropertyValue("k1", "p1", "value1", visibility)
                .save(authorizations);
    }

    @Test
    public void testValid() throws Exception {
        super.testValid(v1);
    }

    @Override
    public void testBadVisibility() throws Exception {
        super.testBadVisibility(v1);
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
        vertexSetPropertyVisibility.handle(
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